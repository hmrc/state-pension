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

package uk.gov.hmrc.statepension.connectors
import com.google.inject.Inject
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.domain.nps.APIType
import uk.gov.hmrc.statepension.domain.nps.APIType.{IfLiabilities, IfNIRecord, IfSummary}
import uk.gov.hmrc.statepension.services.ApplicationMetrics

import scala.concurrent.ExecutionContext

class IfConnector @Inject()(
                             val http: HttpClient,
                             val metrics: ApplicationMetrics,
                             appConfig: AppConfig
                           )(implicit ec: ExecutionContext) extends NpsConnector(appConfig) {

  import appConfig.ifConnectorConfig._

  val ifBaseUrl: String = serviceUrl
  override val originatorIdKey: String = serviceOriginatorIdKey
  override val originatorIdValue: String =  serviceOriginatorIdValue
  override val environmentHeader: (String, String) = ("Environment", environment)
  override val token: String = authorizationToken

  override def summaryUrl(nino: Nino): String = s"$ifBaseUrl/individuals/state-pensions/nino/${nino.withoutSuffix}/summary"
  override def liabilitiesUrl(nino: Nino): String =  s"$ifBaseUrl/individuals/state-pensions/nino/${nino.withoutSuffix}/liabilities"
  override def niRecordUrl(nino: Nino): String = s"$ifBaseUrl/individuals/state-pensions/nino/${nino.withoutSuffix}/ni-details"

  override val summaryMetricType: APIType = IfSummary
  override val liabilitiesMetricType: APIType = IfLiabilities
  override val niRecordMetricType: APIType = IfNIRecord
}
