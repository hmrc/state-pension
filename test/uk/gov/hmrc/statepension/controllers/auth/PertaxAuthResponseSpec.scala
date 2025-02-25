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

package uk.gov.hmrc.statepension.controllers.auth

import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}
import org.scalatest.matchers.must.Matchers.mustBe

class PertaxAuthResponseSpec extends AnyWordSpec {

  "PertaxAuthResponse" must {
    "serialize and deserialize correctly" in {
      val response = PertaxAuthResponse("AUTH_SUCCESS", "Authentication successful")

      val json = Json.toJson(response)
      val parsed = json.validate[PertaxAuthResponse]

      parsed mustBe JsSuccess(response)
    }
  }
}
