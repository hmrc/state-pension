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

package uk.gov.hmrc.statepension.controllers.sandbox

import play.api.Logger
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.AuditEvent
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.statepension.config.AppContext
import uk.gov.hmrc.statepension.connectors.CustomAuditConnector
import uk.gov.hmrc.statepension.controllers.StatePensionController
import uk.gov.hmrc.statepension.services.{SandboxStatePensionService, StatePensionService}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._



object StatePensionController extends StatePensionController {
  override val statePensionService: StatePensionService = SandboxStatePensionService
  override val app: String = "Sandbox-State-Pension"
  override val context: String = AppContext.apiGatewayContext
  override val customAuditConnector: CustomAuditConnector = new CustomAuditConnector {
    override lazy val auditConnector: AuditConnector = ???
    override def sendEvent(event: AuditEvent)(implicit hc: HeaderCarrier): Unit = Logger.info(s"Sandbox Audit event sent: ${event.auditType}")
  }
}
