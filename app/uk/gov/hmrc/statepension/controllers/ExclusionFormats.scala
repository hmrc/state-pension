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

package uk.gov.hmrc.statepension.controllers

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.api.controllers.{ErrorGenericBadRequest, ErrorInternalServerError}

object ExclusionFormats {
  implicit val errorGenericBadRequestFormat: OFormat[ErrorGenericBadRequest] = Json.format[ErrorGenericBadRequest]
//  implicit val errorNotFoundFormat: OFormat[ErrorNotFound.type] = Json.format[uk.gov.hmrc.api.controllers.ErrorNotFound.type]
  implicit val errorInternalServerErrorFormat: OFormat[ErrorInternalServerError.type] = Json.format[uk.gov.hmrc.api.controllers.ErrorInternalServerError.type]
//  implicit val errorCopeProcessingFormat: OFormat[ErrorResponses.ExclusionCopeProcessing.type] = Json.format[ErrorResponses.ExclusionCopeProcessing.type]
//  implicit val errorCopeProcessingFailedFormat: OFormat[ErrorResponses.ExclusionCopeProcessingFailed.type] = Json.format[ErrorResponses.ExclusionCopeProcessingFailed.type]
//  implicit val exclusionDeadFormat: OFormat[ErrorResponses.ExclusionDead.type] = Json.format[ErrorResponses.ExclusionDead.type]
//  implicit val exclusionManualCorrespondenceFormat: OFormat[ErrorResponses.ExclusionManualCorrespondence.type] = Json.format[ErrorResponses.ExclusionManualCorrespondence.type]
}
