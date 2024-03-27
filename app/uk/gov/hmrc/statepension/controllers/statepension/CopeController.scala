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

package uk.gov.hmrc.statepension.controllers.statepension

import com.google.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.statepension.services.CopeService

import scala.concurrent.ExecutionContext

class CopeController @Inject()(
                                copeService: CopeService,
                                authAction: AuthAction,
                                cc: ControllerComponents)(implicit val executionContext: ExecutionContext)
  extends BackendController(cc) {

  def get(nino: Nino): Action[AnyContent] = authAction.async {
    copeService.getCopeCase(nino) map {
      case Some(copeprocessing: ErrorResponseCopeProcessing) => Forbidden(Json.toJson(copeprocessing))
      case Some(copeFailed: ErrorResponseCopeFailed) => Forbidden(Json.toJson(copeFailed))
      case _ => NotFound("User is not a cope case")
    }
  }
}
