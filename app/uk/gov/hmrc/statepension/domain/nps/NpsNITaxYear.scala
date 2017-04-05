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

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, _}

case class NpsNITaxYear(startTaxYear: Int, qualifying: Boolean, underInvestigation: Boolean, payableFlag: Boolean) {
  lazy val payable: Boolean = payableFlag && !qualifying && !underInvestigation //payableFlag from NPS Response is incorrect
}

object NpsNITaxYear {
  val readBooleanFromInt: JsPath => Reads[Boolean] = jsPath => jsPath.read[Int].map(_.equals(1))

  implicit val reads: Reads[NpsNITaxYear] = (
    (__ \ "rattd_tax_year").read[Int] and
      readBooleanFromInt(__ \ "qualifying") and
      readBooleanFromInt(__ \ "under_investigation_flag") and
      readBooleanFromInt(__ \ "payable")
    ) (NpsNITaxYear.apply _)
}
