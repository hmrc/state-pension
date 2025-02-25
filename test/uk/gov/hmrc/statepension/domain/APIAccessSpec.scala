/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.libs.json.{JsSuccess, Json}
import org.scalatest.wordspec.AnyWordSpec
import org.scalactic.Prettifier.default

class APIAccessSpec extends AnyWordSpec with Matchers {

  "APIAccess" must {
    "serialize and deserialize correctly with whitelistedApplicationIds" in {
      val apiAccess = APIAccess(`type` = "PUBLIC", whitelistedApplicationIds = Some(Seq("app1", "app2")))

      val json = Json.toJson(apiAccess)
      val parsed = json.validate[APIAccess]

      parsed shouldBe JsSuccess(apiAccess)
    }

    "serialize and deserialize correctly without whitelistedApplicationIds" in {
      val apiAccess = APIAccess(`type` = "PRIVATE", whitelistedApplicationIds = None)

      val json = Json.toJson(apiAccess)
      val parsed = json.validate[APIAccess]

      parsed shouldBe JsSuccess(apiAccess)
    }
  }
}
