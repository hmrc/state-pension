/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.statepension.config.AppContext
import uk.gov.hmrc.statepension.controllers.auth.AuthAction
import uk.gov.hmrc.statepension.services.CheckPensionService

class CheckPensionController @Inject()(
                                        override val authAction: AuthAction,
                                        override val appContext: AppContext,
                                        override val statePensionService: CheckPensionService,
                                        override val customAuditConnector: AuditConnector
                                      ) extends StatePensionController {
  override def endpointUrl(nino: Nino): String =
    uk.gov.hmrc.statepension.controllers.statepension.routes.CheckPensionController.get(nino).url
}
