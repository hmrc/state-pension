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

import org.mockito.Mockito.{times, verify, when}
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.statepension.builders.RateServiceBuilder
import uk.gov.hmrc.statepension.connectors.{NpsConnector, ProxyCacheConnector}
import uk.gov.hmrc.statepension.domain.MQPScenario.ContinueWorking
import uk.gov.hmrc.statepension.domain.nps._
import uk.gov.hmrc.statepension.domain.{Scenario, StatePension}
import uk.gov.hmrc.statepension.models.ProxyCacheToggle
import utils.StatePensionBaseSpec

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class StatePensionServiceAgeUnderConsiderationSpec extends StatePensionBaseSpec {

  val mockNpsConnector: NpsConnector = mock[NpsConnector]
  val mockMetrics: ApplicationMetrics = mock[ApplicationMetrics]
  val defaultForecasting = new ForecastingService(rateService = RateServiceBuilder.default)
  val mockProxyCacheConnector: ProxyCacheConnector = mock[ProxyCacheConnector]
  val mockFeatureFlagService: FeatureFlagService = mock[FeatureFlagService]

  lazy val service: StatePensionService = new StatePensionService {
    override lazy val now: LocalDate = LocalDate.of(2017, 2, 16)
    override val nps: NpsConnector = mockNpsConnector
    override val forecastingService: ForecastingService = defaultForecasting
    override val rateService: RateService = RateServiceBuilder.default
    override val metrics: ApplicationMetrics = mockMetrics
    override val customAuditConnector: AuditConnector = mock[AuditConnector]
    override implicit val executionContext: ExecutionContext = global

    override val proxyCacheConnector: ProxyCacheConnector = mockProxyCacheConnector
    override val featureFlagService: FeatureFlagService = mockFeatureFlagService

    override def getMCI(summary: Summary, nino: Nino): Future[Boolean] =
      Future.successful(false)

    override def checkPensionRequest: Boolean = true

    when(mockFeatureFlagService.get(ArgumentMatchers.any()))
      .thenReturn(Future.successful(FeatureFlag(ProxyCacheToggle, isEnabled = false, description = None)))
  }

  when(mockNpsConnector.getLiabilities(ArgumentMatchers.any())(ArgumentMatchers.any()))
    .thenReturn(Future.successful(
      List()
    ))

  def regularStatementWithDateOfBirth(dateOfBirth: LocalDate, statePensionAgeDate: LocalDate): Summary = {
    Summary(
      earningsIncludedUpTo = LocalDate.of(2016, 4, 5),
      statePensionAgeDate = statePensionAgeDate,
      finalRelevantStartYear = 2018,
      pensionSharingOrderSERPS = false,
      dateOfBirth = dateOfBirth,
      amounts = PensionAmounts(
        pensionEntitlement = 161.18,
        startingAmount2016 = 161.18,
        protectedPayment2016 = 5.53,
        AmountA2016(
          basicStatePension = 119.3,
          pre97AP = 17.79,
          post97AP = 6.03,
          post02AP = 15.4,
          graduatedRetirementBenefit = 2.66
        ),
        AmountB2016(
          mainComponent = 155.65
        )
      ),
      manualCorrespondenceIndicator = None
    )
  }

  "StatePensionService with a HOD Connection" when {

    "the customer has state pension age under consideration flag set to false as the date of birth is before the required range " should {

      val statePensionAgeDate = LocalDate.of(2034, 4, 5)
      val dateOfBirth = LocalDate.of(1970, 4, 5)
      val regularStatement = regularStatementWithDateOfBirth(dateOfBirth, statePensionAgeDate)

      when(mockNpsConnector.getLiabilities(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
        List()
      ))

      when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
        NIRecord(qualifyingYears = 36, List())
      ))

      lazy val statePension: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

      "statePension have statePensionAgeUnderConsideration flag as false" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
          regularStatement
        ))

        statePension.statePensionAgeUnderConsideration shouldBe false
      }

      "log a summary metric" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
          regularStatement
        ))

        verify(mockMetrics, Mockito.atLeastOnce()).summary(
          ArgumentMatchers.eq[BigDecimal](161.18),
          ArgumentMatchers.eq[BigDecimal](161.18),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(Scenario.Reached),
          ArgumentMatchers.eq[BigDecimal](161.18),
          ArgumentMatchers.eq(0),
          ArgumentMatchers.eq(None),
          ArgumentMatchers.eq[BigDecimal](161.18),
          ArgumentMatchers.eq[BigDecimal](119.3),
          ArgumentMatchers.eq[BigDecimal](39.22),
          ArgumentMatchers.eq[BigDecimal](2.66),
          ArgumentMatchers.eq[BigDecimal](155.65),
          ArgumentMatchers.eq[BigDecimal](0),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(None),
          ArgumentMatchers.eq(false)
        )
      }
    }

    "the customer has state pension age under consideration flag set to true as the date of birth is at the minimum of the required range " should {

      val statePensionAgeDate = LocalDate.of(2034, 4, 6)
      val dateOfBirth = LocalDate.of(1970, 4, 6)
      val regularStatement = regularStatementWithDateOfBirth(dateOfBirth, statePensionAgeDate)

      when(mockNpsConnector.getLiabilities(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
        List()
      ))

      when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
        NIRecord(qualifyingYears = 36, List())
      ))

      lazy val statePension: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

      "statePension have statePensionAgeUnderConsideration flag as true" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
          regularStatement
        ))

        statePension.statePensionAgeUnderConsideration shouldBe true
      }

      "log a summary metric" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
          regularStatement
        ))

        verify(mockMetrics, Mockito.atLeastOnce()).summary(
          ArgumentMatchers.eq[BigDecimal](161.18),
          ArgumentMatchers.eq[BigDecimal](161.18),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(Scenario.Reached),
          ArgumentMatchers.eq[BigDecimal](161.18),
          ArgumentMatchers.eq(0),
          ArgumentMatchers.eq(None),
          ArgumentMatchers.eq[BigDecimal](161.18),
          ArgumentMatchers.eq[BigDecimal](119.3),
          ArgumentMatchers.eq[BigDecimal](39.22),
          ArgumentMatchers.eq[BigDecimal](2.66),
          ArgumentMatchers.eq[BigDecimal](155.65),
          ArgumentMatchers.eq[BigDecimal](0),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(None),
          ArgumentMatchers.eq(true)
        )
      }
    }

    "the customer has state pension age under consideration flag set to true as the date of birth is in the middle of the required range " should {

      val summary = Summary(
        earningsIncludedUpTo = LocalDate.of(2016, 4, 5),
        statePensionAgeDate = LocalDate.of(2038, 1, 1),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = LocalDate.of(1976, 7, 7),
        dateOfDeath = None,
        reducedRateElection = true,
        countryCode = 1,
        amounts = PensionAmounts(
          pensionEntitlement = 32.61,
          startingAmount2016 = 35.58,
          protectedPayment2016 = 0,
          AmountA2016(
            basicStatePension = 31.81,
            pre97AP = 0,
            post97AP = 0,
            post02AP = 0,
            pre88GMP = 0,
            post88GMP = 0,
            pre88COD = 0,
            post88COD = 0,
            graduatedRetirementBenefit = 0
          ),
          AmountB2016(
            mainComponent = 35.58,
            rebateDerivedAmount = 0
          )
        ),
        manualCorrespondenceIndicator = None
      )

      lazy val statePension: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

      "statePension have statePensionAgeUnderConsideration flag as true" in {

        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
          summary
        ))

        when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
          NIRecord(qualifyingYears = 9, List(NITaxYear(Some(2000), Some(false), Some(false), Some(true)), NITaxYear(Some(2001), Some(false), Some(false), Some(true))))
        ))

        statePension.statePensionAgeUnderConsideration shouldBe true
      }

      "log a summary metric" in {

        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
          summary
        ))

        when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
          NIRecord(qualifyingYears = 9, List(NITaxYear(Some(2000), Some(false), Some(false), Some(true)), NITaxYear(Some(2001), Some(false), Some(false), Some(true))))
        ))

        verify(mockMetrics, times(1)).summary(
          ArgumentMatchers.eq[BigDecimal](155.65),
          ArgumentMatchers.eq[BigDecimal](0),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(Scenario.ContinueWorkingMax),
          ArgumentMatchers.eq[BigDecimal](155.65),
          ArgumentMatchers.eq(28),
          ArgumentMatchers.eq(Some(ContinueWorking)),
          ArgumentMatchers.eq[BigDecimal](35.58),
          ArgumentMatchers.eq[BigDecimal](31.81),
          ArgumentMatchers.eq[BigDecimal](0),
          ArgumentMatchers.eq[BigDecimal](0),
          ArgumentMatchers.eq[BigDecimal](35.58),
          ArgumentMatchers.eq[BigDecimal](0),
          ArgumentMatchers.eq(true),
          ArgumentMatchers.eq(Some(32.61)),
          ArgumentMatchers.eq(true)
        )
      }
    }

    "the customer has state pension age under consideration flag set to true as the date of birth is at the maximum of the required range " should {

      val dateOfBirth = LocalDate.of(1978, 4, 5)

      val statePensionAgeDate = LocalDate.of(2042, 4, 5)

      val regularStatement = regularStatementWithDateOfBirth(dateOfBirth, statePensionAgeDate)

      lazy val statePension: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

      "statePension have statePensionAgeUnderConsideration flag as true" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(regularStatement))

        when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(
            NIRecord(qualifyingYears = 36, List())
          ))

        statePension.statePensionAgeUnderConsideration shouldBe true
      }

      "log a summary metric" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(regularStatement))

        when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(
            NIRecord(qualifyingYears = 36, List())
          ))

        verify(mockMetrics, Mockito.atLeastOnce()).summary(
          ArgumentMatchers.eq[BigDecimal](161.18),
          ArgumentMatchers.eq[BigDecimal](161.18),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(Scenario.Reached),
          ArgumentMatchers.eq[BigDecimal](161.18),
          ArgumentMatchers.eq(0),
          ArgumentMatchers.eq(None),
          ArgumentMatchers.eq[BigDecimal](161.18),
          ArgumentMatchers.eq[BigDecimal](119.3),
          ArgumentMatchers.eq[BigDecimal](39.22),
          ArgumentMatchers.eq[BigDecimal](2.66),
          ArgumentMatchers.eq[BigDecimal](155.65),
          ArgumentMatchers.eq[BigDecimal](0),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(None),
          ArgumentMatchers.eq(true)
        )
      }
    }

    "the customer has state pension age under consideration flag set to false as the date of birth is after the required range " should {

      val dateOfBirth = LocalDate.of(1978, 4, 6)

      val statePensionAgeDate = LocalDate.of(2042, 4, 6)

      val regularStatement = regularStatementWithDateOfBirth(dateOfBirth, statePensionAgeDate)

      lazy val statePension: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

      "statePension have statePensionAgeUnderConsideration flag as false" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(regularStatement))

        when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(
            NIRecord(qualifyingYears = 36, List())
          ))

        statePension.statePensionAgeUnderConsideration shouldBe false
      }

      "log a summary metric" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(regularStatement))

        when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(
            NIRecord(qualifyingYears = 36, List())
          ))

        verify(mockMetrics, Mockito.atLeastOnce()).summary(
          ArgumentMatchers.eq[BigDecimal](161.18),
          ArgumentMatchers.eq[BigDecimal](161.18),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(Scenario.Reached),
          ArgumentMatchers.eq[BigDecimal](161.18),
          ArgumentMatchers.eq(0),
          ArgumentMatchers.eq(None),
          ArgumentMatchers.eq[BigDecimal](161.18),
          ArgumentMatchers.eq[BigDecimal](119.3),
          ArgumentMatchers.eq[BigDecimal](39.22),
          ArgumentMatchers.eq[BigDecimal](2.66),
          ArgumentMatchers.eq[BigDecimal](155.65),
          ArgumentMatchers.eq[BigDecimal](0),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(None),
          ArgumentMatchers.eq(false)
        )
      }
    }
  }
}
