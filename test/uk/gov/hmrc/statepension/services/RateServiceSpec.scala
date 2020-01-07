/*
 * Copyright 2020 HM Revenue & Customs
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

import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.builders.RateServiceBuilder

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

  "revaluationRates" should {
    "parse the config correctly" in {
      val rates = RateServiceBuilder.apply(Map(0 -> 0), revaluationStartingAmount = 2.5, revaluationProtectedPayment = 1.01).revaluationRates
      rates.startingAmount shouldBe 2.5
      rates.protectedPayment shouldBe 1.01
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
    "be the highest key in the ratesTable even when it's not at the end of the map" in {
      val service = RateServiceBuilder(Map(
        1 -> 10,
        2 -> 20,
        5 -> 50,
        3 -> 30
      ))
      service.MAX_AMOUNT shouldBe 50
    }
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
    "return 0 for 0 years" in {
      testRateService.getBasicSPAmount(0) shouldBe 0
    }

    "return 3.98 for 1 year" in {
      testRateService.getBasicSPAmount(1) shouldBe 3.98
    }

    "return 7.95 for 2 years" in {
      testRateService.getBasicSPAmount(2) shouldBe 7.95
    }

    "return 11.93 for 3 years" in {
      testRateService.getBasicSPAmount(3) shouldBe 11.93
    }

    "return 15.91 for 4 years" in {
      testRateService.getBasicSPAmount(4) shouldBe 15.91
    }

    "return 19.88 for 5 years" in {
      testRateService.getBasicSPAmount(5) shouldBe 19.88
    }

    "return 23.86 for 6 years" in {
      testRateService.getBasicSPAmount(6) shouldBe 23.86
    }

    "return 27.84 for 7 years" in {
      testRateService.getBasicSPAmount(7) shouldBe 27.84
    }

    "return 31.81 for 8 years" in {
      testRateService.getBasicSPAmount(8) shouldBe 31.81
    }

    "return 35.79 for 9 years" in {
      testRateService.getBasicSPAmount(9) shouldBe 35.79
    }

    "return 39.77 for 10 years" in {
      testRateService.getBasicSPAmount(10) shouldBe 39.77
    }

    "return 43.74 for 11 years" in {
      testRateService.getBasicSPAmount(11) shouldBe 43.74
    }

    "return 47.72 for 12 years" in {
      testRateService.getBasicSPAmount(12) shouldBe 47.72
    }

    "return 51.7 for 13 years" in {
      testRateService.getBasicSPAmount(13) shouldBe 51.7
    }

    "return 55.67 for 14 years" in {
      testRateService.getBasicSPAmount(14) shouldBe 55.67
    }

    "return 59.65 for 15 years" in {
      testRateService.getBasicSPAmount(15) shouldBe 59.65
    }

    "return 63.63 for 16 years" in {
      testRateService.getBasicSPAmount(16) shouldBe 63.63
    }

    "return 67.6 for 17 years" in {
      testRateService.getBasicSPAmount(17) shouldBe 67.6
    }

    "return 71.58 for 18 years" in {
      testRateService.getBasicSPAmount(18) shouldBe 71.58
    }

    "return 75.56 for 19 years" in {
      testRateService.getBasicSPAmount(19) shouldBe 75.56
    }

    "return 79.53 for 20 years" in {
      testRateService.getBasicSPAmount(20) shouldBe 79.53
    }

    "return 83.51 for 21 years" in {
      testRateService.getBasicSPAmount(21) shouldBe 83.51
    }

    "return 87.49 for 22 years" in {
      testRateService.getBasicSPAmount(22) shouldBe 87.49
    }

    "return 91.46 for 23 years" in {
      testRateService.getBasicSPAmount(23) shouldBe 91.46
    }

    "return 95.44 for 24 years" in {
      testRateService.getBasicSPAmount(24) shouldBe 95.44
    }

    "return 99.42 for 25 years" in {
      testRateService.getBasicSPAmount(25) shouldBe 99.42
    }

    "return 103.39 for 26 years" in {
      testRateService.getBasicSPAmount(26) shouldBe 103.39
    }

    "return 107.37 for 27 years" in {
      testRateService.getBasicSPAmount(27) shouldBe 107.37
    }

    "return 111.35 for 28 years" in {
      testRateService.getBasicSPAmount(28) shouldBe 111.35
    }

    "return 115.32 for 29 years" in {
      testRateService.getBasicSPAmount(29) shouldBe 115.32
    }

    "return 119.3 for 30 years" in {
      testRateService.getBasicSPAmount(30) shouldBe 119.30
    }

    "return 119.3 for 31 years" in {
      testRateService.getBasicSPAmount(31) shouldBe 119.30
    }
  }

  "getSPAmount2016" should {
    "return 0 for 0 years" in {
      testRateService.getSPAmount2016(0) shouldBe 0
    }

    "return 4.45 for 1 years" in {
      testRateService.getSPAmount2016(1) shouldBe 4.45
    }

    "return 8.89 for 2 years" in {
      testRateService.getSPAmount2016(2) shouldBe 8.89
    }

    "return 13.34 for 3 years" in {
      testRateService.getSPAmount2016(3) shouldBe 13.34
    }

    "return 17.79 for 4 years" in {
      testRateService.getSPAmount2016(4) shouldBe 17.79
    }

    "return 22.24 for 5 years" in {
      testRateService.getSPAmount2016(5) shouldBe 22.24
    }

    "return 26.68 for 6 years" in {
      testRateService.getSPAmount2016(6) shouldBe 26.68
    }

    "return 31.13 for 7 years" in {
      testRateService.getSPAmount2016(7) shouldBe 31.13
    }

    "return 35.58 for 8 years" in {
      testRateService.getSPAmount2016(8) shouldBe 35.58
    }

    "return 40.02 for 9 years" in {
      testRateService.getSPAmount2016(9) shouldBe 40.02
    }

    "return 44.47 for 10 years" in {
      testRateService.getSPAmount2016(10) shouldBe 44.47
    }

    "return 48.92 for 11 years" in {
      testRateService.getSPAmount2016(11) shouldBe 48.92
    }

    "return 53.37 for 12 years" in {
      testRateService.getSPAmount2016(12) shouldBe 53.37
    }

    "return 57.81 for 13 years" in {
      testRateService.getSPAmount2016(13) shouldBe 57.81
    }

    "return 62.26 for 14 years" in {
      testRateService.getSPAmount2016(14) shouldBe 62.26
    }

    "return 66.71 for 15 years" in {
      testRateService.getSPAmount2016(15) shouldBe 66.71
    }

    "return 71.15 for 16 years" in {
      testRateService.getSPAmount2016(16) shouldBe 71.15
    }

    "return 75.6 for 17 years" in {
      testRateService.getSPAmount2016(17) shouldBe 75.6
    }

    "return 80.05 for 18 years" in {
      testRateService.getSPAmount2016(18) shouldBe 80.05
    }

    "return 84.5 for 19 years" in {
      testRateService.getSPAmount2016(19) shouldBe 84.5
    }

    "return 88.94 for 20 years" in {
      testRateService.getSPAmount2016(20) shouldBe 88.94
    }

    "return 93.39 for 21 years" in {
      testRateService.getSPAmount2016(21) shouldBe 93.39
    }

    "return 97.84 for 22 years" in {
      testRateService.getSPAmount2016(22) shouldBe 97.84
    }

    "return 102.28 for 23 years" in {
      testRateService.getSPAmount2016(23) shouldBe 102.28
    }

    "return 106.73 for 24 years" in {
      testRateService.getSPAmount2016(24) shouldBe 106.73
    }

    "return 111.18 for 25 years" in {
      testRateService.getSPAmount2016(25) shouldBe 111.18
    }

    "return 115.63 for 26 years" in {
      testRateService.getSPAmount2016(26) shouldBe 115.63
    }

    "return 120.07 for 27 years" in {
      testRateService.getSPAmount2016(27) shouldBe 120.07
    }

    "return 124.52 for 28 years" in {
      testRateService.getSPAmount2016(28) shouldBe 124.52
    }

    "return 128.97 for 29 years" in {
      testRateService.getSPAmount2016(29) shouldBe 128.97
    }

    "return 133.41 for 30 years" in {
      testRateService.getSPAmount2016(30) shouldBe 133.41
    }

    "return 137.86 for 31 years" in {
      testRateService.getSPAmount2016(31) shouldBe 137.86
    }

    "return 142.31 for 32 years" in {
      testRateService.getSPAmount2016(32) shouldBe 142.31
    }

    "return 146.76 for 33 years" in {
      testRateService.getSPAmount2016(33) shouldBe 146.76
    }

    "return 151.2 for 34 years" in {
      testRateService.getSPAmount2016(34) shouldBe 151.2
    }

    "return 155.65 for 35 years" in {
      testRateService.getSPAmount2016(35) shouldBe 155.65
    }

    "return 155.65 for 36 years" in {
      testRateService.getSPAmount2016(36) shouldBe 155.65
    }

  }

}
