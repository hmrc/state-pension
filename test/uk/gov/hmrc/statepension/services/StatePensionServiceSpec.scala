/*
 * Copyright 2016 HM Revenue & Customs
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
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.domain._

class StatePensionServiceSpec extends StatePensionUnitSpec with OneAppPerSuite with ScalaFutures {

  private val dummyStatement: StatePension = StatePension(
    // scalastyle:off magic.number
    earningsIncludedUpTo = new LocalDate(2015, 4, 5),
    amounts = StatePensionAmounts(
      protectedPayment = false,
      current = StatePensionAmount(
        None,
        None,
        133.41,
        580.10,
        6961.14
      ),
      forecast = StatePensionAmount(
        yearsToWork = Some(3),
        None,
        146.76,
        638.14,
        7657.73
      ),
      maximum = StatePensionAmount(
        yearsToWork = Some(3),
        gapsToFill = Some(2),
        weeklyAmount = 155.65,
        monthlyAmount = 676.80,
        annualAmount = 8121.59
      ),
      cope = StatePensionAmount(
        None,
        None,
        0.00,
        0.00,
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
    
  }
}
