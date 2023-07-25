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
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.domain.nps.APIType.{Liabilities, NIRecord, Summary}
import uk.gov.hmrc.statepension.domain.nps._
import uk.gov.hmrc.statepension.models.ProxyCacheToggle
import uk.gov.hmrc.statepension.services.ApplicationMetrics

import scala.concurrent.{ExecutionContext, Future}

class DesConnector @Inject()(
  val http: HttpClient,
  val metrics: ApplicationMetrics,
  appConfig: AppConfig,
  featureFlagService: FeatureFlagService
)(
  implicit ec: ExecutionContext
) extends NpsConnector(appConfig) {

  import appConfig.desConnectorConfig._

  private val desBaseUrl: String = serviceUrl
  override val originatorIdKey: String = serviceOriginatorIdKey
  override val originatorIdValue: String =  serviceOriginatorIdValue
  override val environmentHeader: (String, String) = ("Environment", environment)
  override val token: Future[String] =
    featureFlagService.get(ProxyCacheToggle) map {
      proxyCache =>
        if (proxyCache.isEnabled) appConfig.internalAuthToken
        else authorizationToken
    }

  override val summaryMetricType: APIType = Summary
  override val liabilitiesMetricType: APIType = Liabilities
  override val niRecordMetricType: APIType = NIRecord

  private def url(nino: Nino, path: String): Future[String] =
    featureFlagService.get(ProxyCacheToggle) map {
      proxyCache =>
        if (proxyCache.isEnabled) {
          s"${appConfig.proxyCacheUrl}/ni-and-sp-proxy-cache/$nino/$path"
        } else {
          s"$desBaseUrl/individuals/${nino.withoutSuffix}/pensions/$path"
        }
    }

  override def summaryUrl(nino: Nino): Future[String] =
    url(nino, "summary")
  override def liabilitiesUrl(nino: Nino): Future[String] =
    url(nino, "liabilities")
  override def niRecordUrl(nino: Nino): Future[String] =
    url(nino, "ni")

}
