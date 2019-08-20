/*
 * Copyright 2019 HM Revenue & Customs
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

import org.joda.time.LocalDate
import org.mockito.Mockito.{never, times, verify, when}
import org.mockito.{Matchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.builders.RateServiceBuilder
import uk.gov.hmrc.statepension.connectors.{DesConnector, StatePensionAuditConnector}
import uk.gov.hmrc.statepension.domain.nps._
import uk.gov.hmrc.statepension.domain.{Exclusion, Scenario, StatePension}

import scala.concurrent.Future

class StatePensionServiceStatementSpec extends StatePensionUnitSpec
  with OneAppPerSuite
  with ScalaFutures
  with MockitoSugar
  with BeforeAndAfterEach {

  val mockDesConnector: DesConnector = mock[DesConnector]
  val mockStatePensionAuditConnector = mock[StatePensionAuditConnector]
  val mockMetrics: ApplicationMetrics = mock[ApplicationMetrics]
  val mockCitizenDetails: CitizenDetailsService = mock[CitizenDetailsService]
  val defaultForecasting = new ForecastingService(rateService = RateServiceBuilder.default)

  lazy val service: StatePensionService = new StatePensionService(mockDesConnector,
    mockCitizenDetails,
    defaultForecasting,
    RateServiceBuilder.default,
    mockMetrics,
    mockStatePensionAuditConnector) {
    override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
  }

  override def beforeEach: Unit = {
    Mockito.reset(mockDesConnector, mockMetrics, mockCitizenDetails, mockStatePensionAuditConnector)

    when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(false))

    when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(List()))

    when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(
        DesNIRecord(qualifyingYears = 20, List(DesNITaxYear(Some(2000), Some(false), Some(false), Some(true)),
          DesNITaxYear(Some(2001), Some(false), Some(false), Some(true))))
      ))

  }

  "StatePensionService with a HOD Connection" when {

    "there are no exclusions" when {
      "there is a regular statement (Reached)" should {

        val regularStatement = DesSummary(
          earningsIncludedUpTo = new LocalDate(2016, 4, 5),
          sex = "F",
          statePensionAgeDate = new LocalDate(2019, 9, 6),
          finalRelevantStartYear = 2018,
          pensionSharingOrderSERPS = false,
          dateOfBirth = new LocalDate(1954, 3, 9),
          amounts = DesStatePensionAmounts(
            pensionEntitlement = 161.18,
            startingAmount2016 = 161.18,
            protectedPayment2016 = 5.53,
            DesAmountA2016(
              basicStatePension = 119.3,
              pre97AP = 17.79,
              post97AP = 6.03,
              post02AP = 15.4,
              pre88GMP = 0,
              post88GMP = 0,
              pre88COD = 0,
              post88COD = 0,
              graduatedRetirementBenefit = 2.66
            ),
            DesAmountB2016(
              mainComponent = 155.65,
              rebateDerivedAmount = 0
            )
          )
        )

        "log a summary metric" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          verify(mockMetrics, Mockito.atLeastOnce()).summary(
            Matchers.eq[BigDecimal](161.18),
            Matchers.eq[BigDecimal](161.18),
            Matchers.eq(false),
            Matchers.eq(Scenario.Reached),
            Matchers.eq[BigDecimal](161.18),
            Matchers.eq(0),
            Matchers.eq(None),
            Matchers.eq[BigDecimal](161.18),
            Matchers.eq[BigDecimal](119.3),
            Matchers.eq[BigDecimal](39.22),
            Matchers.eq[BigDecimal](2.66),
            Matchers.eq[BigDecimal](155.65),
            Matchers.eq[BigDecimal](0),
            Matchers.eq(false),
            Matchers.eq(None),
            Matchers.eq(false)
          )
        }

        "not log an exclusion metric" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          verify(mockMetrics, never).exclusion(Matchers.any())
        }

        "return earningsIncludedUpTo of 2016-4-5" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          whenReady(statement) { sp =>
            sp.earningsIncludedUpTo shouldBe new LocalDate(2016, 4, 5)
          }
        }

        "return qualifying years of 36" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          whenReady(statement) { sp =>
            sp.numberOfQualifyingYears shouldBe 36
          }
        }

        "return pension date of 2019-9-6" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          whenReady(statement) { sp =>
            sp.pensionDate shouldBe new LocalDate(2019, 9, 6)
          }
        }

        "return oldRules additionalStatePension as 39.22" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          whenReady(statement) { sp =>
            sp.amounts.oldRules.additionalStatePension shouldBe 39.22
          }
        }

        "return oldRules graduatedRetirementsBenefit as 2.66" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          whenReady(statement) { sp =>
            sp.amounts.oldRules.graduatedRetirementBenefit shouldBe 2.66
          }
        }

        "return oldRules basicStatePension as 119.3" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          whenReady(statement) { sp =>
            sp.amounts.oldRules.basicStatePension shouldBe 119.3
          }
        }

        "return newRules grossStatePension as 155.65" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          whenReady(statement) { sp =>
            sp.amounts.newRules.grossStatePension shouldBe 155.65
          }
        }

        "return newRules rebateDerivedAmount as 0.00" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          whenReady(statement) { sp =>
            sp.amounts.newRules.rebateDerivedAmount shouldBe 0.00
          }
        }

        "return RRECurrentWeeklyAmount as None" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          whenReady(statement) { sp =>
            sp.reducedRateElectionCurrentWeeklyAmount shouldBe None
          }
        }

        "return reducedRateElection as false" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          whenReady(statement) { sp =>
            sp.reducedRateElection shouldBe false
          }
        }

        "return statePensionAgeUnderConsideration as false" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          whenReady(statement) { sp =>
            sp.statePensionAgeUnderConsideration shouldBe false
          }
        }

        "return final relevant year" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          whenReady(statement) { sp =>
            sp.finalRelevantYear shouldBe "2018-19"
          }
        }

        "when there is a pensionSharingOrder return true" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement.copy(pensionSharingOrderSERPS = true)))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          service.getStatement(generateNino()).right.get.pensionSharingOrder shouldBe true
        }

        "when there is no pensionSharingOrder return false" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement.copy(pensionSharingOrderSERPS = false)))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          service.getStatement(generateNino()).right.get.pensionSharingOrder shouldBe false
        }

        "return pension age of 65" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          whenReady(statement) { sp =>
            sp.pensionAge shouldBe 65
          }
        }

        "return full state pension rate of 155.65" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          whenReady(statement) { sp =>
            sp.currentFullWeeklyPensionAmount shouldBe 155.65
          }
        }

        "when there is a protected payment of some value return true" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(
              regularStatement.copy(amounts = regularStatement.amounts.copy(protectedPayment2016 = 0))
            ))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          service.getStatement(generateNino()).right.get.amounts.protectedPayment shouldBe false
        }

        "when there is a protected payment of 0 return false" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(
              regularStatement.copy(amounts = regularStatement.amounts.copy(protectedPayment2016 = 6.66))
            ))


          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

          service.getStatement(generateNino()).right.get.amounts.protectedPayment shouldBe true
        }

        "when there is a rebate derived amount of 12.34 it" should {

          "return a weekly cope amount of 12.34" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(
                regularStatement.copy(amounts = regularStatement.amounts.copy(
                  amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 12.34)))
              ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.cope.weeklyAmount shouldBe 12.34
          }

          "return a monthly cope amount of 53.66" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(
                regularStatement.copy(amounts = regularStatement.amounts.copy(
                  amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 12.34)))
              ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.cope.monthlyAmount shouldBe 53.66
          }

          "return an annual cope amount of 643.88" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(
                regularStatement.copy(amounts = regularStatement.amounts.copy(
                  amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 12.34)))
              ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.cope.annualAmount shouldBe 643.88
          }

          "return a weekly starting amount of 161.18" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(
                regularStatement.copy(amounts = regularStatement.amounts.copy(
                  amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 12.34)))
              ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.starting.weeklyAmount shouldBe 161.18
          }

          "return a monthly starting amount of 700.85" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(
                regularStatement.copy(amounts = regularStatement.amounts.copy(
                  amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 12.34)))
              ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.starting.monthlyAmount shouldBe 700.85
          }

          "return an annual starting amount of 8410.14" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(
                regularStatement.copy(amounts = regularStatement.amounts.copy(
                  amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 12.34)))
              ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.starting.annualAmount shouldBe 8410.14
          }
        }

        "when there is a rebate derived amount of 0 it" should {

          "return a weekly cope amount of 0" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(
                regularStatement.copy(amounts = regularStatement.amounts.copy(
                  amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 0)))
              ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.cope.weeklyAmount shouldBe 0
          }

          "return a monthly cope amount of 0" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(
                regularStatement.copy(amounts = regularStatement.amounts.copy(
                  amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 0)))
              ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.cope.monthlyAmount shouldBe 0
          }

          "return an annual cope amount of 0" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(
                regularStatement.copy(amounts = regularStatement.amounts.copy(
                  amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 0)))
              ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.cope.annualAmount shouldBe 0
          }
        }

        "when there is all amounts of 0 it" should {

          "return a weekly starting amount of 0" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
              regularStatement.copy(amounts = regularStatement.amounts.copy(
                pensionEntitlement = 0,
                startingAmount2016 = 0,
                protectedPayment2016 = 0,
                DesAmountA2016(
                  basicStatePension = 0,
                  pre97AP = 0,
                  post97AP = 0,
                  post02AP = 0,
                  pre88GMP = 0,
                  post88GMP = 0,
                  pre88COD = 0,
                  post88COD = 0,
                  graduatedRetirementBenefit = 0
                ),
                DesAmountB2016(
                  mainComponent = 0,
                  rebateDerivedAmount = 0
                )
              ))
            ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.starting.weeklyAmount shouldBe 0
          }

          "return a monthly starting amount of 0" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
              regularStatement.copy(amounts = regularStatement.amounts.copy(
                pensionEntitlement = 0,
                startingAmount2016 = 0,
                protectedPayment2016 = 0,
                DesAmountA2016(
                  basicStatePension = 0,
                  pre97AP = 0,
                  post97AP = 0,
                  post02AP = 0,
                  pre88GMP = 0,
                  post88GMP = 0,
                  pre88COD = 0,
                  post88COD = 0,
                  graduatedRetirementBenefit = 0
                ),
                DesAmountB2016(
                  mainComponent = 0,
                  rebateDerivedAmount = 0
                )
              ))
            ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.starting.monthlyAmount shouldBe 0
          }

          "return an annual starting amount of 0" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
              regularStatement.copy(amounts = regularStatement.amounts.copy(
                pensionEntitlement = 0,
                startingAmount2016 = 0,
                protectedPayment2016 = 0,
                DesAmountA2016(
                  basicStatePension = 0,
                  pre97AP = 0,
                  post97AP = 0,
                  post02AP = 0,
                  pre88GMP = 0,
                  post88GMP = 0,
                  pre88COD = 0,
                  post88COD = 0,
                  graduatedRetirementBenefit = 0
                ),
                DesAmountB2016(
                  mainComponent = 0,
                  rebateDerivedAmount = 0
                )
              ))
            ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.starting.annualAmount shouldBe 0
          }
        }

        "when there is an entitlement of 161.18 it" should {

          "return a weekly current amount of 161.18" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(regularStatement.copy(
                amounts = regularStatement.amounts.copy(pensionEntitlement = 161.18))
              ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.current.weeklyAmount shouldBe 161.18
          }

          "return a monthly current amount of 161.18" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(regularStatement.copy(
                amounts = regularStatement.amounts.copy(pensionEntitlement = 161.18))
              ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.current.monthlyAmount shouldBe 700.85
          }

          "return an annual current amount of 161.18" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(regularStatement.copy(
                amounts = regularStatement.amounts.copy(pensionEntitlement = 161.18))
              ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            service.getStatement(generateNino()).right.get.amounts.current.annualAmount shouldBe 8410.14
          }
        }

        "the forecast" should {
          "return a weekly current amount of 161.18" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(regularStatement.copy(
                amounts = regularStatement.amounts.copy(pensionEntitlement = 161.18))
              ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.forecast.weeklyAmount shouldBe 161.18
          }

          "return a monthly current amount of 161.18" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(regularStatement.copy(
                amounts = regularStatement.amounts.copy(pensionEntitlement = 161.18))
              ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get
            statement.amounts.forecast.monthlyAmount shouldBe 700.85
          }

          "return an annual current amount of 161.18" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(regularStatement.copy(
                amounts = regularStatement.amounts.copy(pensionEntitlement = 161.18))
              ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.forecast.annualAmount shouldBe 8410.14
          }

          "return years to work 0 as they have reached" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(regularStatement.copy(
                amounts = regularStatement.amounts.copy(pensionEntitlement = 161.18))
              ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.forecast.yearsToWork.get shouldBe 0
          }
        }

        "when there is an entitlement of 0 it" should {

          "return a weekly current amount of 0" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
              regularStatement.copy(amounts = DesStatePensionAmounts())
            ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.current.weeklyAmount shouldBe 0
          }

          "return a monthly current amount of 0" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
              regularStatement.copy(amounts = DesStatePensionAmounts())
            ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.current.monthlyAmount shouldBe 0
          }

          "return an annual current amount of 0" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
              regularStatement.copy(amounts = DesStatePensionAmounts())
            ))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).right.get

            statement.amounts.current.annualAmount shouldBe 0
          }
        }
      }

    }

    "there are exclusions" when {
      "there is a regular statement (Reached)" should {

        val regularStatement = DesSummary(
          earningsIncludedUpTo = new LocalDate(2016, 4, 5),
          sex = "F",
          statePensionAgeDate = new LocalDate(2019, 9, 6),
          finalRelevantStartYear = 2018,
          pensionSharingOrderSERPS = false,
          dateOfBirth = new LocalDate(1954, 3, 9),
          amounts = DesStatePensionAmounts(
            pensionEntitlement = 161.18,
            startingAmount2016 = 0,
            protectedPayment2016 = 5.53,
            DesAmountA2016(
              basicStatePension = 119.3,
              pre97AP = 17.79,
              post97AP = 6.03,
              post02AP = 15.4,
              pre88GMP = 0,
              post88GMP = 0,
              pre88COD = 0,
              post88COD = 0,
              graduatedRetirementBenefit = 2.66
            ),
            DesAmountB2016(
              mainComponent = 155.65,
              rebateDerivedAmount = 0
            )
          )
        )

        "when there is a starting amount of 0 it" should {

          "return an AmountDissonance exclusion" in {
            when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(regularStatement))

            when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
              .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))

            val statement = service.getStatement(generateNino()).left

            statement.get.exclusionReasons shouldBe List(Exclusion.AmountDissonance)
            statement.get.pensionAge shouldBe 65
            statement.get.pensionDate.toString shouldBe "2019-09-06"
            statement.get.statePensionAgeUnderConsideration.toString shouldBe "false"
          }
        }
      }
    }

    "there is a regular statement (Forecast)" should {

      val regularStatement = DesSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "F",
        statePensionAgeDate = new LocalDate(2019, 9, 6),
        finalRelevantStartYear = 2018,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1954, 3, 9),
        amounts = DesStatePensionAmounts(
          pensionEntitlement = 121.41,
          startingAmount2016 = 121.41,
          protectedPayment2016 = 5.53,
          DesAmountA2016(
            basicStatePension = 79.53,
            pre97AP = 17.79,
            post97AP = 6.03,
            post02AP = 15.4,
            pre88GMP = 0,
            post88GMP = 0,
            pre88COD = 0,
            post88COD = 0,
            graduatedRetirementBenefit = 2.66
          ),
          DesAmountB2016(
            mainComponent = 88.94,
            rebateDerivedAmount = 0
          )
        )
      )

      "the OldRules amounts" should {
        "return additionalPension amount of 39.22" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            regularStatement
          ))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            DesNIRecord(qualifyingYears = 20, List())
          ))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          statement.amounts.oldRules.additionalStatePension shouldBe 39.22
        }

        "return graduatedRetirementBenefit amount of 2.66" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            regularStatement
          ))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            DesNIRecord(qualifyingYears = 20, List())
          ))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          statement.amounts.oldRules.graduatedRetirementBenefit shouldBe 2.66
        }

        "return basicStatePension amount of 2.66" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            regularStatement
          ))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            DesNIRecord(qualifyingYears = 20, List())
          ))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          statement.amounts.oldRules.basicStatePension shouldBe 79.53
        }
      }

      "the NewRules amounts" should {
        "return grossStatePension amount of 88.94" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            regularStatement
          ))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            DesNIRecord(qualifyingYears = 20, List())
          ))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          statement.amounts.newRules.grossStatePension shouldBe 88.94
        }

        "return rebateDerivedAmount amount of 0.00" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            regularStatement
          ))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            DesNIRecord(qualifyingYears = 20, List())
          ))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          statement.amounts.newRules.rebateDerivedAmount shouldBe 0.00
        }
      }

      "the forecast amount" should {

        "return a weekly forecast amount of 134.75" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            regularStatement
          ))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            DesNIRecord(qualifyingYears = 20, List())
          ))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          statement.amounts.forecast.weeklyAmount shouldBe 134.75
        }

        "return a monthly forecast amount of 585.92" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            regularStatement
          ))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            DesNIRecord(qualifyingYears = 20, List())
          ))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          statement.amounts.forecast.monthlyAmount shouldBe 585.92
        }

        "return an annual forecast amount of 7031.06" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            regularStatement
          ))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            DesNIRecord(qualifyingYears = 20, List())
          ))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          statement.amounts.forecast.annualAmount shouldBe 7031.06
        }

        "return years to work of 3 and that is how long is left" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            regularStatement
          ))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            DesNIRecord(qualifyingYears = 20, List())
          ))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          statement.amounts.forecast.yearsToWork.get shouldBe 3
        }

      }

      "log a summary metric" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          regularStatement
        ))

        when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          DesNIRecord(qualifyingYears = 20, List())
        ))

        val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

        whenReady(statement) { summary =>
          verify(mockMetrics, times(1)).summary(
            Matchers.eq[BigDecimal](134.75),
            Matchers.eq[BigDecimal](121.41),
            Matchers.eq(false),
            Matchers.eq(Scenario.ContinueWorkingNonMax),
            Matchers.eq[BigDecimal](134.75),
            Matchers.eq(3),
            Matchers.eq(None),
            Matchers.eq[BigDecimal](121.41),
            Matchers.eq[BigDecimal](79.53),
            Matchers.eq[BigDecimal](39.22),
            Matchers.eq[BigDecimal](2.66),
            Matchers.eq[BigDecimal](88.94),
            Matchers.eq[BigDecimal](0),
            Matchers.eq(false),
            Matchers.eq(None),
            Matchers.eq(false)
          )
        }
      }

      "not log an exclusion metric" in {
        verify(mockMetrics, never).exclusion(Matchers.any())
      }
    }

    "there is a regular statement (grossStatePension)" should {

      val regularStatement = DesSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "F",
        statePensionAgeDate = new LocalDate(2019, 9, 6),
        finalRelevantStartYear = 2018,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1954, 3, 9),
        amounts = DesStatePensionAmounts(
          pensionEntitlement = 121.41,
          startingAmount2016 = 121.41,
          protectedPayment2016 = 5.53,
          DesAmountA2016(
            basicStatePension = 79.53,
            pre97AP = 17.79,
            post97AP = 6.03,
            post02AP = 15.4,
            pre88GMP = 0,
            post88GMP = 0,
            pre88COD = 0,
            post88COD = 0,
            graduatedRetirementBenefit = 2.66
          ),
          DesAmountB2016(
            mainComponent = 66.37, // Net SP
            rebateDerivedAmount = 18.13
          )
        )
      )


      "the NewRules amounts" should {
        "return grossStatePension amount of 84.50" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 20, List())))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          statement.amounts.newRules.grossStatePension shouldBe 84.50
        }

        "return rebateDerivedAmount amount of 18.13" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 20, List())))

          val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          statement.amounts.newRules.rebateDerivedAmount shouldBe 18.13
        }
      }

    }

    "there is a regular statement (Fill Gaps)" should {

      val regularStatement = DesSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "F",
        statePensionAgeDate = new LocalDate(2019, 9, 6),
        finalRelevantStartYear = 2018,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1954, 3, 9),
        amounts = DesStatePensionAmounts(
          pensionEntitlement = 121.4123,
          startingAmount2016 = 121.41,
          protectedPayment2016 = 5.53,
          DesAmountA2016(
            basicStatePension = 79.53,
            pre97AP = 17.79,
            post97AP = 6.03,
            post02AP = 15.4,
            pre88GMP = 0,
            post88GMP = 0,
            pre88COD = 0,
            post88COD = 0,
            graduatedRetirementBenefit = 2.66
          ),
          DesAmountB2016(
            mainComponent = 88.94,
            rebateDerivedAmount = 0
          )
        )
      )

      "the personal maximum amount" should {

        "return a weekly personal max amount of 142.71" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          lazy val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          statement.amounts.maximum.weeklyAmount shouldBe 142.71
        }

        "return a monthly personal max amount of 620.53" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          lazy val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          statement.amounts.maximum.monthlyAmount shouldBe 620.53
        }

        "return an annual personal max amount of 7446.40" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          lazy val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          statement.amounts.maximum.annualAmount shouldBe 7446.40
        }

        "return 2 gaps to fill" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          lazy val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          statement.amounts.maximum.gapsToFill shouldBe Some(2)
        }

        "return 3 years to work" in {
          when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
            .thenReturn(Future.successful(regularStatement))

          lazy val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

          statement.amounts.maximum.yearsToWork shouldBe Some(3)
        }
      }

      "summary have totalAP as 41.88" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        lazy val summaryF: Future[DesSummary] = mockDesConnector.getSummary(Matchers.any())(Matchers.any())

        whenReady(summaryF) { summary =>
          summary.amounts.amountA2016.totalAP shouldBe 41.88
        }
      }

      "summary have additionalStatePension as 39.22" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        lazy val summaryF: Future[DesSummary] = mockDesConnector.getSummary(Matchers.any())(Matchers.any())

        whenReady(summaryF) { summary =>
          summary.amounts.amountA2016.additionalStatePension shouldBe 39.22
        }
      }

      "summary have graduatedRetirementBenefit as 2.66" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        lazy val summaryF: Future[DesSummary] = mockDesConnector.getSummary(Matchers.any())(Matchers.any())

        whenReady(summaryF) { summary =>
          summary.amounts.amountA2016.graduatedRetirementBenefit shouldBe 2.66
        }
      }

      "summary have basicStatePension as 79.53" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        lazy val summaryF: Future[DesSummary] = mockDesConnector.getSummary(Matchers.any())(Matchers.any())

        whenReady(summaryF) { summary =>
          summary.amounts.amountA2016.basicStatePension shouldBe 79.53
        }
      }

      "statePension have additionalStatePension as 39.22" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        lazy val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

        whenReady(statement) { statePension =>
          statePension.amounts.oldRules.additionalStatePension shouldBe 39.22

        }
      }

      "statePension have graduatedRetirementBenefit as 2.66" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        lazy val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

        whenReady(statement) { statePension =>
          statePension.amounts.oldRules.graduatedRetirementBenefit shouldBe 2.66
        }
      }

      "statePension have basicStatePension as 79.53" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        lazy val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

        whenReady(statement) { statePension =>
          statePension.amounts.oldRules.basicStatePension shouldBe 79.53
        }
      }

      "statePension have grossStatePension as 88.94" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        lazy val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

        whenReady(statement) { statePension =>
          statePension.amounts.newRules.grossStatePension shouldBe 88.94
        }
      }

      "statePension have rebateDerivedAmount as 0.00" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        lazy val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

        whenReady(statement) { statePension =>
          statePension.amounts.newRules.rebateDerivedAmount shouldBe 0.00
        }
      }

      "log a summary metric" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        lazy val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

        whenReady(statement) { statePension =>
          verify(mockMetrics, times(1)).summary(
            Matchers.eq[BigDecimal](134.75),
            Matchers.eq[BigDecimal](121.41),
            Matchers.eq(false),
            Matchers.eq(Scenario.FillGaps),
            Matchers.eq[BigDecimal](142.71),
            Matchers.eq(3),
            Matchers.eq(None),
            Matchers.eq[BigDecimal](121.41),
            Matchers.eq[BigDecimal](79.53),
            Matchers.eq[BigDecimal](39.22),
            Matchers.eq[BigDecimal](2.66),
            Matchers.eq[BigDecimal](88.94),
            Matchers.eq[BigDecimal](0),
            Matchers.eq(false),
            Matchers.eq(None),
            Matchers.eq(false)
          )
        }
      }

      "not log an exclusion metric" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        verify(mockMetrics, never).exclusion(Matchers.any())
      }
    }

    "there is an mqp user" should {

      val regularStatement = DesSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "F",
        statePensionAgeDate = new LocalDate(2019, 9, 6),
        finalRelevantStartYear = 2018,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1954, 3, 9),
        amounts = DesStatePensionAmounts(
          pensionEntitlement = 40.53,
          startingAmount2016 = 40.53,
          protectedPayment2016 = 0,
          DesAmountA2016(
            basicStatePension = 35.79,
            pre97AP = 0,
            post97AP = 0,
            post02AP = 4.74,
            pre88GMP = 0,
            post88GMP = 0,
            pre88COD = 0,
            post88COD = 0,
            graduatedRetirementBenefit = 0
          ),
          DesAmountB2016(
            mainComponent = 40.02,
            rebateDerivedAmount = 0
          )
        )
      )

      "return 0 for the current amount" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 9, List())))

        lazy val statement: Future[StatePension] = service.getStatement(generateNino()).right.get
        whenReady(statement) { statement =>
          statement.amounts.current.weeklyAmount shouldBe 0
        }
      }
    }
  }
}