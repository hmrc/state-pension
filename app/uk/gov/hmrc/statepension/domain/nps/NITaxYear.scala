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

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, _}

import scala.util.Try

case class NITaxYear(startTaxYear: Option[Int], qualifying: Option[Boolean], underInvestigation: Option[Boolean], payableFlag: Option[Boolean]) {
  lazy val payable: Boolean = payableFlag.get && !qualifying.get && !underInvestigation.get //payableFlag from NPS Response is incorrect
}

object NITaxYear {
  val readBooleanFromInt: JsPath => Reads[Boolean] = jsPath => jsPath.read[Int].map(_.equals(1))

  val readNullableIntFromString: JsPath => Reads[Option[Int]] =
    jsPath => jsPath.readNullable[String].map(s => s.map(st => Try(st.toInt).toOption.getOrElse(throw new Exception(s"${jsPath.path.head} is not a valid integer"))))

  implicit val reads: Reads[NITaxYear] = (
    readNullableIntFromString(__ \ "rattdTaxYear") and
      (__ \ "qualifying").readNullable[Boolean] and
      (__ \ "underInvestigationFlag").readNullable[Boolean] and
      (__ \ "payable").readNullable[Boolean]
    ) (NITaxYear.apply)
}
