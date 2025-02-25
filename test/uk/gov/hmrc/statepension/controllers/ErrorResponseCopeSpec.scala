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


import org.scalatest.matchers.must.Matchers

import java.time.LocalDate
import play.api.libs.json.{JsSuccess, Json}
import utils.StatePensionBaseSpec
import org.scalatest.matchers.must.Matchers.mustBe


class ErrorResponseCopeSpec extends StatePensionBaseSpec {

  "ErrorResponseCopeProcessing" must {
    "serialize and deserialize correctly" in {
      val errorResponse = ErrorResponseCopeProcessing(
        code = "COPE_PROCESSING",
        copeDataAvailableDate = LocalDate.of(2025, 1, 1),
        previousAvailableDate = Some(LocalDate.of(2024, 6, 1))
      )

      val json = Json.toJson(errorResponse)
      val parsed = json.validate[ErrorResponseCopeProcessing]

      parsed mustBe JsSuccess(errorResponse)
    }
  }

  "ErrorResponseCopeFailed" must {
    "serialize and deserialize correctly" in {
      val errorResponse = ErrorResponseCopeFailed(
        code = "COPE_FAILED"
      )

      val json = Json.toJson(errorResponse)
      val parsed = json.validate[ErrorResponseCopeFailed]

      parsed mustBe JsSuccess(errorResponse)
    }
  }
}
