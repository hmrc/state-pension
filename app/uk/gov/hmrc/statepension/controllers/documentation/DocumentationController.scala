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

package uk.gov.hmrc.statepension.controllers.documentation

import com.google.inject.Inject
import controllers.Assets
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.statepension.config.{APIAccessConfig, AppConfig}
import uk.gov.hmrc.statepension.domain.APIAccess
import uk.gov.hmrc.statepension.views._

class DocumentationController @Inject()(appContext: AppConfig,
                                        controllerComponents: ControllerComponents,
                                        assets: Assets) extends BackendController(controllerComponents){

  def definition(): Action[AnyContent] = Action {
    Ok(txt.definition(buildAccess(), buildStatus())).as("application/json")
  }

  def conf(version: String, file: String): Action[AnyContent] = {
    assets.at(s"/public/api/conf/$version", file)
  }

  private def buildAccess(): APIAccess = {
    val access = APIAccessConfig(appContext.access)
    APIAccess(access.accessType)
  }

  private def buildStatus(): String = appContext.status.getOrElse("BETA")
}
