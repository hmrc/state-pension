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

package uk.gov.hmrc.statepension.controllers

import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.statepension.config.{APIAccessConfig, AppContext}
import uk.gov.hmrc.statepension.domain.APIAccess
import uk.gov.hmrc.statepension.views._

trait DocumentationController extends uk.gov.hmrc.api.controllers.DocumentationController {

  val appContext: AppContext

  override def definition(): Action[AnyContent] = Action {
    Ok(txt.definition(buildAccess(), buildStatus())).withHeaders("Content-Type" -> "application/json")
  }

  override def documentation(version: String, file: String): Action[AnyContent] = {
    super.at(s"/public/api/conf/$version", file)
  }

  private def buildAccess(): APIAccess = {
    val access = APIAccessConfig(appContext.access)
    APIAccess(access.accessType, access.whiteListedApplicationIds)
  }

  private def buildStatus(): String = appContext.status.getOrElse("PROTOTYPED")
}

object DocumentationController extends DocumentationController {
  override val appContext: AppContext = AppContext
}
