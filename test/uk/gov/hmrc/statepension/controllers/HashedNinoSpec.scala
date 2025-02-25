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

package uk.gov.hmrc.statepension.controllers

import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.domain.Nino
import org.scalatest.matchers.must.Matchers.mustBe

class HashedNinoSpec extends AnyWordSpec {

  "HashedNino" must {
    "serialize and deserialize correctly" in {
      val hashedNino = HashedNino(Nino("AA123456A"))

      val json = Json.toJson(hashedNino)
      val parsed = json.validate[HashedNino]

      parsed mustBe JsSuccess(hashedNino)
    }
  }
}
