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

package uk.gov.hmrc.statepension.domain.nps

import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.domain.StatePensionAmount

class StatePensionSpec extends StatePensionUnitSpec {

  "StatePensionAmount" should {
    "Weekly / Monthly / Annual Calculation" should {
      "return 151.25, 657.67, 7892.01" in {
        StatePensionAmount(None, None, 151.25).monthlyAmount shouldBe 657.67
        StatePensionAmount(None, None, 151.25).annualAmount shouldBe 7892.01
      }

      "return 43.21, 187.89, 2254.64" in {
        StatePensionAmount(None, None, 43.21).monthlyAmount shouldBe 187.89
        StatePensionAmount(None, None, 43.21).annualAmount shouldBe 2254.64
      }

      "return 95.07, 413.38, 4960.62" in {
        StatePensionAmount(None, None, 95.07).monthlyAmount shouldBe 413.38
        StatePensionAmount(None, None, 95.07).annualAmount shouldBe 4960.62
      }

      "yearsToWork and gapsToFill have no bearing on calculation" in {
        StatePensionAmount(Some(2), None, 95.07).monthlyAmount shouldBe 413.38
        StatePensionAmount(None, Some(2), 95.07).annualAmount shouldBe 4960.62
        StatePensionAmount(None, Some(2), 95.07).monthlyAmount shouldBe 413.38
        StatePensionAmount(Some(2), None, 95.07).annualAmount shouldBe 4960.62
        StatePensionAmount(Some(2), Some(2), 95.07).monthlyAmount shouldBe 413.38
        StatePensionAmount(Some(2), Some(2), 95.07).annualAmount shouldBe 4960.62
      }
    }
  }
}
