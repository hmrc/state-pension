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
import play.api.mvc.{BodyParsers, ControllerComponents}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.controllers.ErrorHandling
import uk.gov.hmrc.statepension.controllers.auth.PrivilegedAuthAction
import uk.gov.hmrc.statepension.repositories.CopeProcessingRepository
import uk.gov.hmrc.statepension.services.DashboardService

import scala.concurrent.ExecutionContext

class DashboardController @Inject()(
                                     override val authAction: PrivilegedAuthAction,
                                     override val appConfig: AppConfig,
                                     override val statePensionService: DashboardService,
                                     override val customAuditConnector: AuditConnector,
                                     override val controllerComponents: ControllerComponents,
                                     errorHandling: ErrorHandling,
                                     copeProcessingRepository: CopeProcessingRepository,
                                     val parser: BodyParsers.Default,
                                     val executionContext: ExecutionContext
                                   )(override implicit val ec: ExecutionContext)
  extends StatePensionController(controllerComponents, errorHandling, copeProcessingRepository) {
  override def endpointUrl(nino: Nino): String =
    uk.gov.hmrc.statepension.controllers.statepension.routes.DashboardController.get(nino).url
}
