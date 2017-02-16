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
import org.mockito.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.connectors.NpsConnector
import uk.gov.hmrc.statepension.domain._
import uk.gov.hmrc.statepension.domain.nps.{NpsAmountA2016, NpsAmountB2016, NpsStatePensionAmounts, NpsSummary}
import org.mockito.Mockito._

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
      )
    ),
    pensionAge = 64,
    pensionDate = new LocalDate(2018, 7, 6),
    finalRelevantYear = "2017-18",
    numberOfQualifyingYears = 30,
    pensionSharingOrder = false,
    currentFullWeeklyPensionAmount = 155.65
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

    "there is a regular statement" should {

      val service = new NpsConnection {
        override lazy val nps: NpsConnector = mock[NpsConnector]
        override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
      }

      val regularStatement = NpsSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "F",
        qualifyingYears = 36,
        statePensionAgeDate = new LocalDate(2019, 9, 6),
        finalRelevantStartYear = 2018,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1954, 3, 9),
        amounts = NpsStatePensionAmounts(
          pensionEntitlement = 161.18,
          startingAmount2016 = 161.18,
          protectedPayment2016 = 5.53,
          additionalPensionAccruedLastTaxYear = 2.36,
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

      when(service.nps.getSummary).thenReturn(Future.successful(
        regularStatement
      ))

      lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get

      "return earningsIncludedUpTo of 2016-4-5" in {
        whenReady(statePensionF) { sp =>
          sp.earningsIncludedUpTo shouldBe new LocalDate(2016, 4, 5)
        }
      }

      "return qualifying years of 36" in {
        whenReady(statePensionF) { sp =>
          sp.numberOfQualifyingYears shouldBe 36
        }
      }

      "return pension date of 2019-9-6" in {
        whenReady(statePensionF) { sp =>
          sp.pensionDate shouldBe new LocalDate(2019, 9, 6)
        }
      }

      "return final relevant year" in {
        whenReady(statePensionF) { sp =>
          sp.finalRelevantYear shouldBe "2018-19"
        }
      }

      "when there is a pensionSharingOrder return true" in {
        when(service.nps.getSummary).thenReturn(Future.successful(
          regularStatement.copy(pensionSharingOrderSERPS = true)
        ))
        service.getStatement(generateNino()).right.get.pensionSharingOrder shouldBe true
      }

      "when there is no pensionSharingOrder return false" in {
        when(service.nps.getSummary).thenReturn(Future.successful(
          regularStatement.copy(pensionSharingOrderSERPS = false)
        ))
        service.getStatement(generateNino()).right.get.pensionSharingOrder shouldBe false
      }

      "return pension age of 65" in {
        whenReady(statePensionF) { sp =>
          sp.pensionAge shouldBe 65
        }
      }

      "return full state pension rate of 155.65" in {
        whenReady(statePensionF) { sp =>
          sp.currentFullWeeklyPensionAmount shouldBe 155.65
        }
      }

      "when there is a protected payment of some value return true" in {
        when(service.nps.getSummary).thenReturn(Future.successful(
          regularStatement.copy(amounts = regularStatement.amounts.copy(protectedPayment2016 = 0))
        ))
        service.getStatement(generateNino()).right.get.amounts.protectedPayment shouldBe false
      }

      "when there is a protected payment of 0 return false" in {
        when(service.nps.getSummary).thenReturn(Future.successful(
          regularStatement.copy(amounts = regularStatement.amounts.copy(protectedPayment2016 = 6.66))
        ))
        service.getStatement(generateNino()).right.get.amounts.protectedPayment shouldBe true
      }

      "when there is a rebate derived amount of 12.34 it" should {
        when(service.nps.getSummary).thenReturn(Future.successful(
          regularStatement.copy(amounts = regularStatement.amounts.copy(amountB2016 = regularStatement.amounts.amountB2016.copy(rebateDerivedAmount = 12.34)))
        ))

        val statement = service.getStatement(generateNino()).right.get

        "return a weekly cope amount of 12.34" in {
          statement.amounts.cope.weeklyAmount shouldBe 12.34
        }

        "return a monthly cope amount of 12.34" in {
          statement.amounts.cope.monthlyAmount shouldBe 53.66
        }

        "return an annual cope amount of 12.34" in {
          statement.amounts.cope.annualAmount shouldBe 643.88
        }
      }

      "when there is a rebate derived amount of 0 it" should {
        when(service.nps.getSummary).thenReturn(Future.successful(
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
        when(service.nps.getSummary).thenReturn(Future.successful(
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

      "when there is an entitlement of 0 it" should {
        when(service.nps.getSummary).thenReturn(Future.successful(
          regularStatement.copy(amounts = regularStatement.amounts.copy(pensionEntitlement = 0))
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

    "the customer is dead" should {

      val service = new NpsConnection {
        override lazy val nps: NpsConnector = mock[NpsConnector]
        override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
      }

      val summary = NpsSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "F",
        qualifyingYears = 35,
        statePensionAgeDate = new LocalDate(2050, 7, 7),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1983, 7, 7),
        dateOfDeath = Some(new LocalDate(2000, 9, 13)),
        NpsStatePensionAmounts()
      )


      when(service.nps.getSummary).thenReturn(Future.successful(
        summary
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
    }

    "the customer is over state pension age" should {
      val service = new NpsConnection {
        override lazy val nps: NpsConnector = mock[NpsConnector]
        override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
      }

      val summary = NpsSummary(
        earningsIncludedUpTo = new LocalDate(1954, 4, 5),
        sex = "F",
        qualifyingYears = 35,
        statePensionAgeDate = new LocalDate(2016, 1, 1),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1954, 7, 7),
        dateOfDeath = None,
        NpsStatePensionAmounts()
      )


      when(service.nps.getSummary).thenReturn(Future.successful(
        summary
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

      "have a pension date of 2050-7-7" in {
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionDate shouldBe new LocalDate(2016, 1, 1)
        }
      }
    }
  }
}
