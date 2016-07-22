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

import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.statepension.services.StatePensionService

trait StatePensionController extends BaseController with HeaderValidator with ErrorHandling with HalSupport with Links {

  val statePensionService: StatePensionService
	def get(nino: Nino): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      errorWrapper(statePensionService.getStatement(nino).map {
        case Left(exclusion) => Ok(halResourceSelfLink(Json.toJson(exclusion), statePensionHref(nino)))
        case Right(statePension) => Ok(halResourceSelfLink(Json.toJson(statePension), statePensionHref(nino)))
      })
  }
}

