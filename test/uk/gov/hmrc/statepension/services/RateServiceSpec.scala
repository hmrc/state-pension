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

import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.builders.RateServiceBuilder

import scala.math.BigDecimal.RoundingMode

class RateServiceSpec extends StatePensionUnitSpec {

  val testRateService: RateService = RateServiceBuilder.default

  "ratesTable" should {
    "parse the config correctly" in {
      testRateService.ratesTable shouldBe Map(
        0 -> 0,
        1 -> 4.45,
        2 -> 8.89,
        3 -> 13.34,
        4 -> 17.79,
        5 -> 22.24,
        6 -> 26.68,
        7 -> 31.13,
        8 -> 35.58,
        9 -> 40.02,
        10 -> 44.47,
        11 -> 48.92,
        12 -> 53.37,
        13 -> 57.81,
        14 -> 62.26,
        15 -> 66.71,
        16 -> 71.15,
        17 -> 75.6,
        18 -> 80.05,
        19 -> 84.5,
        20 -> 88.94,
        21 -> 93.39,
        22 -> 97.84,
        23 -> 102.28,
        24 -> 106.73,
        25 -> 111.18,
        26 -> 115.63,
        27 -> 120.07,
        28 -> 124.52,
        29 -> 128.97,
        30 -> 133.41,
        31 -> 137.86,
        32 -> 142.31,
        33 -> 146.76,
        34 -> 151.2,
        35 -> 155.65
      )
    }
  }

  "max years" should {
    "be the highest key in the ratesTable even when it's not at the end of the map" in {
      val service = RateServiceBuilder(Map(
        1 -> 10,
        2 -> 20,
        5 -> 50,
        3 -> 30
      ))
      service.MAX_YEARS shouldBe 5
    }
  }

  "max amount" should {
    val service = RateServiceBuilder(Map(
      1 -> 10,
      2 -> 20,
      5 -> 50,
      3 -> 30
    ))
    service.MAX_AMOUNT shouldBe 50
  }

  "getSPAmount called" should {
    "return None for no years" in {
      testRateService.getSPAmount(0) shouldBe 0
    }

    "return the maximum amount for a high number" in {
      testRateService.getSPAmount(100) shouldBe 155.65
    }

    "return the maximum amount for 35" in {
      testRateService.getSPAmount(35) shouldBe 155.65
    }

    "22 Qualifying years should return £97.84" in {
      testRateService.getSPAmount(22) shouldBe 97.84
    }

    "17 Qualifying years should return £75.60" in {
      testRateService.getSPAmount(17) shouldBe 75.60
    }
  }

  "yearsNeededForAmount" when {
    "the amount needed is 0 or less" should {
      "return 0" in {
        testRateService.yearsNeededForAmount(0) shouldBe 0
        testRateService.yearsNeededForAmount(-1) shouldBe 0
      }
    }

    "the amount needed is 1p" should {
      "return 1 year" in {
        testRateService.yearsNeededForAmount(0.01) shouldBe 1
      }
    }

    "the amount needed is 4.45 (the exact amount needed for one year)" should {
      "return 1 year" in {
        testRateService.yearsNeededForAmount(4.45) shouldBe 1
      }
    }

    "the amount needed is 4.46 (1p over the the exact amount needed for one year)" should {
      "return 2 year" in {
        testRateService.yearsNeededForAmount(4.46) shouldBe 2
      }
    }

    "the amount needed is 123" should {
      "return 28 years" in {
        testRateService.yearsNeededForAmount(123) shouldBe 28
      }
    }
  }

  "getBasicSPAmount called" should {
    "return none for no years" in {
      testRateService.getBasicSPAmount(0) shouldBe 0
    }

    "return 119.30 for 30 years" in {
      testRateService.getBasicSPAmount(30) shouldBe 119.30
    }

    "return 119.30 for 31 years" in {
      testRateService.getBasicSPAmount(31) shouldBe 119.30
    }

    "return 99.42 for 25 years" in {
      testRateService.getBasicSPAmount(25) shouldBe 99.42
    }

    "return 87.49 for 22 years" in {
      testRateService.getBasicSPAmount(22) shouldBe 87.49
    }

    "return 39.77 for 10 years" in {
      testRateService.getBasicSPAmount(10) shouldBe 39.77
    }

    "return 3.98 for 1 year" in {
      testRateService.getBasicSPAmount(1) shouldBe 3.98
    }
  }

}
