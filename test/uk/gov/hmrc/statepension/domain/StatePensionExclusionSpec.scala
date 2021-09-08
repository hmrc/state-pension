/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.statepension.domain

import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.{JsError, JsString, JsSuccess}
import uk.gov.hmrc.statepension.StatePensionBaseSpec

class StatePensionExclusionSpec extends StatePensionBaseSpec {

  val jsonExclusionList: List[(JsString, Exclusion)] = List(
    (JsString("Dead"), Exclusion.Dead),
    (JsString("IsleOfMan"), Exclusion.IsleOfMan),
    (JsString("ManualCorrespondenceIndicator"), Exclusion.ManualCorrespondenceIndicator),
    (JsString("PostStatePensionAge"), Exclusion.PostStatePensionAge),
    (JsString("AmountDissonance"), Exclusion.AmountDissonance)
  )

  "Exclusion reads" should {
    "write to JSON successfully" in {
      jsonExclusionList foreach {
        case (json, exclusion) =>
          Exclusion.ExclusionFormat.reads(json) shouldBe JsSuccess(exclusion)
      }
    }

    "return JsError when given invalid Json" in {
      Exclusion.ExclusionFormat.reads(JsString("{‘")) shouldBe JsError("Exclusion not valid!")
    }
  }

  "Exclusion writes" should {
    "convert an Exclusion" in {
      jsonExclusionList foreach {
        case (json, exclusion) =>
          Exclusion.ExclusionFormat.writes(exclusion) shouldBe json
      }
    }
  }

}
