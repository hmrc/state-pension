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
import play.api.Logging
import play.api.libs.json.Reads
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpReadsInstances.readEitherOf
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.domain.nps._
import uk.gov.hmrc.statepension.services.ApplicationMetrics

import java.util.UUID.randomUUID
import scala.concurrent.{ExecutionContext, Future}

abstract class NpsConnector @Inject()(
  appConfig: AppConfig,
  connectorUtil: ConnectorUtil
)(
  implicit ec: ExecutionContext
) extends Logging {

  val http: HttpClientV2
  val metrics: ApplicationMetrics
  val token: String
  val originatorIdKey: String
  val originatorIdValue: String
  val environmentHeader: (String, String)
  def summaryUrl(nino: Nino): String
  def liabilitiesUrl(nino: Nino): String
  def niRecordUrl(nino: Nino): String

  val summaryMetricType: APIType
  val liabilitiesMetricType: APIType
  val niRecordMetricType: APIType

  private val serviceOriginatorId: String  => (String, String) = (originatorIdKey, _)

  def getSummary(nino: Nino)(implicit headerCarrier: HeaderCarrier): Future[Summary] =
    connectToHOD[Summary](
      url          = summaryUrl(nino),
      api          = summaryMetricType,
      originatorId = serviceOriginatorId(setServiceOriginatorId(originatorIdValue))
    ) flatMap {
      case Right(summary) =>
        Future.successful(summary)
      case Left(error) =>
        Future.failed(error)
    }

  def getLiabilities(nino: Nino)(implicit headerCarrier: HeaderCarrier): Future[List[Liability]] =
    connectToHOD[Liabilities](
      url          = liabilitiesUrl(nino),
      api          = liabilitiesMetricType,
      originatorId = serviceOriginatorId(originatorIdValue)
    ) flatMap {
      case Right(liabilities) =>
        Future.successful(liabilities.liabilities)
      case Left(error) =>
        Future.failed(error)
    }

  def getNIRecord(nino: Nino)(implicit headerCarrier: HeaderCarrier): Future[NIRecord] =
    connectToHOD[NIRecord](
      url          = niRecordUrl(nino),
      api          = niRecordMetricType,
      originatorId = serviceOriginatorId(originatorIdValue)
    ) flatMap {
      case Right(niRecord) =>
        Future.successful(niRecord)
      case Left(error) =>
        Future.failed(error)
    }

  private def connectToHOD[A](
    url: String,
    api: APIType,
    originatorId: (String, String)
  )(
    implicit headerCarrier: HeaderCarrier,
    reads: Reads[A]
  ): Future[Either[Exception, A]] = {
    val timerContext = metrics.startTimer(api)

    connectorUtil.handleConnectorResponse(
      http
        .get(url"$url")
        .setHeader(HeaderNames.authorisation -> s"Bearer $token")
        .setHeader("CorrelationId" -> randomUUID().toString)
        .setHeader(environmentHeader)
        .setHeader(originatorId)
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
            metrics.incrementFailedCounter(api)
          case Right(_) =>
            ()
        }
        result
    }
  }

  private def getHeaderValueByKey(key: String)(implicit headerCarrier: HeaderCarrier): String =
    headerCarrier.headers(Seq(key)).toMap.getOrElse(key, "Header not found")

  private def setServiceOriginatorId(value: String)(implicit headerCarrier: HeaderCarrier): String = {
    appConfig.dwpApplicationId match {
      case Some(appIds) if appIds contains getHeaderValueByKey("x-application-id") =>
        appConfig.dwpOriginatorId
      case _ =>
        value
    }
  }
}

