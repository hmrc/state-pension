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

import org.mockito.ArgumentMatchers.{eq => mockEq, _}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
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

class StatePensionServiceAgeUnderConsiderationProxyCacheSpec extends StatePensionBaseSpec {

  val mockNpsConnector: NpsConnector = mock[NpsConnector]
  val mockMetrics: ApplicationMetrics = mock[ApplicationMetrics]
  val defaultForecasting = new ForecastingService(rateService = RateServiceBuilder.default)
  val mockProxyCacheConnector: ProxyCacheConnector = mock[ProxyCacheConnector]
  val mockFeatureFlagService: FeatureFlagService = mock[FeatureFlagService]

  val service: StatePensionService = new StatePensionService {
    override lazy val now: LocalDate = LocalDate.of(2017, 2, 16)
    override val nps: NpsConnector = mockNpsConnector
    override val forecastingService: ForecastingService = defaultForecasting
    override val rateService: RateService = RateServiceBuilder.default
    override val metrics: ApplicationMetrics = mockMetrics
    override val customAuditConnector: AuditConnector = mock[AuditConnector]
    override implicit val executionContext: ExecutionContext = global

    override val proxyCacheConnector: ProxyCacheConnector = mockProxyCacheConnector
    override val featureFlagService: FeatureFlagService = mockFeatureFlagService

    override def getMCI(summary: Summary, nino: Nino)(implicit hc: HeaderCarrier): Future[Boolean] =
      Future.successful(false)

    when(mockFeatureFlagService.get(any()))
      .thenReturn(Future.successful(FeatureFlag(ProxyCacheToggle, isEnabled = true, description = None)))
  }

  def regularStatementWithDateOfBirth(
    dateOfBirth: LocalDate,
    statePensionAgeDate: LocalDate
  ): Summary =
    Summary(
      earningsIncludedUpTo          = LocalDate.of(2016, 4, 5),
      statePensionAgeDate           = statePensionAgeDate,
      finalRelevantStartYear        = 2018,
      pensionSharingOrderSERPS      = false,
      dateOfBirth                   = dateOfBirth,
      amounts                       = PensionAmounts(
        pensionEntitlement   = 161.18,
        startingAmount2016   = 161.18,
        protectedPayment2016 = 5.53,
        amountA2016                 = AmountA2016(
          basicStatePension          = 119.3,
          pre97AP                    = 17.79,
          post97AP                   = 6.03,
          post02AP                   = 15.4,
          graduatedRetirementBenefit = 2.66
        ),
        amountB2016                 = AmountB2016(
          mainComponent = 155.65
        )
      ),
      manualCorrespondenceIndicator = None
    )

  private val liabilities: Liabilities =
    Liabilities(List())

  private val niRecord: NIRecord =
    NIRecord(
      qualifyingYears = 9,
      taxYears = List(
        NITaxYear(
          startTaxYear = Some(2000),
          qualifying = Some(false),
          underInvestigation = Some(false),
          payableFlag = Some(true)
        ),
        NITaxYear(
          startTaxYear = Some(2001),
          qualifying = Some(false),
          underInvestigation = Some(false),
          payableFlag = Some(true)
        )
      )
    )

  "StatePensionService with a HOD Connection" when {

    "the customer has state pension age under consideration flag set to false" +
      " as the date of birth is before the required range" should {

      val statePensionAgeDate = LocalDate.of(2034, 4, 5)
      val dateOfBirth = LocalDate.of(1970, 4, 5)
      val regularStatement = regularStatementWithDateOfBirth(dateOfBirth, statePensionAgeDate)

      when(mockProxyCacheConnector.getProxyCacheData(any())(any()))
        .thenReturn(Future.successful(ProxyCacheData(
          summary = regularStatement,
          niRecord = NIRecord(qualifyingYears = 36, List()),
          liabilities = liabilities
        )))

      val statePension: StatePension = await(service.getStatement(generateNino()).toOption.get)

      "have statePension statePensionAgeUnderConsideration flag as false" in {

        statePension.statePensionAgeUnderConsideration shouldBe false
      }

      "log a summary metric" in {
        when(mockProxyCacheConnector.getProxyCacheData(any())(any()))
          .thenReturn(Future.successful(ProxyCacheData(
            summary = regularStatement,
            niRecord = NIRecord(qualifyingYears = 36, List()),
            liabilities = liabilities
          )))

        verify(mockMetrics, Mockito.atLeastOnce()).summary(
          mockEq[BigDecimal](161.18),
          mockEq[BigDecimal](161.18),
          mockEq(false),
          mockEq(Scenario.Reached),
          mockEq[BigDecimal](161.18),
          mockEq(0),
          mockEq(None),
          mockEq[BigDecimal](161.18),
          mockEq[BigDecimal](119.3),
          mockEq[BigDecimal](39.22),
          mockEq[BigDecimal](2.66),
          mockEq[BigDecimal](155.65),
          mockEq[BigDecimal](0),
          mockEq(false),
          mockEq(None),
          mockEq(false)
        )
      }
    }

    "the customer has state pension age under consideration flag set to true" +
      " as the date of birth is at the minimum of the required range" should {

      val statePensionAgeDate = LocalDate.of(2034, 4, 6)
      val dateOfBirth = LocalDate.of(1970, 4, 6)
      val regularStatement = regularStatementWithDateOfBirth(dateOfBirth, statePensionAgeDate)

      when(mockProxyCacheConnector.getProxyCacheData(any())(any()))
        .thenReturn(Future.successful(ProxyCacheData(
          summary = regularStatement,
          niRecord = NIRecord(qualifyingYears = 36, List()),
          liabilities = liabilities
        )))

      val statePension: StatePension = await(service.getStatement(generateNino()).toOption.get)

      "have statePension statePensionAgeUnderConsideration flag as true" in {

        statePension.statePensionAgeUnderConsideration shouldBe true
      }

      "log a summary metric" in {
        when(mockProxyCacheConnector.getProxyCacheData(any())(any()))
          .thenReturn(Future.successful(ProxyCacheData(
            summary = regularStatement,
            niRecord = NIRecord(qualifyingYears = 36, List()),
            liabilities = liabilities
          )))

        verify(mockMetrics, Mockito.atLeastOnce()).summary(
          mockEq[BigDecimal](161.18),
          mockEq[BigDecimal](161.18),
          mockEq(false),
          mockEq(Scenario.Reached),
          mockEq[BigDecimal](161.18),
          mockEq(0),
          mockEq(None),
          mockEq[BigDecimal](161.18),
          mockEq[BigDecimal](119.3),
          mockEq[BigDecimal](39.22),
          mockEq[BigDecimal](2.66),
          mockEq[BigDecimal](155.65),
          mockEq[BigDecimal](0),
          mockEq(false),
          mockEq(None),
          mockEq(true)
        )
      }
    }

    "the customer has state pension age under consideration flag set to true" +
      " as the date of birth is in the middle of the required range" should {

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
          ),
          AmountB2016(
            mainComponent = 35.58,
          )
        ),
        manualCorrespondenceIndicator = None
      )

      when(mockProxyCacheConnector.getProxyCacheData(any())(any()))
        .thenReturn(Future.successful(ProxyCacheData(
          summary = summary,
          niRecord = niRecord,
          liabilities = liabilities
        )))

      val statePension: StatePension = await(service.getStatement(generateNino()).toOption.get)

      "have statePension statePensionAgeUnderConsideration flag as true" in {

        statePension.statePensionAgeUnderConsideration shouldBe true
      }

      "log a summary metric" in {

        when(mockProxyCacheConnector.getProxyCacheData(any())(any()))
          .thenReturn(Future.successful(ProxyCacheData(
            summary = summary,
            niRecord = niRecord,
            liabilities = liabilities
          )))

        verify(mockMetrics, times(1)).summary(
          mockEq[BigDecimal](155.65),
          mockEq[BigDecimal](0),
          mockEq(false),
          mockEq(Scenario.ContinueWorkingMax),
          mockEq[BigDecimal](155.65),
          mockEq(28),
          mockEq(Some(ContinueWorking)),
          mockEq[BigDecimal](35.58),
          mockEq[BigDecimal](31.81),
          mockEq[BigDecimal](0),
          mockEq[BigDecimal](0),
          mockEq[BigDecimal](35.58),
          mockEq[BigDecimal](0),
          mockEq(true),
          mockEq(Some(32.61)),
          mockEq(true)
        )
      }
    }

    "the customer has state pension age under consideration flag set to true" +
      " as the date of birth is at the maximum of the required range " should {

      val dateOfBirth = LocalDate.of(1978, 4, 5)

      val statePensionAgeDate = LocalDate.of(2042, 4, 5)

      val regularStatement = regularStatementWithDateOfBirth(dateOfBirth, statePensionAgeDate)

      when(mockProxyCacheConnector.getProxyCacheData(any())(any()))
        .thenReturn(Future.successful(ProxyCacheData(
          summary = regularStatement,
          niRecord = NIRecord(qualifyingYears = 36, List()),
          liabilities = liabilities
        )))

      val statePension: StatePension = await(service.getStatement(generateNino()).toOption.get)

      "have statePension statePensionAgeUnderConsideration flag as true" in {

        statePension.statePensionAgeUnderConsideration shouldBe true
      }

      "log a summary metric" in {
        when(mockProxyCacheConnector.getProxyCacheData(any())(any()))
          .thenReturn(Future.successful(ProxyCacheData(
            summary = regularStatement,
            niRecord = NIRecord(qualifyingYears = 36, List()),
            liabilities = liabilities
          )))

        verify(mockMetrics, Mockito.atLeastOnce()).summary(
          mockEq[BigDecimal](161.18),
          mockEq[BigDecimal](161.18),
          mockEq(false),
          mockEq(Scenario.Reached),
          mockEq[BigDecimal](161.18),
          mockEq(0),
          mockEq(None),
          mockEq[BigDecimal](161.18),
          mockEq[BigDecimal](119.3),
          mockEq[BigDecimal](39.22),
          mockEq[BigDecimal](2.66),
          mockEq[BigDecimal](155.65),
          mockEq[BigDecimal](0),
          mockEq(false),
          mockEq(None),
          mockEq(true)
        )
      }
    }

    "the customer has state pension age under consideration flag set to false" +
      " as the date of birth is after the required range" should {

      val dateOfBirth = LocalDate.of(1978, 4, 6)

      val statePensionAgeDate = LocalDate.of(2042, 4, 6)

      val regularStatement = regularStatementWithDateOfBirth(dateOfBirth, statePensionAgeDate)

      when(mockProxyCacheConnector.getProxyCacheData(any())(any()))
        .thenReturn(Future.successful(ProxyCacheData(
          summary = regularStatement,
          niRecord = NIRecord(qualifyingYears = 36, List()),
          liabilities = liabilities
        )))

      val statePension: StatePension = await(service.getStatement(generateNino()).toOption.get)

      "have statePension statePensionAgeUnderConsideration flag as false" in {

        statePension.statePensionAgeUnderConsideration shouldBe false
      }

      "log a summary metric" in {
        when(mockProxyCacheConnector.getProxyCacheData(any())(any()))
          .thenReturn(Future.successful(ProxyCacheData(
            summary = regularStatement,
            niRecord = NIRecord(qualifyingYears = 36, List()),
            liabilities = liabilities
          )))

        verify(mockMetrics, Mockito.atLeastOnce()).summary(
          mockEq[BigDecimal](161.18),
          mockEq[BigDecimal](161.18),
          mockEq(false),
          mockEq(Scenario.Reached),
          mockEq[BigDecimal](161.18),
          mockEq(0),
          mockEq(None),
          mockEq[BigDecimal](161.18),
          mockEq[BigDecimal](119.3),
          mockEq[BigDecimal](39.22),
          mockEq[BigDecimal](2.66),
          mockEq[BigDecimal](155.65),
          mockEq[BigDecimal](0),
          mockEq(false),
          mockEq(None),
          mockEq(false)
        )
      }
    }
  }
}
