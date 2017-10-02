/*
 * Copyright 2017 HM Revenue & Customs
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

import org.joda.time.{DateTimeZone, LocalDate, Period, PeriodType}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.statepension.connectors.{CustomAuditConnector, NispConnector, NpsConnector}
import uk.gov.hmrc.statepension.domain._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import play.api.Play.current
import uk.gov.hmrc.statepension.domain.Exclusion.Exclusion
import uk.gov.hmrc.statepension.domain.nps.APIType.CitizenDetails
import uk.gov.hmrc.statepension.domain.nps.{Country, NpsSummary}
import uk.gov.hmrc.statepension.events.Forecasting
import uk.gov.hmrc.statepension.util.EitherReads._
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.Future

trait StatePensionService {
  def getStatement(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExclusion, StatePension]]
}

trait NispConnection extends StatePensionService {
  val nisp = NispConnector

  override def getStatement(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExclusion, StatePension]] = {
    nisp.getStatePension(nino)
  }
}

trait NpsConnection extends StatePensionService {
  def nps: NpsConnector

  def citizenDetailsService: CitizenDetailsService
  def forecastingService: ForecastingService
  def rateService: RateService

  def now: LocalDate

  def metrics: Metrics

  def customAuditConnector: CustomAuditConnector

  override def getStatement(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExclusion, StatePension]] = {

    val summaryF = nps.getSummary(nino)
    val liablitiesF = nps.getLiabilities(nino)
    val manualCorrespondenceF = citizenDetailsService.checkManualCorrespondenceIndicator(nino)
    val niRecordF = nps.getNIRecord(nino)

    for (
      summary <- summaryF;
      liablities <- liablitiesF;
      niRecord <- niRecordF;
      manualCorrespondence <- manualCorrespondenceF
    ) yield {

      val exclusions: List[Exclusion] = new ExclusionService(
        dateOfDeath = summary.dateOfDeath,
        pensionDate = summary.statePensionAgeDate,
        now,
        reducedRateElection = summary.reducedRateElection,
        isAbroad = Country.isAbroad(summary.countryCode),
        sex = summary.sex,
        summary.amounts.pensionEntitlement,
        summary.amounts.startingAmount2016,
        forecastingService.calculateStartingAmount(summary.amounts.amountA2016.total, summary.amounts.amountB2016.mainComponent),
        liablities,
        manualCorrespondence
      ).getExclusions

      val purgedRecord = niRecord.purge(summary.finalRelevantStartYear)

      auditNPSSummary(nino, summary, purgedRecord.qualifyingYears, exclusions)

      if (exclusions.nonEmpty) {

        metrics.exclusion(filterExclusions(exclusions))

        Left(StatePensionExclusion(
          exclusionReasons = exclusions,
          pensionAge = summary.statePensionAge,
          pensionDate = summary.statePensionAgeDate
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
            oldRules = OldRules(basicStatePension = summary.amounts.amountA2016.basicStatePension,
                                additionalStatePension = summary.amounts.amountA2016.additionalStatePension,
                                graduatedRetirementBenefit = summary.amounts.amountA2016.graduatedRetirementBenefit)
          ),
          pensionAge = summary.statePensionAge,
          pensionDate = summary.statePensionAgeDate,
          finalRelevantYear = summary.finalRelevantYear,
          numberOfQualifyingYears = purgedRecord.qualifyingYears,
          pensionSharingOrder = summary.pensionSharingOrderSERPS,
          currentFullWeeklyPensionAmount = rateService.MAX_AMOUNT,
          summary.reducedRateElection
        )

        metrics.summary(statePension.amounts.forecast.weeklyAmount, statePension.amounts.current.weeklyAmount,
          statePension.contractedOut, statePension.forecastScenario, statePension.amounts.maximum.weeklyAmount,
          statePension.amounts.forecast.yearsToWork.getOrElse(0), statePension.mqpScenario,
          summary.reducedRateElection,
          additionalStatePension=summary.amounts.amountA2016.additionalStatePension,
          graduatedRetirementBenefit=summary.amounts.amountA2016.graduatedRetirementBenefit)

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
    } else if (exclusions.contains(Exclusion.Abroad)) {
      Exclusion.Abroad
    } else {
      throw new RuntimeException(s"Un-accounted for exclusion in NpsConnection: $exclusions")
    }
  }

  private[services] def auditNPSSummary(nino: Nino, summary: NpsSummary, qualifyingYears: Int, exclusions: List[Exclusion])(implicit hc: HeaderCarrier): Unit = {
    //Audit NPS Data used in calculation
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
}

object StatePensionServiceViaNisp extends StatePensionService with NispConnection

object StatePensionService extends StatePensionService with NpsConnection {
  override lazy val nps: NpsConnector = NpsConnector
  override lazy val citizenDetailsService: CitizenDetailsService = CitizenDetailsService
  override lazy val forecastingService: ForecastingService = ForecastingService
  override lazy val rateService: RateService = RateService
  override lazy val metrics: Metrics = Metrics
  override val customAuditConnector: CustomAuditConnector = CustomAuditConnector
  override def now: LocalDate = LocalDate.now(DateTimeZone.forTimeZone(TimeZone.getTimeZone("Europe/London")))
}

object SandboxStatePensionService extends StatePensionService {
  private val dummyStatement: StatePension = StatePension(
    // scalastyle:off magic.number
    earningsIncludedUpTo = new LocalDate(2015, 4, 5),
    amounts = StatePensionAmounts(
      protectedPayment = false,
      current = StatePensionAmount(
        None,
        None,
        133.41
      ),
      forecast = StatePensionAmount(
        yearsToWork = Some(3),
        None,
        146.76
      ),
      maximum = StatePensionAmount(
        yearsToWork = Some(3),
        gapsToFill = Some(2),
        weeklyAmount = 155.65
      ),
      cope = StatePensionAmount(
        None,
        None,
        0.00
      ),
      oldRules = OldRules(basicStatePension = 119.30,
                          additionalStatePension = 38.9,
                          graduatedRetirementBenefit = 10.00)
    ),
    pensionAge = 64,
    pensionDate = new LocalDate(2018, 7, 6),
    finalRelevantYear = "2017-18",
    numberOfQualifyingYears = 30,
    pensionSharingOrder = false,
    currentFullWeeklyPensionAmount = 155.65,
    reducedRateElection = false
  )
  private val defaultResponse = Right(dummyStatement)

  private val resourcePath = "conf/resources/sandbox/"

  private def getFileFromPrefix(nino: Nino): Either[StatePensionExclusion, StatePension] = {
    val prefix = nino.toString.substring(0, 2)
    play.api.Play.getExistingFile(resourcePath + prefix + ".json") match {
      case Some(file) => Json.parse(scala.io.Source.fromFile(file).mkString).as[Either[StatePensionExclusion, StatePension]]
      case None => Logger.info(s"Sandbox: Resource not found for $prefix, using default"); defaultResponse
    }
  }

  override def getStatement(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExclusion, StatePension]] = Future(getFileFromPrefix(nino))
}
