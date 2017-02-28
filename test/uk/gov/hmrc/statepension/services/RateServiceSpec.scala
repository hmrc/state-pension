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

import uk.gov.hmrc.statepension.StatePensionUnitSpec

import scala.math.BigDecimal.RoundingMode

class RateServiceSpec extends StatePensionUnitSpec {

  "spAmountPerYear" should {
    "return 4.32 (Maximum Amount - 155.65 divided by Maximum Years - 35" in {
      RateService.spAmountPerYear.setScale(8, RoundingMode.HALF_UP) shouldBe BigDecimal(4.44714286)
    }
  }

  "getSPAmount called" should {
    "return None for no years" in {
      RateService.getSPAmount(0) shouldBe 0
    }

    "return the maximum amount for a high number" in {
      RateService.getSPAmount(100) shouldBe 155.65
    }

    "return the maximum amount for 35" in {
      RateService.getSPAmount(35) shouldBe 155.65
    }

    "22 Qualifying years should return £97.84" in {
      RateService.getSPAmount(22).setScale(10, RoundingMode.FLOOR) shouldBe BigDecimal((155.65 / 35) * 22).setScale(10, RoundingMode.FLOOR)
    }

    "17 Qualifying years should return £75.60" in {
      RateService.getSPAmount(17).setScale(10, RoundingMode.FLOOR) shouldBe BigDecimal((155.65 / 35) * 17).setScale(10, RoundingMode.FLOOR)
    }
  }

  "getBasicSPAmount called" should {
    "return none for no years" in {
      RateService.getBasicSPAmount(0) shouldBe 0
    }

    "return 119.30 for 30 years" in {
      RateService.getBasicSPAmount(30) shouldBe 119.30
    }

    "return 119.30 for 31 years" in {
      RateService.getBasicSPAmount(31) shouldBe 119.30
    }

    "return 99.42 for 25 years" in {
      RateService.getBasicSPAmount(25) shouldBe 99.42
    }

    "return 87.49 for 22 years" in {
      RateService.getBasicSPAmount(22) shouldBe 87.49
    }

    "return 39.77 for 10 years" in {
      RateService.getBasicSPAmount(10) shouldBe 39.77
    }

    "return 3.98 for 1 year" in {
      RateService.getBasicSPAmount(1) shouldBe 3.98
    }
  }


}
