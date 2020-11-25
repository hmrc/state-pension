/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.statepension.services

import java.util.TimeZone

import com.google.inject.Inject
import org.joda.time.{DateTimeZone, LocalDate}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.statepension.connectors.{DesConnector, NpsConnector, StatePensionAuditConnector}
import uk.gov.hmrc.statepension.domain.Exclusion.Exclusion
import uk.gov.hmrc.statepension.domain._
import uk.gov.hmrc.statepension.domain.nps.{Country, Summary}
import uk.gov.hmrc.statepension.events.Forecasting

import scala.concurrent.Future

trait StatePensionService {

  val nps: NpsConnector
  val forecastingService: ForecastingService
  val rateService: RateService
  val metrics: ApplicationMetrics
  val customAuditConnector: StatePensionAuditConnector
  def getMCI(summary: Summary, nino: Nino)(implicit hc: HeaderCarrier): Future[Boolean]

  def now: LocalDate = LocalDate.now(DateTimeZone.forTimeZone(TimeZone.getTimeZone("Europe/London")))

  def getStatement(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExclusion, StatePension]] = {

    val summaryF =  nps.getSummary(nino)
    val liablitiesF = nps.getLiabilities(nino)
    val niRecordF = nps.getNIRecord(nino)

    for {
      summary <- summaryF
      liablities <- liablitiesF
      niRecord <- niRecordF
      manualCorrespondence <- getMCI(summary, nino)
    } yield {

      val exclusions: List[Exclusion] = ExclusionService(
        dateOfDeath = summary.dateOfDeath,
        pensionDate = summary.statePensionAgeDate,
        now,
        summary.amounts.pensionEntitlement,
        summary.amounts.startingAmount2016,
        forecastingService.calculateStartingAmount(summary.amounts.amountA2016.total, summary.amounts.amountB2016.mainComponent),
        liablities,
        manualCorrespondence
      ).getExclusions

      val purgedRecord = niRecord.purge(summary.finalRelevantStartYear)

      auditSummary(nino, summary, purgedRecord.qualifyingYears, exclusions)

      if (exclusions.nonEmpty) {

        metrics.exclusion(filterExclusions(exclusions))

        Left(StatePensionExclusion(
          exclusionReasons = exclusions,
          pensionAge = summary.statePensionAge,
          pensionDate = summary.statePensionAgeDate,
          statePensionAgeUnderConsideration = if (exclusions.contains(Exclusion.AmountDissonance) || exclusions.contains(Exclusion.IsleOfMan))
            checkStatePensionAgeUnderConsideration(summary.dateOfBirth) else false
        ))
      } else {

        val forecast = forecastingService.calculateForecastAmount(
          summary.earningsIncludedUpTo,
          summary.finalRelevantStartYear,
          summary.amounts.pensionEntitlementRounded,
          purgedRecord.qualifyingYears
        )

        val personalMaximum = forecastingService.calculatePersonalMaximum(
          summary.earningsIncludedUpTo,
          summary.finalRelevantStartYear,
          purgedRecord.qualifyingYearsPre2016,
          purgedRecord.qualifyingYearsPost2016,
          payableGapsPre2016 = purgedRecord.payableGapsPre2016,
          payableGapsPost2016 = purgedRecord.payableGapsPost2016,
          additionalPension = summary.amounts.amountA2016.totalAP,
          rebateDerivedAmount = summary.amounts.amountB2016.rebateDerivedAmount
        )

        val statePension = StatePension(
          earningsIncludedUpTo = summary.earningsIncludedUpTo,
          amounts = StatePensionAmounts(
            summary.amounts.protectedPayment2016 > 0,
            StatePensionAmount(None, None, forecastingService.sanitiseCurrentAmount(summary.amounts.pensionEntitlementRounded, purgedRecord.qualifyingYears)),
            StatePensionAmount(Some(forecast.yearsToWork), None, forecast.amount),
            StatePensionAmount(Some(personalMaximum.yearsToWork), Some(personalMaximum.gapsToFill), personalMaximum.amount),
            StatePensionAmount(None, None, summary.amounts.amountB2016.rebateDerivedAmount),
            StatePensionAmount(None, None, summary.amounts.startingAmount2016),
            oldRules = OldRules(basicStatePension = summary.amounts.amountA2016.basicStatePension,
              additionalStatePension = summary.amounts.amountA2016.additionalStatePension,
              graduatedRetirementBenefit = summary.amounts.amountA2016.graduatedRetirementBenefit),
            newRules = NewRules(grossStatePension = summary.amounts.amountB2016.mainComponent + summary.amounts.amountB2016.rebateDerivedAmount,
              rebateDerivedAmount = summary.amounts.amountB2016.rebateDerivedAmount)
          ),
          pensionAge = summary.statePensionAge,
          pensionDate = summary.statePensionAgeDate,
          finalRelevantYear = summary.finalRelevantYear,
          numberOfQualifyingYears = purgedRecord.qualifyingYears,
          pensionSharingOrder = summary.pensionSharingOrderSERPS,
          currentFullWeeklyPensionAmount = rateService.MAX_AMOUNT,
          reducedRateElection = summary.reducedRateElection,
          reducedRateElectionCurrentWeeklyAmount = if (summary.reducedRateElection) Some(summary.amounts.pensionEntitlementRounded)
          else None,
          statePensionAgeUnderConsideration = checkStatePensionAgeUnderConsideration(summary.dateOfBirth)
        )
        metrics.summary(statePension.amounts.forecast.weeklyAmount, statePension.amounts.current.weeklyAmount,
          statePension.contractedOut, statePension.forecastScenario, statePension.amounts.maximum.weeklyAmount,
          statePension.amounts.forecast.yearsToWork.getOrElse(0), statePension.mqpScenario,
          statePension.amounts.starting.weeklyAmount, statePension.amounts.oldRules.basicStatePension,
          statePension.amounts.oldRules.additionalStatePension, statePension.amounts.oldRules.graduatedRetirementBenefit,
          statePension.amounts.newRules.grossStatePension, statePension.amounts.newRules.rebateDerivedAmount,
          statePension.reducedRateElection, statePension.reducedRateElectionCurrentWeeklyAmount,
          statePension.statePensionAgeUnderConsideration
        )

        Right(statePension)
      }
    }
  }

  private[services] def filterExclusions(exclusions: List[Exclusion]): Exclusion = {
    if (exclusions.contains(Exclusion.Dead)) {
      Exclusion.Dead
    } else if (exclusions.contains(Exclusion.ManualCorrespondenceIndicator)) {
      Exclusion.ManualCorrespondenceIndicator
    } else if (exclusions.contains(Exclusion.PostStatePensionAge)) {
      Exclusion.PostStatePensionAge
    } else if (exclusions.contains(Exclusion.AmountDissonance)) {
      Exclusion.AmountDissonance
    } else if (exclusions.contains(Exclusion.IsleOfMan)) {
      Exclusion.IsleOfMan
    } else {
      throw new RuntimeException(s"Un-accounted for exclusion in NpsConnection: $exclusions")
    }
  }

  private[services] def auditSummary(nino: Nino, summary: Summary, qualifyingYears: Int, exclusions: List[Exclusion])(implicit hc: HeaderCarrier): Unit = {
    //Audit Des Data used in calculation
    customAuditConnector.sendEvent(Forecasting(
      nino,
      summary.earningsIncludedUpTo,
      qualifyingYears,
      summary.amounts.amountA2016,
      summary.amounts.amountB2016,
      summary.finalRelevantStartYear,
      exclusions
    ))
  }

  final val CHANGE_SPA_MIN_DATE = new LocalDate(1970, 4, 6)
  final val CHANGE_SPA_MAX_DATE = new LocalDate(1978, 4, 5)

  private def checkStatePensionAgeUnderConsideration(dateOfBirth: LocalDate): Boolean = {
    !dateOfBirth.isBefore(CHANGE_SPA_MIN_DATE)  && !dateOfBirth.isAfter(CHANGE_SPA_MAX_DATE)
  }

}
