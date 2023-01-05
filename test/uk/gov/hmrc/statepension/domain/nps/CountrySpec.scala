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

package uk.gov.hmrc.statepension.domain.nps

import uk.gov.hmrc.statepension.StatePensionBaseSpec

class CountrySpec extends StatePensionBaseSpec {

  "Not Specified should be 0" in {
    Country.NOT_SPECIFIED shouldBe 0
  }

  "Great Britain should be 1" in {
    Country.GREAT_BRITAIN shouldBe 1
  }

  "Northern Ireland should be 8" in {
    Country.NORTHERN_IRELAND shouldBe 8
  }

  "England should be 114" in {
    Country.ENGLAND shouldBe 114
  }

  "Scotland should be 115" in {
    Country.SCOTLAND shouldBe 115
  }

  "Wales should be 116" in {
    Country.WALES shouldBe 116
  }

  "Isle of Man should be 7" in {
    Country.ISLE_OF_MAN shouldBe 7
  }

  "isAbroad" should {
    "should return true if the Country is not in the UK" in {
      Country.isAbroad(222) shouldBe true
    }

    "should return false if the Country is GREAT BRITAIN" in {
      Country.isAbroad(Country.GREAT_BRITAIN) shouldBe false
    }

    "should return false if the Country is ISLE OF MAN" in {
      Country.isAbroad(Country.ISLE_OF_MAN) shouldBe false
    }

    "should return false if the Country is NORTHERN IRELAND" in {
      Country.isAbroad(Country.NORTHERN_IRELAND) shouldBe false
    }

    "should return false if the Country is ENGLAND" in {
      Country.isAbroad(Country.ENGLAND) shouldBe false
    }

    "should return false if the Country is SCOTLAND" in {
      Country.isAbroad(Country.SCOTLAND) shouldBe false
    }

    "should return false if the Country is WALES" in {
      Country.isAbroad(Country.WALES) shouldBe false
    }

    "should return false if the Country is NOT SPECIFIED OR NOT USED" in {
      Country.isAbroad(Country.NOT_SPECIFIED) shouldBe false
    }

  }
}
