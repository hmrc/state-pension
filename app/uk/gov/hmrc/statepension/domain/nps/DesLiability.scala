/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.libs.json._

case class DesLiability(liabilityType: Int)

object DesLiability {

  val readNullableInt: JsPath => Reads[Int] =
    jsPath => jsPath.readNullable[Int].map(_.getOrElse(0))

  implicit val reads: Reads[DesLiability] = readNullableInt(__ \ "liabilityType").map(DesLiability.apply)

//  implicit val reads: Reads[DesLiability] = (__ \ "liabilityType").read[Int].map(DesLiability.apply)
}

case class DesLiabilities(liabilities: List[DesLiability])

object DesLiabilities {

  val readNullableList:JsPath => Reads[List[DesLiability]] =
    jsPath => jsPath.readNullable[List[DesLiability]].map(_.getOrElse(List.empty))

  implicit val reads: Reads[DesLiabilities] =
    readNullableList(__ \ "liabilities").map(DesLiabilities.apply)

//  implicit val reads: Reads[DesLiabilities] = {
//    (__ \ "liabilities").read[List[DesLiability]].map(DesLiabilities.apply)
  //}
}
