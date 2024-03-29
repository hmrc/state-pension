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

import play.api.Logging
import play.api.libs.json._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.domain.nps.{APIType, ProxyCacheData}
import uk.gov.hmrc.statepension.services.ApplicationMetrics

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProxyCacheConnector @Inject ()(
  httpClient: HttpClientV2,
  metrics: ApplicationMetrics,
  appConfig: AppConfig,
  connectorUtil: ConnectorUtil
)(
  implicit val executionContext: ExecutionContext
) extends Logging {

  import appConfig.desConnectorConfig._

  def get(
    nino: Nino
  )(
    implicit headerCarrier: HeaderCarrier
  ): Future[ProxyCacheData] =
    connect(nino).flatMap {
      case Right(proxyCacheData) =>
        Future.successful(proxyCacheData)
      case Left(error) =>
        Future.failed(error)
    }

  private def connect(
    nino: Nino
  )(
    implicit headerCarrier: HeaderCarrier,
    reads: Reads[ProxyCacheData]
  ): Future[Either[Exception, ProxyCacheData]] = {
    val timerContext = metrics.startTimer(APIType.ProxyCache)

    connectorUtil.handleConnectorResponse(
      futureResponse = httpClient
        .get(url"${appConfig.proxyCacheUrl}/ni-and-sp-proxy-cache/$nino")
        .setHeader(HeaderNames.authorisation -> appConfig.internalAuthToken)
        .setHeader("Originator-Id"           -> "DA_PF")
        .setHeader("Environment"             -> environment)
        .setHeader("CorrelationId"           -> UUID.randomUUID().toString)
        .setHeader(HeaderNames.xRequestId    -> headerCarrier.requestId.fold("-")(_.value))
        .execute[Either[UpstreamErrorResponse, HttpResponse]]
        .transform {
          result =>
            timerContext.stop()
            result
        }
    ) map {
      result =>
        result match {
          case Left(_) =>
            metrics.incrementFailedCounter(APIType.ProxyCache)
          case Right(_) =>
            ()
        }
        result
    }
  }
}
