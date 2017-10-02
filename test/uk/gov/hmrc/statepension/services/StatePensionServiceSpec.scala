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

import org.joda.time.LocalDate
import org.mockito.{Matchers, Mockito}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.connectors.{CustomAuditConnector, NpsConnector}
import uk.gov.hmrc.statepension.domain._
import uk.gov.hmrc.statepension.domain.nps._
import org.mockito.Mockito._
import uk.gov.hmrc.statepension.builders.RateServiceBuilder
import uk.gov.hmrc.statepension.helpers.StubCustomAuditConnector

import scala.concurrent.Future

class StatePensionServiceSpec extends StatePensionUnitSpec with OneAppPerSuite with ScalaFutures with MockitoSugar {

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
      OldRules(basicStatePension = 119.30,
               additionalStatePension = 38.90,
               graduatedRetirementBenefit = 10.00
      )
    ),
    pensionAge = 64,
    pensionDate = new LocalDate(2018, 7, 6),
    finalRelevantYear = "2017-18",
    numberOfQualifyingYears = 30,
    pensionSharingOrder = false,
    currentFullWeeklyPensionAmount = 155.65,
    reducedRateElection = false
  )

  "Sandbox" should {
    "return dummy data for non-existent prefix" in {
      val nino: Nino = generateNinoWithPrefix("ZX")
      whenReady(SandboxStatePensionService.getStatement(nino)(HeaderCarrier())) { result =>
        result shouldBe Right(dummyStatement)
      }
    }

    "PS prefix should return a Post State Pension Age exclusion" in {
      whenReady(SandboxStatePensionService.getStatement(generateNinoWithPrefix("PS"))(HeaderCarrier())) { result =>
        result shouldBe Left(StatePensionExclusion(
          exclusionReasons = List(Exclusion.PostStatePensionAge),
          pensionAge = 66,
          pensionDate = new LocalDate(2021, 5, 16)
        ))
      }
    }

    "EZ prefix should return Dead exclusion" in {
      whenReady(SandboxStatePensionService.getStatement(generateNinoWithPrefix("EZ"))(HeaderCarrier())) { result =>
        result shouldBe Left(StatePensionExclusion(
          exclusionReasons = List(Exclusion.Dead),
          pensionAge = 66,
          pensionDate = new LocalDate(2021, 5, 16)
        ))
      }
    }

    "PG prefix should return MCI exclusion" in {
      whenReady(SandboxStatePensionService.getStatement(generateNinoWithPrefix("PG"))(HeaderCarrier())) { result =>
        result shouldBe Left(StatePensionExclusion(
          exclusionReasons = List(Exclusion.ManualCorrespondenceIndicator),
          pensionAge = 66,
          pensionDate = new LocalDate(2021, 5, 16)
        ))
      }
    }

  }

  "StatePensionService with a HOD Connection" when {

    val mockCitizenDetails = mock[CitizenDetailsService]
    when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any())).thenReturn(Future.successful(false))

    val defaultForecasting = new ForecastingService {
      override lazy val rateService: RateService = RateServiceBuilder.default
    }

    "there are no exclusions" when {
      "there is a regular statement (Reached)" should {

        val service = new NpsConnection {
          override lazy val nps: NpsConnector = mock[NpsConnector]
          override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
          override lazy val citizenDetailsService: CitizenDetailsService = mockCitizenDetails
          override lazy val metrics: Metrics = mock[Metrics]
          override val customAuditConnector: CustomAuditConnector = StubCustomAuditConnector
          override lazy val forecastingService: ForecastingService = defaultForecasting
          override lazy val rateService: RateService = RateServiceBuilder.default
        }

        val regularStatement = NpsSummary(
          earningsIncludedUpTo = new LocalDate(2016, 4, 5),
          sex = "F",
          statePensionAgeDate = new LocalDate(2019, 9, 6),
          finalRelevantStartYear = 2018,
          pensionSharingOrderSERPS = false,
          dateOfBirth = new LocalDate(1954, 3, 9),
          amounts = NpsStatePensionAmounts(
            pensionEntitlement = 161.18,
            startingAmount2016 = 161.18,
            protectedPayment2016 = 5.53,
            NpsAmountA2016(
              basicPension = 119.3,
              pre97AP = 17.79,
              post97AP = 6.03,
              post02AP = 15.4,
              pre88GMP = 0,
              post88GMP = 0,
              pre88COD = 0,
              post88COD = 0,
              grb = 2.66
            ),
            NpsAmountB2016(
              mainComponent = 155.65,
              rebateDerivedAmount = 0
            )
          )
        )

        when(service.nps.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          regularStatement
        ))

        when(service.nps.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          List()
        ))

        when(service.nps.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          NpsNIRecord(qualifyingYears = 36, List())
        ))

        val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

        "log a summary metric" in {
          verify(service.metrics, Mockito.atLeastOnce()).summary(
            Matchers.eq[BigDecimal](161.18),
            Matchers.eq[BigDecimal](161.18),
            Matchers.eq(false),
            Matchers.eq(Scenario.Reached),
            Matchers.eq[BigDecimal](161.18),
            Matchers.eq(0),
            Matchers.eq(None),
            Matchers.eq(false),
            Matchers.eq[BigDecimal](39.22),
            Matchers.eq[BigDecimal](2.66)
          )
        }

        "not log an exclusion metric" in {
          verify(service.metrics, never).exclusion(Matchers.any())
        }

        "return earningsIncludedUpTo of 2016-4-5" in {
          whenReady(statement) { sp =>
            sp.earningsIncludedUpTo shouldBe new LocalDate(2016, 4, 5)
          }
        }

        "return qualifying years of 36" in {
          whenReady(statement) { sp =>
            sp.numberOfQualifyingYears shouldBe 36
          }
        }

        "return pension date of 2019-9-6" in {
          whenReady(statement) { sp =>
            sp.pensionDate shouldBe new LocalDate(2019, 9, 6)
          }
        }

        "return oldRules Additonal Pension as 39.22" in {
          whenReady(statement) { sp =>
            sp.amounts.oldRules.additionalStatePension  shouldBe 39.22
          }
        }

        "return oldRules GraduatedRetirementsBenefits as 2.66" in {
          whenReady(statement) { sp =>
            sp.amounts.oldRules.graduatedRetirementBenefit shouldBe 2.66
          }
        }
        "return final relevant year" in {
          whenReady(statement) { sp =>
            sp.finalRelevantYear shouldBe "2018-19"
          }
        }

        "when there is a pensionSharingOrder return true" in {
          when(service.nps.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            regularStatement.copy(pensionSharingOrderSERPS = true)
          ))
          service.getStatement(generateNino()).right.get.pensionSharingOrder shouldBe true
        }

        "when there is no pensionSharingOrder return false" in {
          when(service.nps.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            regularStatement.copy(pensionSharingOrderSERPS = false)
          ))
          service.getStatement(generateNino()).right.get.pensionSharingOrder shouldBe false
        }

        "return pension age of 65" in {
          whenReady(statement) { sp =>
            sp.pensionAge shouldBe 65
          }
        }

        "return full state pension rate of 155.65" in {
          whenReady(statement) { sp =>
            sp.currentFullWeeklyPensionAmount shouldBe 155.65
          }
        }

        "when there is a protected payment of some value return true" in {
          when(service.nps.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            regularStatement.copy(amounts = regularStatement.amounts.copy(protectedPayment2016 = 0))
          ))
          service.getStatement(generateNino()).right.get.amounts.protectedPayment shouldBe false
        }

        "when there is a protected payment of 0 return false" in {
          when(service.nps.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            regularStatement.copy(amounts = regularStatement.amounts.copy(protectedPayment2016 = 6.66))
          ))
          service.getStatement(generateNino()).right.get.amounts.protectedPayment shouldBe true
        }

        "when there is a rebate derived amount of 12.34 it" should {
          when(service.nps.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            regularStatement.copy(amounts = regularStatement.amounts.copy(amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 12.34)))
          ))

          val statement = service.getStatement(generateNino()).right.get

          "return a weekly cope amount of 12.34" in {
            statement.amounts.cope.weeklyAmount shouldBe 12.34
          }

          "return a monthly cope amount of 53.66" in {
            statement.amounts.cope.monthlyAmount shouldBe 53.66
          }

          "return an annual cope amount of 643.88" in {
            statement.amounts.cope.annualAmount shouldBe 643.88
          }
        }

        "when there is a rebate derived amount of 0 it" should {
          when(service.nps.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            regularStatement.copy(amounts = regularStatement.amounts.copy(amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 0)))
          ))

          val statement = service.getStatement(generateNino()).right.get

          "return a weekly cope amount of 0" in {
            statement.amounts.cope.weeklyAmount shouldBe 0
          }

          "return a monthly cope amount of 0" in {
            statement.amounts.cope.monthlyAmount shouldBe 0
          }

          "return an annual cope amount of 0" in {
            statement.amounts.cope.annualAmount shouldBe 0
          }

        }

        "when there is an entitlement of 161.18 it" should {
          when(service.nps.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            regularStatement.copy(amounts = regularStatement.amounts.copy(pensionEntitlement = 161.18))
          ))

          val statement = service.getStatement(generateNino()).right.get

          "return a weekly current amount of 161.18" in {
            statement.amounts.current.weeklyAmount shouldBe 161.18
          }

          "return a monthly current amount of 161.18" in {
            statement.amounts.current.monthlyAmount shouldBe 700.85
          }

          "return an annual current amount of 161.18" in {
            service.getStatement(generateNino()).right.get.amounts.current.annualAmount shouldBe 8410.14
          }
        }

        "the forecast" should {
          "return a weekly current amount of 161.18" in {
            statement.amounts.forecast.weeklyAmount shouldBe 161.18
          }

          "return a monthly current amount of 161.18" in {
            statement.amounts.forecast.monthlyAmount shouldBe 700.85
          }

          "return an annual current amount of 161.18" in {
            statement.amounts.forecast.annualAmount shouldBe 8410.14
          }

          "return years to work 0 as they have reached" in {
            statement.amounts.forecast.yearsToWork.get shouldBe 0
          }
        }

        "when there is an entitlement of 0 it" should {
          when(service.nps.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
            regularStatement.copy(amounts = NpsStatePensionAmounts())
          ))

          val statement = service.getStatement(generateNino()).right.get

          "return a weekly current amount of 0" in {
            statement.amounts.current.weeklyAmount shouldBe 0
          }

          "return a monthly current amount of 0" in {
            statement.amounts.current.monthlyAmount shouldBe 0
          }

          "return an annual current amount of 0" in {
            statement.amounts.current.annualAmount shouldBe 0
          }
        }
      }

    }

    "there is a regular statement (Forecast)" should {

      val service = new NpsConnection {
        override lazy val nps: NpsConnector = mock[NpsConnector]
        override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
        override lazy val citizenDetailsService: CitizenDetailsService = mockCitizenDetails
        override lazy val metrics: Metrics = mock[Metrics]
        override val customAuditConnector: CustomAuditConnector = StubCustomAuditConnector
        override lazy val forecastingService: ForecastingService = defaultForecasting
        override lazy val rateService: RateService = RateServiceBuilder.default
      }

      val regularStatement = NpsSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "F",
        statePensionAgeDate = new LocalDate(2019, 9, 6),
        finalRelevantStartYear = 2018,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1954, 3, 9),
        amounts = NpsStatePensionAmounts(
          pensionEntitlement = 121.41,
          startingAmount2016 = 121.41,
          protectedPayment2016 = 5.53,
          NpsAmountA2016(
            basicPension = 79.53,
            pre97AP = 17.79,
            post97AP = 6.03,
            post02AP = 15.4,
            pre88GMP = 0,
            post88GMP = 0,
            pre88COD = 0,
            post88COD = 0,
            grb = 2.66
          ),
          NpsAmountB2016(
            mainComponent = 88.94,
            rebateDerivedAmount = 0
          )
        )
      )

      when(service.nps.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        regularStatement
      ))

      when(service.nps.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        List()
      ))

      when(service.nps.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        NpsNIRecord(qualifyingYears = 20, List())
      ))

      val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

      "the OldRules amounts" should {
        "return additionalPension amount of 39.22" in {
          statement.amounts.oldRules.additionalStatePension shouldBe 39.22
        }
        "return graduatedRetirementBenefits amount of 2.66" in {
          statement.amounts.oldRules.graduatedRetirementBenefit shouldBe 2.66
        }
      }

      "the forecast amount" should {

        "return a weekly forecast amount of 134.75" in {
          statement.amounts.forecast.weeklyAmount shouldBe 134.75
        }

        "return a monthly forecast amount of 585.92" in {
          statement.amounts.forecast.monthlyAmount shouldBe 585.92
        }

        "return an annual forecast amount of 7031.06" in {
          statement.amounts.forecast.annualAmount shouldBe 7031.06
        }

        "return years to work of 3 and that is how long is left" in {
          statement.amounts.forecast.yearsToWork.get shouldBe 3
        }

      }

      "log a summary metric" in {
        verify(service.metrics, times(1)).summary(
          Matchers.eq[BigDecimal](134.75),
          Matchers.eq[BigDecimal](121.41),
          Matchers.eq(false),
          Matchers.eq(Scenario.ContinueWorkingNonMax),
          Matchers.eq[BigDecimal](134.75),
          Matchers.eq(3),
          Matchers.eq(None),
          Matchers.eq(false),
          Matchers.eq[BigDecimal](39.22),
          Matchers.eq[BigDecimal](2.66)
        )
      }

      "not log an exclusion metric" in {
        verify(service.metrics, never).exclusion(Matchers.any())
      }
    }

    "there is a regular statement (Fill Gaps)" should {
      val service = new NpsConnection {
        override lazy val nps: NpsConnector = mock[NpsConnector]
        override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
        override lazy val citizenDetailsService: CitizenDetailsService = mockCitizenDetails
        override lazy val metrics: Metrics = mock[Metrics]
        override val customAuditConnector: CustomAuditConnector = StubCustomAuditConnector
        override lazy val forecastingService: ForecastingService = defaultForecasting
        override lazy val rateService: RateService = RateServiceBuilder.default
      }

      val regularStatement = NpsSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "F",
        statePensionAgeDate = new LocalDate(2019, 9, 6),
        finalRelevantStartYear = 2018,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1954, 3, 9),
        amounts = NpsStatePensionAmounts(
          pensionEntitlement = 121.4123,
          startingAmount2016 = 121.41,
          protectedPayment2016 = 5.53,
          NpsAmountA2016(
            basicPension = 79.53,
            pre97AP = 17.79,
            post97AP = 6.03,
            post02AP = 15.4,
            pre88GMP = 0,
            post88GMP = 0,
            pre88COD = 0,
            post88COD = 0,
            grb = 2.66
          ),
          NpsAmountB2016(
            mainComponent = 88.94,
            rebateDerivedAmount = 0
          )
        )
      )

      when(service.nps.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        regularStatement
      ))

      when(service.nps.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        List()
      ))

      when(service.nps.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        NpsNIRecord(qualifyingYears = 20, List(NpsNITaxYear(2000, false, false, true), NpsNITaxYear(2001, false, false, true)))
      ))

      lazy val summaryF: Future[NpsSummary] = service.nps.getSummary(Matchers.any())(Matchers.any())
      lazy val statement: Future[StatePension] = service.getStatement(generateNino()).right.get

      "the personal maximum amount" should {

        "return a weekly personal max amount of 142.71" in {
          statement.amounts.maximum.weeklyAmount shouldBe 142.71
        }

        "return a monthly personal max amount of 620.53" in {
          statement.amounts.maximum.monthlyAmount shouldBe 620.53
        }

        "return an annual personal max amount of 7446.40" in {
          statement.amounts.maximum.annualAmount shouldBe 7446.40
        }

        "return 2 gaps to fill" in {
          statement.amounts.maximum.gapsToFill shouldBe Some(2)
        }

        "return 3 years to work" in {
          statement.amounts.maximum.yearsToWork shouldBe Some(3)
        }
      }

      "summary have totalAP as 41.88" in {
        whenReady(summaryF) { summary =>
          summary.amounts.amountA2016.totalAP shouldBe 41.88
        }
      }

      "summary have additionalStatePension as 39.99" in {
        whenReady(summaryF) { summary =>
          summary.amounts.amountA2016.additionalStatePension shouldBe 39.22
        }
      }

      "summary have graduatedRetirementBenefits as 2.66" in {
        whenReady(summaryF) { summary =>
          summary.amounts.amountA2016.graduatedRetirementBenefit shouldBe 2.66
        }
      }
      "statePension have AdditionalStatePension as 39.22" in {
        whenReady(statement) { statePension =>
          statePension.amounts.oldRules.additionalStatePension shouldBe 39.22

        }
      }
      "statePension have graduatedRetirementBenefits as 2.66" in {
        whenReady(statement) { statePension =>
          statePension.amounts.oldRules.graduatedRetirementBenefit shouldBe 2.66
        }
      }

      "log a summary metric" in {
        verify(service.metrics, times(1)).summary(
            Matchers.eq[BigDecimal](134.75),
          Matchers.eq[BigDecimal](121.41),
          Matchers.eq(false),
          Matchers.eq(Scenario.FillGaps),
          Matchers.eq[BigDecimal](142.71),
          Matchers.eq(3),
          Matchers.eq(None),
          Matchers.eq(false),
          Matchers.eq[BigDecimal](39.22),
          Matchers.eq[BigDecimal](2.66)
        )
      }

      "not log an exclusion metric" in {
        verify(service.metrics, never).exclusion(Matchers.any())
      }

    }

    "there is an mqp user" should {
      val service = new NpsConnection {
        override lazy val nps: NpsConnector = mock[NpsConnector]
        override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
        override lazy val citizenDetailsService: CitizenDetailsService = mockCitizenDetails
        override lazy val metrics: Metrics = mock[Metrics]
        override val customAuditConnector: CustomAuditConnector = StubCustomAuditConnector
        override lazy val forecastingService: ForecastingService = defaultForecasting
        override lazy val rateService: RateService = RateServiceBuilder.default
      }

      val regularStatement = NpsSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "F",
        statePensionAgeDate = new LocalDate(2019, 9, 6),
        finalRelevantStartYear = 2018,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1954, 3, 9),
        amounts = NpsStatePensionAmounts(
          pensionEntitlement = 40.53,
          startingAmount2016 = 40.53,
          protectedPayment2016 = 0,
          NpsAmountA2016(
            basicPension = 35.79,
            pre97AP = 0,
            post97AP = 0,
            post02AP = 4.74,
            pre88GMP = 0,
            post88GMP = 0,
            pre88COD = 0,
            post88COD = 0,
            grb = 0
          ),
          NpsAmountB2016(
            mainComponent = 40.02,
            rebateDerivedAmount = 0
          )
        )
      )

      when(service.nps.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        regularStatement
      ))

      when(service.nps.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        List()
      ))

      when(service.nps.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        NpsNIRecord(qualifyingYears = 9, List())
      ))

      "return 0 for the current amount" in {
        lazy val statement: Future[StatePension] = service.getStatement(generateNino()).right.get
        statement.amounts.current.weeklyAmount shouldBe 0
      }
    }

    "the customer is dead" should {

      val service = new NpsConnection {
        override lazy val nps: NpsConnector = mock[NpsConnector]
        override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
        override lazy val citizenDetailsService: CitizenDetailsService = mockCitizenDetails
        override lazy val metrics: Metrics = mock[Metrics]
        override val customAuditConnector: CustomAuditConnector = StubCustomAuditConnector
        override lazy val forecastingService: ForecastingService = defaultForecasting
        override lazy val rateService: RateService = RateServiceBuilder.default
      }

      val summary = NpsSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "F",
        statePensionAgeDate = new LocalDate(2050, 7, 7),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1983, 7, 7),
        dateOfDeath = Some(new LocalDate(2000, 9, 13)),
        reducedRateElection = false,
        countryCode = 1,
        NpsStatePensionAmounts()
      )

      when(service.nps.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        NpsNIRecord(qualifyingYears = 35, List(NpsNITaxYear(2000, false, false, true), NpsNITaxYear(2001, false, false, true)))
      ))

      when(service.nps.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        summary
      ))

      when(service.nps.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        List()
      ))

      lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get

      "return dead exclusion" in {
        whenReady(exclusionF) { exclusion =>
          exclusion.exclusionReasons shouldBe List(Exclusion.Dead)
        }
      }

      "have a pension age of 67" in {
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionAge shouldBe 67
        }
      }

      "have a pension date of 2050-7-7" in {
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionDate shouldBe new LocalDate(2050, 7, 7)
        }
      }

      "log an exclusion metric" in {
        verify(service.metrics, times(1)).exclusion(
          Matchers.eq(Exclusion.Dead)
        )
      }

      "not log a summary metric" in {
        verify(service.metrics, never).summary(Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(),Matchers.any())
      }

    }

    "the customer is over state pension age" should {
      val service = new NpsConnection {
        override lazy val nps: NpsConnector = mock[NpsConnector]
        override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
        override lazy val citizenDetailsService: CitizenDetailsService = mockCitizenDetails
        override lazy val metrics: Metrics = mock[Metrics]
        override val customAuditConnector: CustomAuditConnector = StubCustomAuditConnector
        override lazy val forecastingService: ForecastingService = defaultForecasting
        override lazy val rateService: RateService = RateServiceBuilder.default
      }

      val summary = NpsSummary(
        earningsIncludedUpTo = new LocalDate(1954, 4, 5),
        sex = "F",
        statePensionAgeDate = new LocalDate(2016, 1, 1),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1954, 7, 7),
        dateOfDeath = None,
        reducedRateElection = false,
        countryCode = 1,
        NpsStatePensionAmounts()
      )


      when(service.nps.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        summary
      ))

      when(service.nps.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        List()
      ))

      when(service.nps.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        NpsNIRecord(qualifyingYears = 35, List(NpsNITaxYear(2000, false, false, true), NpsNITaxYear(2001, false, false, true)))
      ))

      lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get

      "return post state pension age exclusion" in {
        whenReady(exclusionF) { exclusion =>
          exclusion.exclusionReasons shouldBe List(Exclusion.PostStatePensionAge)
        }
      }

      "have a pension age of 61" in {
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionAge shouldBe 61
        }
      }

      "have a pension date of 2016-1-1" in {
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionDate shouldBe new LocalDate(2016, 1, 1)
        }
      }

      "log an exclusion metric" in {
        verify(service.metrics, times(1)).exclusion(
          Matchers.eq(Exclusion.PostStatePensionAge)
        )
      }

      "not log a summary metric" in {
        verify(service.metrics, never).summary(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),Matchers.any())
      }

    }

    "the customer has married women's reduced rate election" should {
      val service = new NpsConnection {
        override lazy val nps: NpsConnector = mock[NpsConnector]
        override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
        override lazy val citizenDetailsService: CitizenDetailsService = mockCitizenDetails
        override lazy val metrics: Metrics = mock[Metrics]
        override val customAuditConnector: CustomAuditConnector = StubCustomAuditConnector
        override lazy val forecastingService: ForecastingService = defaultForecasting
        override lazy val rateService: RateService = RateServiceBuilder.default
      }

      val summary = NpsSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "F",
        statePensionAgeDate = new LocalDate(2018, 1, 1),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1956, 7, 7),
        dateOfDeath = None,
        reducedRateElection = true,
        countryCode = 1,
        NpsStatePensionAmounts()
      )

      when(service.nps.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        summary
      ))

      when(service.nps.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        List()
      ))

      when(service.nps.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        NpsNIRecord(qualifyingYears = 35, List(NpsNITaxYear(2000, false, false, true), NpsNITaxYear(2001, false, false, true)))
      ))

      lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get

      lazy val summaryF: Future[NpsSummary] = service.nps.getSummary(Matchers.any())(Matchers.any())

      "summary have RRE flag as true" in {
        whenReady(summaryF) { summary =>
          summary.reducedRateElection shouldBe true
        }
      }

      "statePension have RRE flag as true" in {
        whenReady(statePensionF) { statePension =>
          statePension.reducedRateElection shouldBe true
        }
      }

      "log a summary metric" in {
        verify(service.metrics, times(1)).summary(
          Matchers.eq[BigDecimal](151.20),
          Matchers.eq[BigDecimal](0.00),
          Matchers.eq(false),
          Matchers.eq(Scenario.FillGaps),
          Matchers.eq[BigDecimal](155.65),
          Matchers.eq(34),
          Matchers.eq(Some(MQPScenario.ContinueWorking)),
          Matchers.eq(true),
          Matchers.eq[BigDecimal](0),
          Matchers.eq[BigDecimal](0)
        )
      }

    }

    "the customer has male overseas auto credits (abroad exclusion)" should {
      val service = new NpsConnection {
        override lazy val nps: NpsConnector = mock[NpsConnector]
        override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
        override lazy val citizenDetailsService: CitizenDetailsService = mockCitizenDetails
        override lazy val metrics: Metrics = mock[Metrics]
        override val customAuditConnector: CustomAuditConnector = StubCustomAuditConnector
        override lazy val forecastingService: ForecastingService = defaultForecasting
        override lazy val rateService: RateService = RateServiceBuilder.default
      }

      val summary = NpsSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "M",
        statePensionAgeDate = new LocalDate(2018, 1, 1),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1956, 7, 7),
        dateOfDeath = None,
        reducedRateElection = false,
        countryCode = 200,
        NpsStatePensionAmounts()
      )

      when(service.nps.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        summary
      ))
      when(service.nps.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        List()
      ))
      when(service.nps.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        NpsNIRecord(qualifyingYears = 35, List(NpsNITaxYear(2000, false, false, true), NpsNITaxYear(2001, false, false, true)))
      ))

      lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get

      "return abroad" in {
        whenReady(exclusionF) { exclusion =>
          exclusion.exclusionReasons shouldBe List(Exclusion.Abroad)
        }
      }

      "have a pension age of 61" in {
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionAge shouldBe 61
        }
      }

      "have a pension date of 2018-1-1" in {
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionDate shouldBe new LocalDate(2018, 1, 1)
        }
      }

      "log an exclusion metric" in {
        verify(service.metrics, times(1)).exclusion(
          Matchers.eq(Exclusion.Abroad)
        )
      }

      "not log a summary metric" in {
        verify(service.metrics, never).summary(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),Matchers.any())
      }

    }

    "the customer has amount dissonance" should {
      val service = new NpsConnection {
        override lazy val nps: NpsConnector = mock[NpsConnector]
        override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
        override lazy val citizenDetailsService: CitizenDetailsService = mockCitizenDetails
        override lazy val metrics: Metrics = mock[Metrics]
        override val customAuditConnector: CustomAuditConnector = StubCustomAuditConnector
        override lazy val forecastingService: ForecastingService = defaultForecasting
        override lazy val rateService: RateService = RateServiceBuilder.default
      }

      val summary = NpsSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "M",
        statePensionAgeDate = new LocalDate(2018, 1, 1),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1956, 7, 7),
        amounts = NpsStatePensionAmounts(
          pensionEntitlement = 155.65,
          startingAmount2016 = 155.65,
          amountB2016 = NpsAmountB2016(
            mainComponent = 155.64
          )
        )
      )

      when(service.nps.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        summary
      ))
      when(service.nps.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        List()
      ))
      when(service.nps.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        NpsNIRecord(qualifyingYears = 35, List(NpsNITaxYear(2000, false, false, true), NpsNITaxYear(2001, false, false, true)))
      ))

      lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get

      "return amount dissonance" in {
        whenReady(exclusionF) { exclusion =>
          exclusion.exclusionReasons shouldBe List(Exclusion.AmountDissonance)
        }
      }

      "have a pension age of 61" in {
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionAge shouldBe 61
        }
      }

      "have a pension date of 2018-1-1" in {
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionDate shouldBe new LocalDate(2018, 1, 1)
        }
      }

      "log an exclusion metric" in {
        verify(service.metrics, times(1)).exclusion(
          Matchers.eq(Exclusion.AmountDissonance)
        )
      }

      "not log a summary metric" in {
        verify(service.metrics, never).summary(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),Matchers.any())
      }
    }

    "the customer has contributed national insurance in the isle of man" should {
      val service = new NpsConnection {
        override lazy val nps: NpsConnector = mock[NpsConnector]
        override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
        override lazy val citizenDetailsService: CitizenDetailsService = mockCitizenDetails
        override lazy val metrics: Metrics = mock[Metrics]
        override val customAuditConnector: CustomAuditConnector = StubCustomAuditConnector
        override lazy val forecastingService: ForecastingService = defaultForecasting
        override lazy val rateService: RateService = RateServiceBuilder.default
      }

      val summary = NpsSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "M",
        statePensionAgeDate = new LocalDate(2018, 1, 1),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1956, 7, 7)
      )

      when(service.nps.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        summary
      ))
      when(service.nps.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        List(NpsLiability(5))
      ))
      when(service.nps.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        NpsNIRecord(qualifyingYears = 35, List(NpsNITaxYear(2000, false, false, true), NpsNITaxYear(2001, false, false, true)))
      ))


      lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get

      "return isle of man exclusion" in {
        whenReady(exclusionF) { exclusion =>
          exclusion.exclusionReasons shouldBe List(Exclusion.IsleOfMan)
        }
      }

      "have a pension age of 61" in {
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionAge shouldBe 61
        }
      }

      "have a pension date of 2018-1-1" in {
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionDate shouldBe new LocalDate(2018, 1, 1)
        }
      }

      "log an exclusion metric" in {
        verify(service.metrics, times(1)).exclusion(
          Matchers.eq(Exclusion.IsleOfMan)
        )
      }

      "not log a summary metric" in {
        verify(service.metrics, never).summary(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),Matchers.any())
      }
    }

    "the customer has a manual correspondence indicator" should {
      val service = new NpsConnection {
        override lazy val nps: NpsConnector = mock[NpsConnector]
        override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
        override lazy val citizenDetailsService: CitizenDetailsService = mock[CitizenDetailsService]
        override lazy val metrics: Metrics = mock[Metrics]
        override val customAuditConnector: CustomAuditConnector = StubCustomAuditConnector
        override lazy val forecastingService: ForecastingService = defaultForecasting
        override lazy val rateService: RateService = RateServiceBuilder.default
      }

      val summary = NpsSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "M",
        statePensionAgeDate = new LocalDate(2018, 1, 1),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1956, 7, 7)
      )

      when(service.nps.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        summary
      ))
      when(service.nps.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        List()
      ))
      when(service.citizenDetailsService.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any())).thenReturn(Future.successful(true))
      when(service.nps.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        NpsNIRecord(qualifyingYears = 35, List(NpsNITaxYear(2000, false, false, true), NpsNITaxYear(2001, false, false, true)))
      ))

      lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get

      "return mci exclusion" in {
        whenReady(exclusionF) { exclusion =>
          exclusion.exclusionReasons shouldBe List(Exclusion.ManualCorrespondenceIndicator)
        }
      }

      "have a pension age of 61" in {
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionAge shouldBe 61
        }
      }

      "have a pension date of 2018-1-1" in {
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionDate shouldBe new LocalDate(2018, 1, 1)
        }
      }

      "log an exclusion metric" in {
        verify(service.metrics, times(1)).exclusion(
          Matchers.eq(Exclusion.ManualCorrespondenceIndicator)
        )
      }

      "not log a summary metric" in {
        verify(service.metrics, never).summary(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),Matchers.any())
      }
    }
  }
}
