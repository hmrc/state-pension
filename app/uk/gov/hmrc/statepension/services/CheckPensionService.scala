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

package uk.gov.hmrc.statepension.services

import com.google.inject.Inject
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.statepension.connectors.DesConnector
import uk.gov.hmrc.statepension.domain.nps.Summary

import scala.concurrent.{ExecutionContext, Future}

class CheckPensionService @Inject()(
                                     citizenDetailsService: CitizenDetailsService,
                                     override val nps: DesConnector,
                                     override val forecastingService: ForecastingService,
                                     override val rateService: RateService,
                                     override val metrics: ApplicationMetrics,
                                     override val customAuditConnector: AuditConnector,
                                     override val executionContext: ExecutionContext
                                   ) extends StatePensionService {
  override def getMCI(summary: Summary, nino: Nino)(implicit hc: HeaderCarrier): Future[Boolean] =
    citizenDetailsService.checkManualCorrespondenceIndicator(nino)
}
