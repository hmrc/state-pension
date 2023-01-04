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

class LiabilityTypeSpec extends StatePensionBaseSpec {
    "ISLE_OF_MAN" should {
      "be 5" in {
        LiabilityType.ISLE_OF_MAN shouldBe 5
      }
      "not be 15 (other than 5)" in {
        LiabilityType.ISLE_OF_MAN should not be 15
      }
    }
}
