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

package uk.gov.hmrc.statepension.config

import play.api.mvc.PathBindable
import uk.gov.hmrc.statepension.StatePensionBaseSpec

class BindersSpec extends StatePensionBaseSpec {

  "nino.bind" should {

    "return Right with a NINO instance for a valid NINO string" in {
      val nino = generateNino()
      implicit val pathBindable = PathBindable.bindableString

      val result = Binders.ninoBinder.bind("nino", nino.nino)
      result shouldEqual Right(nino)
    }

    "return Left for an invalid NINO string" in {
      val nino = "invalid"
      implicit val pathBindable = PathBindable.bindableString

      val result = Binders.ninoBinder.bind("nino", nino)
      result shouldEqual Left("ERROR_NINO_INVALID")
    }
  }
}
