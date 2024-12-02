/*
 * Copyright 2023 HM Revenue & Customs
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

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.connectors.{NpsConnector, ProxyCacheConnector}
import uk.gov.hmrc.statepension.domain.Exclusion._
import uk.gov.hmrc.statepension.domain._
import uk.gov.hmrc.statepension.domain.nps._
import uk.gov.hmrc.statepension.events.Forecasting

import java.time.{LocalDate, ZoneId}
import scala.concurrent.{ExecutionContext, Future}

trait StatePensionService {
  val nps: NpsConnector
  val proxyCacheConnector: ProxyCacheConnector
  val forecastingService: ForecastingService
  val rateService: RateService
  val metrics: ApplicationMetrics
  val customAuditConnector: AuditConnector
  val appConfig: AppConfig
  implicit val executionContext: ExecutionContext

  def now: LocalDate = LocalDate.now(ZoneId.of("Europe/London"))

  def getMCI(summary: Summary, nino: Nino)(implicit hc: HeaderCarrier): Future[Boolean]

  def checkPensionRequest: Boolean

  def getStatement(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExclusion, StatePension]] =

    if (checkPensionRequest) {
      for {
        pcd <- proxyCacheConnector.get(nino)
        mci <- getMCI(pcd.summary, nino)
      } yield buildStatePension(pcd.summary, pcd.liabilities.liabilities, pcd.niRecord, mci, nino)
    } else {
      for {
        summary     <- nps.getSummary(nino)
        liabilities <- nps.getLiabilities(nino)
        niRecord    <- nps.getNIRecord(nino)
        mci         <- getMCI(summary, nino)
      } yield buildStatePension(summary, liabilities, niRecord, mci, nino)

    }

  private def buildStatePension(
    summary: Summary,
    liabilities: List[Liability],
    niRecord: NIRecord,
    mci: Boolean,
    nino: Nino
  )(
    implicit hc: HeaderCarrier
  ): Either[StatePensionExclusion, StatePension] = {

    val exclusions: List[Exclusion] = ExclusionService(
      dateOfDeath              = summary.dateOfDeath,
      pensionDate              = summary.statePensionAgeDate,
      now                      = now,
      entitlement              = summary.amounts.pensionEntitlement,
      startingAmount           = summary.amounts.startingAmount2016,
      liabilities              = liabilities,
      manualCorrespondenceOnly = mci,
      calculatedStartingAmount = forecastingService.calculateStartingAmount(
        amountA2016 = summary.amounts.amountA2016.total,
        amountB2016 = summary.amounts.amountB2016.mainComponent
      ),
      appConfig = appConfig
    ).getExclusions

    val purgedRecord = niRecord.purge(summary.finalRelevantStartYear)

    auditSummary(nino, summary, purgedRecord.qualifyingYears, exclusions)

    if (exclusions.nonEmpty) {

      metrics.exclusion(filterExclusions(exclusions))

      Left(StatePensionExclusion(
        exclusionReasons                  = exclusions,
        pensionAge                        = summary.statePensionAge,
        pensionDate                       = summary.statePensionAgeDate,
        statePensionAgeUnderConsideration =
          if (exclusions.contains(AmountDissonance) || exclusions.contains(IsleOfMan))
            checkStatePensionAgeUnderConsideration(summary.dateOfBirth)
          else
            false
      ))
    } else {

      val forecast = forecastingService.calculateForecastAmount(
        earningsIncludedUpTo   = summary.earningsIncludedUpTo,
        finalRelevantStartYear = summary.finalRelevantStartYear,
        currentAmount          = summary.amounts.pensionEntitlementRounded,
        qualifyingYears        = purgedRecord.qualifyingYears
      )

      val personalMaximum = forecastingService.calculatePersonalMaximum(
        earningsIncludedUpTo    = summary.earningsIncludedUpTo,
        finalRelevantStartYear  = summary.finalRelevantStartYear,
        qualifyingYearsPre2016  = purgedRecord.qualifyingYearsPre2016,
        qualifyingYearsPost2016 = purgedRecord.qualifyingYearsPost2016,
        payableGapsPre2016      = purgedRecord.payableGapsPre2016,
        payableGapsPost2016     = purgedRecord.payableGapsPost2016,
        additionalPension       = summary.amounts.amountA2016.totalAP,
        rebateDerivedAmount     = summary.amounts.amountB2016.rebateDerivedAmount
      )

      val oldRules = OldRules(
        basicStatePension          = summary.amounts.amountA2016.basicStatePension,
        additionalStatePension     = summary.amounts.amountA2016.additionalStatePension,
        graduatedRetirementBenefit = summary.amounts.amountA2016.graduatedRetirementBenefit
      )

      val newRules = NewRules(
        grossStatePension   = summary.amounts.amountB2016.mainComponent + summary.amounts.amountB2016.rebateDerivedAmount,
        rebateDerivedAmount = summary.amounts.amountB2016.rebateDerivedAmount
      )

      val amounts = StatePensionAmounts(
        protectedPayment = summary.amounts.protectedPayment2016 > 0,
        current          = StatePensionAmount(None, None, forecastingService.sanitiseCurrentAmount(summary.amounts.pensionEntitlementRounded, purgedRecord.qualifyingYears)),
        forecast         = StatePensionAmount(Some(forecast.yearsToWork), None, forecast.amount),
        maximum          = StatePensionAmount(Some(personalMaximum.yearsToWork), Some(personalMaximum.gapsToFill), personalMaximum.amount),
        cope             = StatePensionAmount(None, None, summary.amounts.amountB2016.rebateDerivedAmount),
        starting         = StatePensionAmount(None, None, summary.amounts.startingAmount2016),
        oldRules         = oldRules,
        newRules         = newRules
      )

      val statePension = StatePension(
        earningsIncludedUpTo                   = summary.earningsIncludedUpTo,
        amounts                                = amounts,
        pensionAge                             = summary.statePensionAge,
        pensionDate                            = summary.statePensionAgeDate,
        finalRelevantYear                      = summary.finalRelevantYear,
        numberOfQualifyingYears                = purgedRecord.qualifyingYears,
        pensionSharingOrder                    = summary.pensionSharingOrderSERPS,
        currentFullWeeklyPensionAmount         = rateService.MAX_AMOUNT,
        reducedRateElection                    = summary.reducedRateElection,
        reducedRateElectionCurrentWeeklyAmount = if (summary.reducedRateElection) Some(summary.amounts.pensionEntitlementRounded) else None,
        statePensionAgeUnderConsideration      = checkStatePensionAgeUnderConsideration(summary.dateOfBirth)
      )

      metrics.summary(
        forecast                               = statePension.amounts.forecast.weeklyAmount,
        current                                = statePension.amounts.current.weeklyAmount,
        contractedOut                          = statePension.contractedOut,
        forecastScenario                       = statePension.forecastScenario,
        personalMaximum                        = statePension.amounts.maximum.weeklyAmount,
        yearsToContribute                      = statePension.amounts.forecast.yearsToWork.getOrElse(0),
        mqpScenario                            = statePension.mqpScenario,
        starting                               = statePension.amounts.starting.weeklyAmount,
        basicStatePension                      = statePension.amounts.oldRules.basicStatePension,
        additionalStatePension                 = statePension.amounts.oldRules.additionalStatePension,
        graduatedRetirementBenefit             = statePension.amounts.oldRules.graduatedRetirementBenefit,
        grossStatePension                      = statePension.amounts.newRules.grossStatePension,
        rebateDerivedAmount                    = statePension.amounts.newRules.rebateDerivedAmount,
        reducedRateElection                    = statePension.reducedRateElection,
        reducedRateElectionCurrentWeeklyAmount = statePension.reducedRateElectionCurrentWeeklyAmount,
        statePensionAgeUnderConsideration      = statePension.statePensionAgeUnderConsideration
      )

      Right(statePension)
    }
  }

  private[services] def filterExclusions(exclusions: List[Exclusion]): Exclusion =
    if (exclusions.contains(Dead)) {
      Dead
    } else if (exclusions.contains(ManualCorrespondenceIndicator)) {
      ManualCorrespondenceIndicator
    } else if (exclusions.contains(PostStatePensionAge)) {
      PostStatePensionAge
    } else if (exclusions.contains(AmountDissonance)) {
      AmountDissonance
    } else if (exclusions.contains(IsleOfMan)) {
      IsleOfMan
    } else {
      throw new RuntimeException(s"Un-accounted for exclusion in NpsConnection: $exclusions")
    }

  private[services] def auditSummary(
    nino: Nino,
    summary: Summary,
    qualifyingYears: Int,
    exclusions: List[Exclusion]
  )(
    implicit hc: HeaderCarrier
  ): Unit =
    customAuditConnector.sendEvent(Forecasting(
      nino                   = nino,
      earningsIncludedUpTo   = summary.earningsIncludedUpTo,
      currentQualifyingYears = qualifyingYears,
      amountA                = summary.amounts.amountA2016,
      amountB                = summary.amounts.amountB2016,
      finalRelevantYear      = summary.finalRelevantStartYear,
      exclusions             = exclusions
    ))

  private final val CHANGE_SPA_MIN_DATE = LocalDate.of(1970, 4, 6)
  private final val CHANGE_SPA_MAX_DATE = LocalDate.of(1978, 4, 5)

  private def checkStatePensionAgeUnderConsideration(dateOfBirth: LocalDate): Boolean =
    !dateOfBirth.isBefore(CHANGE_SPA_MIN_DATE) && !dateOfBirth.isAfter(CHANGE_SPA_MAX_DATE)
}
