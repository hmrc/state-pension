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

import org.mockito.Mockito.{never, times, verify, when}
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Injecting
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.statepension.builders.RateServiceBuilder
import uk.gov.hmrc.statepension.config.{AppConfig, StatePensionExclusionOffset}
import uk.gov.hmrc.statepension.connectors.{NpsConnector, ProxyCacheConnector}
import uk.gov.hmrc.statepension.domain.nps._
import uk.gov.hmrc.statepension.domain.{Exclusion, Scenario, StatePension}
import uk.gov.hmrc.statepension.models.ProxyCacheToggle
import utils.{CopeRepositoryHelper, StatePensionBaseSpec}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class StatePensionServiceStatementSpec extends StatePensionBaseSpec
  with GuiceOneAppPerSuite
  with ScalaFutures
  with Injecting
  with CopeRepositoryHelper {

  val mockNpsConnector: NpsConnector = mock[NpsConnector]
  val mockMetrics: ApplicationMetrics = mock[ApplicationMetrics]
  val fakeRateService: RateService = RateServiceBuilder.default
  val defaultForecasting: ForecastingService = new ForecastingService(fakeRateService)
  val mockProxyCacheConnector: ProxyCacheConnector = mock[ProxyCacheConnector]
  val mockFeatureFlagService: FeatureFlagService = mock[FeatureFlagService]
  val mockAppConfig: AppConfig = mock[AppConfig]

  lazy val service: StatePensionService = new StatePensionService {
    val nps: NpsConnector = mockNpsConnector
    val proxyCacheConnector: ProxyCacheConnector = mockProxyCacheConnector
    val featureFlagService: FeatureFlagService = mockFeatureFlagService
    override lazy val now: LocalDate = LocalDate.of(2017, 2, 16)
    override val forecastingService: ForecastingService = defaultForecasting
    override val rateService: RateService = fakeRateService
    override val metrics: ApplicationMetrics = mockMetrics
    override val customAuditConnector: AuditConnector = mock[AuditConnector]
    override val appConfig: AppConfig = mockAppConfig
    override implicit val executionContext: ExecutionContext = inject[ExecutionContext]

    override def checkPensionRequest: Boolean = true

    override def getMCI(summary: Summary, nino: Nino)(implicit hc: HeaderCarrier): Future[Boolean] =
      Future.successful(false)

    when(mockFeatureFlagService.get(ArgumentMatchers.any()))
      .thenReturn(Future.successful(FeatureFlag(ProxyCacheToggle, isEnabled = false, description = None)))

    when(mockAppConfig.statePensionExclusionOffset)
      .thenReturn(StatePensionExclusionOffset(years = 0, months = 0, weeks = 0, days = 1))
  }

  override def beforeEach(): Unit = {
    Mockito.reset(mockNpsConnector)
    Mockito.reset(mockMetrics)

    when(mockNpsConnector.getLiabilities(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(List()))

    when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(
        NIRecord(qualifyingYears = 20, List(NITaxYear(Some(2000), Some(false), Some(false), Some(true)),
          NITaxYear(Some(2001), Some(false), Some(false), Some(true))))
      ))

  }

  "StatePensionService with a HOD Connection" when {

    "there are no exclusions" when {
      "there is a regular statement (Reached)" should {

        val regularStatement = Summary(
          earningsIncludedUpTo = LocalDate.of(2016, 4, 5),
          statePensionAgeDate = LocalDate.of(2019, 9, 6),
          finalRelevantStartYear = 2018,
          pensionSharingOrderSERPS = false,
          dateOfBirth = LocalDate.of(1954, 3, 9),
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

        "log a summary metric" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          service.getStatement(generateNino()).futureValue.toOption.get

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

        "not log an exclusion metric" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          //val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          verify(mockMetrics, never).exclusion(ArgumentMatchers.any())
        }

        "return earningsIncludedUpTo of 2016-4-5" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.earningsIncludedUpTo shouldBe LocalDate.of(2016, 4, 5)
        }

        "return qualifying years of 36" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.numberOfQualifyingYears shouldBe 36
        }

        "return pension date of 2019-9-6" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.pensionDate shouldBe LocalDate.of(2019, 9, 6)
        }

        "return oldRules additionalStatePension as 39.22" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.oldRules.additionalStatePension shouldBe 39.22
        }

        "return oldRules graduatedRetirementsBenefit as 2.66" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.oldRules.graduatedRetirementBenefit shouldBe 2.66
        }

        "return oldRules basicStatePension as 119.3" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(regularStatement))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.oldRules.basicStatePension shouldBe 119.3
        }

        "return newRules grossStatePension as 155.65" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.newRules.grossStatePension shouldBe 155.65
        }

        "return newRules rebateDerivedAmount as 0.00" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.newRules.rebateDerivedAmount shouldBe 0.00
        }

        "return RRECurrentWeeklyAmount as None" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.reducedRateElectionCurrentWeeklyAmount shouldBe None
        }

        "return reducedRateElection as false" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.reducedRateElection shouldBe false
        }

        "return statePensionAgeUnderConsideration as false" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.statePensionAgeUnderConsideration shouldBe false
        }

        "return final relevant year" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.finalRelevantYear shouldBe "2018-19"
        }

        "when there is a pensionSharingOrder return true" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement.copy(pensionSharingOrderSERPS = true)))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          service.getStatement(generateNino()).futureValue.toOption.get.pensionSharingOrder shouldBe true
        }

        "when there is no pensionSharingOrder return false" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement.copy(pensionSharingOrderSERPS = false)))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          service.getStatement(generateNino()).futureValue.toOption.get.pensionSharingOrder shouldBe false
        }

        "return pension age of 65" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.pensionAge shouldBe 65
        }

        "return full state pension rate of 155.65" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.currentFullWeeklyPensionAmount shouldBe 155.65
        }

        "when there is a protected payment of some value return true" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(
              regularStatement.copy(amounts = regularStatement.amounts.copy(protectedPayment2016 = 0))
            ))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          service.getStatement(generateNino()).futureValue.toOption.get.amounts.protectedPayment shouldBe false
        }

        "when there is a protected payment of 0 return false" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(
              regularStatement.copy(amounts = regularStatement.amounts.copy(protectedPayment2016 = 6.66))
            ))


          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

          service.getStatement(generateNino()).futureValue.toOption.get.amounts.protectedPayment shouldBe true
        }

        "when there is a rebate derived amount of 12.34 it" should {

          "return a weekly cope amount of 12.34" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(
                regularStatement.copy(amounts = regularStatement.amounts.copy(
                  amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 12.34)))
              ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.cope.weeklyAmount shouldBe 12.34
          }

          "return a monthly cope amount of 53.66" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(
                regularStatement.copy(amounts = regularStatement.amounts.copy(
                  amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 12.34)))
              ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.cope.monthlyAmount shouldBe 53.66
          }

          "return an annual cope amount of 643.88" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(
                regularStatement.copy(amounts = regularStatement.amounts.copy(
                  amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 12.34)))
              ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.cope.annualAmount shouldBe 643.88
          }

          "return a weekly starting amount of 161.18" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(
                regularStatement.copy(amounts = regularStatement.amounts.copy(
                  amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 12.34)))
              ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.starting.weeklyAmount shouldBe 161.18
          }

          "return a monthly starting amount of 700.85" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(
                regularStatement.copy(amounts = regularStatement.amounts.copy(
                  amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 12.34)))
              ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.starting.monthlyAmount shouldBe 700.85
          }

          "return an annual starting amount of 8410.14" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(
                regularStatement.copy(amounts = regularStatement.amounts.copy(
                  amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 12.34)))
              ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.starting.annualAmount shouldBe 8410.14
          }
        }

        "when there is a rebate derived amount of 0 it" should {

          "return a weekly cope amount of 0" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(
                regularStatement.copy(amounts = regularStatement.amounts.copy(
                  amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 0)))
              ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.cope.weeklyAmount shouldBe 0
          }

          "return a monthly cope amount of 0" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(
                regularStatement.copy(amounts = regularStatement.amounts.copy(
                  amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 0)))
              ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.cope.monthlyAmount shouldBe 0
          }

          "return an annual cope amount of 0" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(
                regularStatement.copy(amounts = regularStatement.amounts.copy(
                  amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 0)))
              ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.cope.annualAmount shouldBe 0
          }
        }

        "when there is all amounts of 0 it" should {

          "return a weekly starting amount of 0" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
              regularStatement.copy(amounts = regularStatement.amounts.copy(
                pensionEntitlement = 0,
                startingAmount2016 = 0,
                protectedPayment2016 = 0,
                AmountA2016(),
                AmountB2016()
              ))
            ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.starting.weeklyAmount shouldBe 0
          }

          "return a monthly starting amount of 0" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
              regularStatement.copy(amounts = regularStatement.amounts.copy(
                pensionEntitlement = 0,
                startingAmount2016 = 0,
                protectedPayment2016 = 0,
                AmountA2016(),
                AmountB2016()
              ))
            ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.starting.monthlyAmount shouldBe 0
          }

          "return an annual starting amount of 0" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
              regularStatement.copy(amounts = regularStatement.amounts.copy(
                pensionEntitlement = 0,
                startingAmount2016 = 0,
                protectedPayment2016 = 0,
                AmountA2016(),
                AmountB2016()
              ))
            ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.starting.annualAmount shouldBe 0
          }
        }

        "when there is an entitlement of 161.18 it" should {

          "return a weekly current amount of 161.18" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(regularStatement.copy(
                amounts = regularStatement.amounts.copy(pensionEntitlement = 161.18))
              ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.current.weeklyAmount shouldBe 161.18
          }

          "return a monthly current amount of 161.18" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(regularStatement.copy(
                amounts = regularStatement.amounts.copy(pensionEntitlement = 161.18))
              ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.current.monthlyAmount shouldBe 700.85
          }

          "return an annual current amount of 161.18" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(regularStatement.copy(
                amounts = regularStatement.amounts.copy(pensionEntitlement = 161.18))
              ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.current.annualAmount shouldBe 8410.14
          }
        }

        "the forecast" should {
          "return a weekly current amount of 161.18" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(regularStatement.copy(
                amounts = regularStatement.amounts.copy(pensionEntitlement = 161.18))
              ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.forecast.weeklyAmount shouldBe 161.18
          }

          "return a monthly current amount of 161.18" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(regularStatement.copy(
                amounts = regularStatement.amounts.copy(pensionEntitlement = 161.18))
              ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get
            statement.amounts.forecast.monthlyAmount shouldBe 700.85
          }

          "return an annual current amount of 161.18" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(regularStatement.copy(
                amounts = regularStatement.amounts.copy(pensionEntitlement = 161.18))
              ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.forecast.annualAmount shouldBe 8410.14
          }

          "return years to work 0 as they have reached" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(regularStatement.copy(
                amounts = regularStatement.amounts.copy(pensionEntitlement = 161.18))
              ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.forecast.yearsToWork.get shouldBe 0
          }
        }

        "when there is an entitlement of 0 it" should {

          "return a weekly current amount of 0" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
              regularStatement.copy(amounts = PensionAmounts())
            ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.current.weeklyAmount shouldBe 0
          }

          "return a monthly current amount of 0" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
              regularStatement.copy(amounts = PensionAmounts())
            ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.current.monthlyAmount shouldBe 0
          }

          "return an annual current amount of 0" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
              regularStatement.copy(amounts = PensionAmounts())
            ))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.toOption.get

            statement.amounts.current.annualAmount shouldBe 0
          }
        }
      }

    }

    "there are exclusions" when {
      "there is a regular statement (Reached)" should {

        val regularStatement = Summary(
          earningsIncludedUpTo = LocalDate.of(2016, 4, 5),
          statePensionAgeDate = LocalDate.of(2019, 9, 6),
          finalRelevantStartYear = 2018,
          pensionSharingOrderSERPS = false,
          dateOfBirth = LocalDate.of(1954, 3, 9),
          amounts = PensionAmounts(
            pensionEntitlement = 161.18,
            startingAmount2016 = 0,
            protectedPayment2016 = 5.53,
            AmountA2016(
              basicStatePension = 119.3,
              pre97AP = 17.79,
              post97AP = 6.03,
              post02AP = 15.4,
              graduatedRetirementBenefit = 2.66
            ),
            AmountB2016(
              mainComponent = 155.65,
            )
          ),
          manualCorrespondenceIndicator = None
        )

        "when there is a starting amount of 0 it" should {

          "return an AmountDissonance exclusion" in {
            when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(regularStatement))

            when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).futureValue.swap

            statement.toOption.get.exclusionReasons shouldBe List(Exclusion.AmountDissonance)
            statement.toOption.get.pensionAge shouldBe 65
            statement.toOption.get.pensionDate.toString shouldBe "2019-09-06"
            statement.toOption.get.statePensionAgeUnderConsideration.toString shouldBe "false"
          }
        }
      }
    }

    "there is a regular statement (Forecast)" should {

      val regularStatement = Summary(
        earningsIncludedUpTo = LocalDate.of(2016, 4, 5),
        statePensionAgeDate = LocalDate.of(2019, 9, 6),
        finalRelevantStartYear = 2018,
        pensionSharingOrderSERPS = false,
        dateOfBirth = LocalDate.of(1954, 3, 9),
        amounts = PensionAmounts(
          pensionEntitlement = 121.41,
          startingAmount2016 = 121.41,
          protectedPayment2016 = 5.53,
          AmountA2016(
            basicStatePension = 79.53,
            pre97AP = 17.79,
            post97AP = 6.03,
            post02AP = 15.4,
            graduatedRetirementBenefit = 2.66
          ),
          AmountB2016(
            mainComponent = 88.94,
          )
        ),
        manualCorrespondenceIndicator = None
      )

      "the OldRules amounts" should {
        "return additionalPension amount of 39.22" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
            regularStatement
          ))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
            NIRecord(qualifyingYears = 20, List())
          ))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.oldRules.additionalStatePension shouldBe 39.22
        }

        "return graduatedRetirementBenefit amount of 2.66" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
            regularStatement
          ))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
            NIRecord(qualifyingYears = 20, List())
          ))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.oldRules.graduatedRetirementBenefit shouldBe 2.66
        }

        "return basicStatePension amount of 2.66" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
            regularStatement
          ))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
            NIRecord(qualifyingYears = 20, List())
          ))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.oldRules.basicStatePension shouldBe 79.53
        }
      }

      "the NewRules amounts" should {
        "return grossStatePension amount of 88.94" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
            regularStatement
          ))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
            NIRecord(qualifyingYears = 20, List())
          ))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.newRules.grossStatePension shouldBe 88.94
        }

        "return rebateDerivedAmount amount of 0.00" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
            regularStatement
          ))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
            NIRecord(qualifyingYears = 20, List())
          ))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.newRules.rebateDerivedAmount shouldBe 0.00
        }
      }

      "the forecast amount" should {

        "return a weekly forecast amount of 134.75" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
            regularStatement
          ))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
            NIRecord(qualifyingYears = 20, List())
          ))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.forecast.weeklyAmount shouldBe 134.75
        }

        "return a monthly forecast amount of 585.92" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
            regularStatement
          ))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
            NIRecord(qualifyingYears = 20, List())
          ))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.forecast.monthlyAmount shouldBe 585.92
        }

        "return an annual forecast amount of 7031.06" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
            regularStatement
          ))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
            NIRecord(qualifyingYears = 20, List())
          ))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.forecast.annualAmount shouldBe 7031.06
        }

        "return years to work of 3 and that is how long is left" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
            regularStatement
          ))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(
            NIRecord(qualifyingYears = 20, List())
          ))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.forecast.yearsToWork.get shouldBe 3
        }

      }

      "log a summary metric" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(regularStatement))
        when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(NIRecord(qualifyingYears = 20, List())))

        service.getStatement(generateNino()).futureValue.toOption.get

        verify(mockMetrics, times(1)).summary(
          ArgumentMatchers.eq[BigDecimal](134.75),
          ArgumentMatchers.eq[BigDecimal](121.41),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(Scenario.ContinueWorkingNonMax),
          ArgumentMatchers.eq[BigDecimal](134.75),
          ArgumentMatchers.eq(3),
          ArgumentMatchers.eq(None),
          ArgumentMatchers.eq[BigDecimal](121.41),
          ArgumentMatchers.eq[BigDecimal](79.53),
          ArgumentMatchers.eq[BigDecimal](39.22),
          ArgumentMatchers.eq[BigDecimal](2.66),
          ArgumentMatchers.eq[BigDecimal](88.94),
          ArgumentMatchers.eq[BigDecimal](0),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(None),
          ArgumentMatchers.eq(false)
        )
      }

      "not log an exclusion metric" in {
        verify(mockMetrics, never).exclusion(ArgumentMatchers.any())
      }
    }

    "there is a regular statement (grossStatePension)" should {

      val regularStatement = Summary(
        earningsIncludedUpTo = LocalDate.of(2016, 4, 5),
        statePensionAgeDate = LocalDate.of(2019, 9, 6),
        finalRelevantStartYear = 2018,
        pensionSharingOrderSERPS = false,
        dateOfBirth = LocalDate.of(1954, 3, 9),
        amounts = PensionAmounts(
          pensionEntitlement = 121.41,
          startingAmount2016 = 121.41,
          protectedPayment2016 = 5.53,
          AmountA2016(
            basicStatePension = 79.53,
            pre97AP = 17.79,
            post97AP = 6.03,
            post02AP = 15.4,
            graduatedRetirementBenefit = 2.66
          ),
          AmountB2016(
            mainComponent = 66.37, // Net SP
            rebateDerivedAmount = 18.13
          )
        ),
        manualCorrespondenceIndicator = None
      )


      "the NewRules amounts" should {
        "return grossStatePension amount of 84.50" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 20, List())))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.newRules.grossStatePension shouldBe 84.50
        }

        "return rebateDerivedAmount amount of 18.13" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(NIRecord(qualifyingYears = 20, List())))

          val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.newRules.rebateDerivedAmount shouldBe 18.13
        }
      }

    }

    "there is a regular statement (Fill Gaps)" should {

      val regularStatement = Summary(
        earningsIncludedUpTo = LocalDate.of(2016, 4, 5),
        statePensionAgeDate = LocalDate.of(2019, 9, 6),
        finalRelevantStartYear = 2018,
        pensionSharingOrderSERPS = false,
        dateOfBirth = LocalDate.of(1954, 3, 9),
        amounts = PensionAmounts(
          pensionEntitlement = 121.4123,
          startingAmount2016 = 121.41,
          protectedPayment2016 = 5.53,
          AmountA2016(
            basicStatePension = 79.53,
            pre97AP = 17.79,
            post97AP = 6.03,
            post02AP = 15.4,
            graduatedRetirementBenefit = 2.66
          ),
          AmountB2016(
            mainComponent = 88.94
          )
        ),
        manualCorrespondenceIndicator = None
      )

      "the personal maximum amount" should {

        "return a weekly personal max amount of 142.71" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          lazy val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.maximum.weeklyAmount shouldBe 142.71
        }

        "return a monthly personal max amount of 620.53" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          lazy val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.maximum.monthlyAmount shouldBe 620.53
        }

        "return an annual personal max amount of 7446.40" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          lazy val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.maximum.annualAmount shouldBe 7446.40
        }

        "return 2 gaps to fill" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          lazy val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.maximum.gapsToFill shouldBe Some(2)
        }

        "return 3 years to work" in {
          when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(regularStatement))

          lazy val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

          statement.amounts.maximum.yearsToWork shouldBe Some(3)
        }
      }

      "summary have totalAP as 41.88" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(regularStatement))

        lazy val summaryF: Future[Summary] = mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())

        whenReady(summaryF) { summary =>
          summary.amounts.amountA2016.totalAP shouldBe 41.88
        }
      }

      "summary have additionalStatePension as 39.22" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(regularStatement))

        lazy val summaryF: Future[Summary] = mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())

        whenReady(summaryF) { summary =>
          summary.amounts.amountA2016.additionalStatePension shouldBe 39.22
        }
      }

      "summary have graduatedRetirementBenefit as 2.66" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(regularStatement))

        lazy val summaryF: Future[Summary] = mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())

        whenReady(summaryF) { summary =>
          summary.amounts.amountA2016.graduatedRetirementBenefit shouldBe 2.66
        }
      }

      "summary have basicStatePension as 79.53" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(regularStatement))

        lazy val summaryF: Future[Summary] = mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())

        whenReady(summaryF) { summary =>
          summary.amounts.amountA2016.basicStatePension shouldBe 79.53
        }
      }

      "statePension have additionalStatePension as 39.22" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(regularStatement))

        lazy val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

        statement.amounts.oldRules.additionalStatePension shouldBe 39.22
      }

      "statePension have graduatedRetirementBenefit as 2.66" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(regularStatement))

        lazy val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

        statement.amounts.oldRules.graduatedRetirementBenefit shouldBe 2.66
      }

      "statePension have basicStatePension as 79.53" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(regularStatement))

        lazy val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

        statement.amounts.oldRules.basicStatePension shouldBe 79.53
      }

      "statePension have grossStatePension as 88.94" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(regularStatement))

        lazy val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

        statement.amounts.newRules.grossStatePension shouldBe 88.94
      }

      "statePension have rebateDerivedAmount as 0.00" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(regularStatement))

        lazy val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get

        statement.amounts.newRules.rebateDerivedAmount shouldBe 0.00
      }

      "log a summary metric" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(regularStatement))

        service.getStatement(generateNino()).futureValue.toOption.get

        verify(mockMetrics, times(1)).summary(
          ArgumentMatchers.eq[BigDecimal](134.75),
          ArgumentMatchers.eq[BigDecimal](121.41),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(Scenario.FillGaps),
          ArgumentMatchers.eq[BigDecimal](142.71),
          ArgumentMatchers.eq(3),
          ArgumentMatchers.eq(None),
          ArgumentMatchers.eq[BigDecimal](121.41),
          ArgumentMatchers.eq[BigDecimal](79.53),
          ArgumentMatchers.eq[BigDecimal](39.22),
          ArgumentMatchers.eq[BigDecimal](2.66),
          ArgumentMatchers.eq[BigDecimal](88.94),
          ArgumentMatchers.eq[BigDecimal](0),
          ArgumentMatchers.eq(false),
          ArgumentMatchers.eq(None),
          ArgumentMatchers.eq(false)
        )
      }

      "not log an exclusion metric" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(regularStatement))

        verify(mockMetrics, never).exclusion(ArgumentMatchers.any())
      }
    }

    "there is an mqp user" should {

      val regularStatement = Summary(
        earningsIncludedUpTo = LocalDate.of(2016, 4, 5),
        statePensionAgeDate = LocalDate.of(2019, 9, 6),
        finalRelevantStartYear = 2018,
        pensionSharingOrderSERPS = false,
        dateOfBirth = LocalDate.of(1954, 3, 9),
        amounts = PensionAmounts(
          pensionEntitlement = 40.53,
          startingAmount2016 = 40.53,
          protectedPayment2016 = 0,
          AmountA2016(
            basicStatePension = 35.79,
            post02AP = 4.74
          ),
          AmountB2016(
            mainComponent = 40.02
          )
        ),
        manualCorrespondenceIndicator = None
      )

      "return 0 for the current amount" in {
        when(mockNpsConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(regularStatement))
        when(mockNpsConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(NIRecord(qualifyingYears = 9, List())))

        lazy val statement: StatePension = service.getStatement(generateNino()).futureValue.toOption.get
        statement.amounts.current.weeklyAmount shouldBe 0
      }
    }
  }
}