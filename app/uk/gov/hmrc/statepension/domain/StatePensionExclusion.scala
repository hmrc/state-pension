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

package uk.gov.hmrc.statepension.domain


import java.time.LocalDate
import play.api.libs.json._

trait Exclusion

object Exclusion {

  case object IsleOfMan extends Exclusion
  case object Dead extends Exclusion
  case object AmountDissonance extends Exclusion
  case object PostStatePensionAge extends Exclusion
  case object ManualCorrespondenceIndicator extends Exclusion

  implicit object ExclusionFormat extends Format[Exclusion] {
    override def reads(json: JsValue): JsResult[Exclusion] =
      json match {
        case JsString("Dead") => JsSuccess(Exclusion.Dead)
        case JsString("AmountDissonance") => JsSuccess(Exclusion.AmountDissonance)
        case JsString("IsleOfMan") => JsSuccess(Exclusion.IsleOfMan)
        case JsString("PostStatePensionAge") => JsSuccess(Exclusion.PostStatePensionAge)
        case JsString("ManualCorrespondenceIndicator") => JsSuccess(Exclusion.ManualCorrespondenceIndicator)
        case _ => JsError("Exclusion not valid!")
      }

    override def writes(ex: Exclusion): JsValue = JsString(ex.toString)
  }

}


case class StatePensionExclusion(exclusionReasons: List[Exclusion],
                                 pensionAge: Int,
                                 pensionDate: LocalDate,
                                 statePensionAgeUnderConsideration: Boolean)

object StatePensionExclusion {
  implicit val formats: OFormat[StatePensionExclusion] = Json.format[StatePensionExclusion]
}
