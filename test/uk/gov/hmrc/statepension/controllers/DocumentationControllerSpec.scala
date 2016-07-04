/*
 * Copyright 2016 HM Revenue & Customs
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

import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class DocumentationControllerSpec extends UnitSpec with WithFakeApplication {

  "respond to GET /api/definition" in {
    val result = route(FakeRequest(GET, "/api/definition"))
    status(result.get) shouldNot be(NOT_FOUND)
  }

  "respond to GET /api/documentation/1.0/hello-world" in {
    val result = route(FakeRequest(GET, "/api/documentation/1.0/hello-world"))
    status(result.get) shouldNot be(NOT_FOUND)
  }
}
