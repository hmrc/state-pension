/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.mvc.{BodyParsers, ControllerComponents}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.controllers.ErrorHandling
import uk.gov.hmrc.statepension.controllers.auth.AuthAction
import uk.gov.hmrc.statepension.services.CheckPensionService

import scala.concurrent.ExecutionContext

class CheckPensionController @Inject()(
                                        override val authAction: AuthAction,
                                        override val appConfig: AppConfig,
                                        override val statePensionService: CheckPensionService,
                                        override val customAuditConnector: AuditConnector,
                                        override val controllerComponents: ControllerComponents,
                                        val parser: BodyParsers.Default,
                                        val executionContext: ExecutionContext,
                                        errorHandling: ErrorHandling
                                      )
  extends StatePensionController(controllerComponents, errorHandling) {
  override def endpointUrl(nino: Nino): String =
    uk.gov.hmrc.statepension.controllers.statepension.routes.CheckPensionController.get(nino).url
}
