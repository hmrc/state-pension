/*
 * Copyright 2025 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.{eq as mockEq, *}
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Injecting
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.statepension.builders.RateServiceBuilder
import uk.gov.hmrc.statepension.config.{AppConfig, StatePensionExclusionOffset}
import uk.gov.hmrc.statepension.connectors.{NpsConnector, ProxyCacheConnector}
import uk.gov.hmrc.statepension.domain.*
import uk.gov.hmrc.statepension.domain.MQPScenario.ContinueWorking
import uk.gov.hmrc.statepension.domain.nps.*
import utils.{CopeRepositoryHelper, StatePensionBaseSpec}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class StatePensionServiceCustomerSpec
  extends StatePensionBaseSpec
    with ScalaFutures
    with GuiceOneAppPerSuite
    with Injecting
    with CopeRepositoryHelper {

  val mockNpsConnector: NpsConnector = mock[NpsConnector]
  val mockMetrics: ApplicationMetrics = mock[ApplicationMetrics]
  val mockRateService: RateService = inject[RateService]
  val defaultForecasting = new ForecastingService(RateServiceBuilder.default)
  val mockProxyCacheConnector: ProxyCacheConnector = mock[ProxyCacheConnector]
  val mockAppConfig: AppConfig = mock[AppConfig]

  def service(mci: Boolean = false): StatePensionService = new StatePensionService {
    val nps: NpsConnector = mockNpsConnector
    val proxyCacheConnector: ProxyCacheConnector = mockProxyCacheConnector
    override lazy val now: LocalDate = LocalDate.of(2017, 2, 16)
    override val forecastingService: ForecastingService = defaultForecasting
    override val rateService: RateService = RateServiceBuilder.default
    override val metrics: ApplicationMetrics = mockMetrics
    override val customAuditConnector: AuditConnector = mock[AuditConnector]
    override val appConfig: AppConfig = mockAppConfig
    override implicit val executionContext: ExecutionContext = inject[ExecutionContext]

    override def getMCI(summary: Summary, nino: Nino)(implicit hc: HeaderCarrier): Future[Boolean] =
      Future.successful(mci)

    override def checkPensionRequest: Boolean = true

    when(mockAppConfig.statePensionExclusionOffset)
      .thenReturn(StatePensionExclusionOffset(years = 0, months = 0, weeks = 0, days = 1))
  }

  private val summary: Summary =
    Summary(
      earningsIncludedUpTo = LocalDate.of(2016, 4, 5),
      statePensionAgeDate = LocalDate.of(2018, 1, 1),
      finalRelevantStartYear = 2049,
      pensionSharingOrderSERPS = false,
      dateOfBirth = LocalDate.of(1956, 7, 7)
    )

  private val niRecord: NIRecord =
    NIRecord(
      qualifyingYears = 35,
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

  private val liabilities: Liabilities =
    Liabilities(List())

  override def beforeEach(): Unit = {
    Mockito.reset(mockProxyCacheConnector)
    Mockito.reset(mockMetrics)
  }

  "StatePensionService with a HOD Connection" when {

    "the customer is dead" should {

      val summary = Summary(
        earningsIncludedUpTo = LocalDate.of(2016, 4, 5),
        statePensionAgeDate = LocalDate.of(2050, 7, 7),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = LocalDate.of(1983, 7, 7),
        dateOfDeath = Some(LocalDate.of(2000, 9, 13)),
        reducedRateElection = false,
        countryCode = 1,
        PensionAmounts(),
        manualCorrespondenceIndicator = None
      )

      when(mockProxyCacheConnector.get(any())(using any()))
        .thenReturn(Future.successful(ProxyCacheData(
          summary = summary,
          niRecord = niRecord,
          liabilities = liabilities
        )))

      val exclusion: StatePensionExclusion =
        await(service().getStatement(generateNino()).left.toOption.get)

      "return dead exclusion" in {
        exclusion.exclusionReasons shouldBe List(Exclusion.Dead)
      }

      "have a pension age of 67" in {
        exclusion.pensionAge shouldBe 67
      }

      "have a pension date of 2050-7-7" in {
        exclusion.pensionDate shouldBe LocalDate.of(2050, 7, 7)
      }

      "not have the statePensionAgeUnderConsideration flag enabled" in {
        exclusion.statePensionAgeUnderConsideration shouldBe false
      }

      "log an exclusion metric" in {
        when(mockProxyCacheConnector.get(any())(using any()))
          .thenReturn(Future.successful(ProxyCacheData(
            summary = summary,
            niRecord = niRecord,
            liabilities = liabilities
          )))

        service().getStatement(generateNino()).futureValue.left.toOption.get

        verify(mockMetrics, times(1)).exclusion(
          mockEq(Exclusion.Dead)
        )
      }

      "not log a summary metric" in {
        when(mockProxyCacheConnector.get(any())(using any()))
          .thenReturn(Future.successful(ProxyCacheData(
            summary = summary,
            niRecord = niRecord,
            liabilities = liabilities
          )))

        verify(mockMetrics, never).summary(any(), any(), any(),
          any(), any(), any(), any(), any(),
          any(), any(), any(), any(), any(),
          any(), any(), any())
      }

    }

    "the customer is over state pension age" should {

      val summary = Summary(
        earningsIncludedUpTo = LocalDate.of(1954, 4, 5),
        statePensionAgeDate = LocalDate.of(2016, 1, 1),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = LocalDate.of(1954, 7, 7),
        dateOfDeath = None,
        reducedRateElection = false,
        countryCode = 1,
        PensionAmounts(),
        manualCorrespondenceIndicator = None
      )

      when(mockProxyCacheConnector.get(any())(using any()))
        .thenReturn(Future.successful(ProxyCacheData(
          summary = summary,
          niRecord = niRecord,
          liabilities = liabilities
        )))

      val exclusion: StatePensionExclusion = await(service().getStatement(generateNino())).left.toOption.get

      "return post state pension age exclusion" in {
        exclusion.exclusionReasons shouldBe List(Exclusion.PostStatePensionAge)
      }

      "have a pension age of 61" in {
        exclusion.pensionAge shouldBe 61
      }

      "have a pension date of 2016-1-1" in {
        exclusion.pensionDate shouldBe LocalDate.of(2016, 1, 1)
      }

      "not have the statePensionAgeUnderConsideration flag enabled" in {
        exclusion.statePensionAgeUnderConsideration shouldBe false
      }

      "log an exclusion metric" in {
        when(mockProxyCacheConnector.get(any())(using any()))
          .thenReturn(Future.successful(ProxyCacheData(
            summary = summary,
            niRecord = niRecord,
            liabilities = liabilities
          )))

        service().getStatement(generateNino()).futureValue.left.toOption.get

        verify(mockMetrics, times(1)).exclusion(
          mockEq(Exclusion.PostStatePensionAge)
        )
      }

      "not log a summary metric" in {
        verify(mockMetrics, never).summary(any(), any(), any(), any(),
          any(), any(), any(), any(), any(), any(),
          any(), any(), any(), any(), any(), any())
      }
    }

    "the customer has married women's reduced rate election" should {

      val summary = Summary(
        earningsIncludedUpTo = LocalDate.of(2016, 4, 5),
        statePensionAgeDate = LocalDate.of(2018, 1, 1),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = LocalDate.of(1956, 7, 7),
        dateOfDeath = None,
        reducedRateElection = true,
        countryCode = 1,
        amounts = PensionAmounts(
          pensionEntitlement = 32.61,
          startingAmount2016 = 35.58,
          protectedPayment2016 = 0,
          AmountA2016(
            basicStatePension = 31.81
          ),
          AmountB2016(
            mainComponent = 35.58
          )
        ),
        manualCorrespondenceIndicator = None
      )

      when(mockProxyCacheConnector.get(any())(using any()))
        .thenReturn(Future.successful(ProxyCacheData(
          summary = summary,
          niRecord = niRecord,
          liabilities = liabilities
        )))

      val statePension: StatePension = await(service().getStatement(generateNino()).toOption.get)

      "statePension have RRE flag as true" in {
        statePension.reducedRateElection shouldBe true
      }

      "statePension" in {
        statePension.reducedRateElectionCurrentWeeklyAmount shouldBe Some(32.61)
      }

      "log a summary metric" in {
        when(mockProxyCacheConnector.get(any())(using any()))
          .thenReturn(Future.successful(ProxyCacheData(
            summary = summary,
            niRecord = NIRecord(
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
            ),
            liabilities = liabilities
          )))

        await(service().getStatement(generateNino()).toOption.get)

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
          mockEq(Some(BigDecimal(32.61))),
          mockEq(false)
        )
      }
    }

    "the customer has amount dissonance" should {

      val summary = Summary(
        earningsIncludedUpTo = LocalDate.of(2016, 4, 5),
        statePensionAgeDate = LocalDate.of(2018, 1, 1),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = LocalDate.of(1956, 7, 7),
        amounts = PensionAmounts(
          pensionEntitlement = 155.65,
          startingAmount2016 = 155.65,
          amountB2016 = AmountB2016(
            mainComponent = 155.64
          )
        ),
        manualCorrespondenceIndicator = None
      )

      when(mockProxyCacheConnector.get(any())(using any()))
        .thenReturn(Future.successful(ProxyCacheData(
          summary = summary,
          niRecord = niRecord,
          liabilities = liabilities
        )))

      val exclusion: StatePensionExclusion = await(service().getStatement(generateNino()).left.toOption.get)

      "return amount dissonance" in {
        exclusion.exclusionReasons shouldBe List(Exclusion.AmountDissonance)
      }

      "have a pension age of 61" in {
        exclusion.pensionAge shouldBe 61
      }

      "have a pension date of 2018-1-1" in {
        exclusion.pensionDate shouldBe LocalDate.of(2018, 1, 1)
      }

      "not have the statePensionAgeUnderConsideration flag enabled" in {
        exclusion.statePensionAgeUnderConsideration shouldBe false
      }

      "log an exclusion metric" in {
        when(mockProxyCacheConnector.get(any())(using any()))
          .thenReturn(Future.successful(ProxyCacheData(
            summary = summary,
            niRecord = niRecord,
            liabilities = liabilities
          )))

        await(service().getStatement(generateNino()).futureValue.left.toOption.get)

        verify(mockMetrics, times(1)).exclusion(
          mockEq(Exclusion.AmountDissonance)
        )
      }

      "not log a summary metric" in {
        when(mockProxyCacheConnector.get(any())(using any()))
          .thenReturn(Future.successful(ProxyCacheData(
            summary = summary,
            niRecord = niRecord,
            liabilities = liabilities
          )))

        verify(mockMetrics, never).summary(any(), any(), any(), any(),
          any(), any(), any(), any(), any(), any(),
          any(), any(), any(), any(), any(), any())
      }
    }

    "the customer has contributed national insurance in the isle of man" should {

      when(mockProxyCacheConnector.get(any())(using any()))
        .thenReturn(Future.successful(ProxyCacheData(
          summary = summary,
          niRecord = niRecord,
          liabilities = Liabilities(List(Liability(Some(5))))
        )))

      val exclusion: StatePensionExclusion = await(service().getStatement(generateNino()).left.toOption.get)


      "return isle of man exclusion" in {
        exclusion.exclusionReasons shouldBe List(Exclusion.IsleOfMan)
      }

      "have a pension age of 61" in {
        exclusion.pensionAge shouldBe 61
      }

      "have a pension date of 2018-1-1" in {
        exclusion.pensionDate shouldBe LocalDate.of(2018, 1, 1)
      }

      "not have the statePensionAgeUnderConsideration flag enabled" in {
        exclusion.statePensionAgeUnderConsideration shouldBe false
      }

      "log an exclusion metric" in {
        when(mockProxyCacheConnector.get(any())(using any()))
          .thenReturn(Future.successful(ProxyCacheData(
            summary = summary,
            niRecord = niRecord,
            liabilities = Liabilities(List(Liability(Some(5))))
          )))

        await(service().getStatement(generateNino()).left.toOption.get)

        verify(mockMetrics, times(1)).exclusion(
          mockEq(Exclusion.IsleOfMan)
        )
      }

      "not log a summary metric" in {
        when(mockProxyCacheConnector.get(any())(using any()))
          .thenReturn(Future.successful(ProxyCacheData(
            summary = summary,
            niRecord = niRecord,
            liabilities = Liabilities(List(Liability(Some(5))))
          )))

        verify(mockMetrics, never).summary(any(), any(), any(), any(),
          any(), any(), any(), any(), any(), any(),
          any(), any(), any(), any(), any(), any())
      }
    }

    "the customer has a manual correspondence indicator" should {
      when(mockProxyCacheConnector.get(any())(using any()))
        .thenReturn(Future.successful(ProxyCacheData(
          summary = summary,
          niRecord = niRecord,
          liabilities = liabilities
        )))

      val exclusion: StatePensionExclusion = await(service(true).getStatement(generateNino()).left.toOption.get)

      "return mci exclusion" in {
        exclusion.exclusionReasons shouldBe List(Exclusion.ManualCorrespondenceIndicator)
      }

      "have a pension age of 61" in {
        exclusion.pensionAge shouldBe 61
      }

      "have a pension date of 2018-1-1" in {
        exclusion.pensionDate shouldBe LocalDate.of(2018, 1, 1)
      }

      "not have the statePensionAgeUnderConsideration flag enabled" in {
        when(mockProxyCacheConnector.get(any())(using any()))
          .thenReturn(Future.successful(ProxyCacheData(
            summary = summary,
            niRecord = niRecord,
            liabilities = liabilities
          )))

        exclusion.statePensionAgeUnderConsideration shouldBe false
      }

      "log an exclusion metric" in {
        when(mockProxyCacheConnector.get(any())(using any()))
          .thenReturn(Future.successful(ProxyCacheData(
            summary = summary,
            niRecord = niRecord,
            liabilities = liabilities
          )))

        await(service(true).getStatement(generateNino()).left.toOption.get)

        verify(mockMetrics, times(1)).exclusion(
          mockEq(Exclusion.ManualCorrespondenceIndicator)
        )
      }

      "not log a summary metric" in {
        when(mockProxyCacheConnector.get(any())(using any()))
          .thenReturn(Future.successful(ProxyCacheData(
            summary = summary,
            niRecord = niRecord,
            liabilities = liabilities
          )))

        verify(mockMetrics, never).summary(any(), any(), any(), any(),
          any(), any(), any(), any(), any(), any(),
          any(), any(), any(), any(), any(), any())
      }
    }
  }
}
