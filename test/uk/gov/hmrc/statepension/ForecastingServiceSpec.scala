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

package uk.gov.hmrc.statepension

import uk.gov.hmrc.statepension.services.ForecastingService

class ForecastingServiceSpec extends StatePensionUnitSpec {

  "calculateStartingAmount" when {
    "Amount A is higher than Amount B" should {
      "return Amount A" in {
        ForecastingService.calculateStartingAmount(100.00, 99.99) shouldBe 100
      }
    }

    "Amount B is higher than Amount A" should {
      "return Amount B" in {
        ForecastingService.calculateStartingAmount(99.99, 100.00) shouldBe 100
      }
    }

    "Amount B are equal Amount A" should {
      "return the value" in {
        ForecastingService.calculateStartingAmount(99, 99) shouldBe 99
      }
    }

  }

}
