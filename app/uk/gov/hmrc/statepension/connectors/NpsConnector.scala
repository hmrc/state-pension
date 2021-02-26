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

package uk.gov.hmrc.statepension.connectors

import java.util.UUID.randomUUID

import com.google.inject.Inject
import play.api.libs.json.{JsPath, JsonValidationError, Reads}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.domain.nps._
import uk.gov.hmrc.statepension.services.ApplicationMetrics

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

abstract class NpsConnector @Inject()(appConfig: AppConfig)(implicit ec: ExecutionContext){

  val http: HttpClient
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

  val serviceOriginatorId: String  => (String, String) = (originatorIdKey, _)

  def getSummary(nino: Nino)(implicit headerCarrier: HeaderCarrier): Future[Summary] =
    connectToHOD[Summary](summaryUrl(nino), summaryMetricType)

  def getLiabilities(nino: Nino)(implicit headerCarrier: HeaderCarrier): Future[List[Liability]] =
    connectToHOD[Liabilities](liabilitiesUrl(nino), liabilitiesMetricType).map(_.liabilities)

  def getNIRecord(nino: Nino)(implicit headerCarrier: HeaderCarrier): Future[NIRecord] =
    connectToHOD[NIRecord](niRecordUrl(nino), niRecordMetricType)

  private def connectToHOD[A](url: String, api: APIType)(implicit headerCarrier: HeaderCarrier, reads: Reads[A]): Future[A] = {
    val timerContext = metrics.startTimer(api)
    val correlationId: (String, String) = "CorrelationId" -> randomUUID().toString
    val responseF = http.GET[HttpResponse](url)(HttpReads.readRaw,
      HeaderCarrier(Some(Authorization(s"Bearer $token")), sessionId = headerCarrier.sessionId, requestId = headerCarrier.requestId)
      .withExtraHeaders(serviceOriginatorId(setServiceOriginatorId(headerCarrier, originatorIdValue)), environmentHeader, correlationId), ec)

    responseF.map { httpResponse =>
      timerContext.stop()
      Try(httpResponse.json.validate[A]).flatMap( jsResult =>
        jsResult.fold(errs => Failure(new JsonValidationException(formatJsonErrors(errs))), valid => Success(valid))
      )
    } recover {
      // http-verbs throws exceptions, convert to Try
      case ex => Failure(ex)
    } flatMap (handleResult(api, _))
  }

  private def handleResult[A](api: APIType, tryResult: Try[A]): Future[A] = {
    tryResult match {
      case Failure(ex) =>
        metrics.incrementFailedCounter(api)
        Future.failed(ex)
      case Success(value) =>
        Future.successful(value)
    }
  }

  private def setServiceOriginatorId(headerCarrier: HeaderCarrier, value: String): String = {
    val appIdHeader: Seq[(String, String)] = headerCarrier.headers.filter(header => header._1 == "x-application-id")
    if (appIdHeader.head._2 == appConfig.dwpApplicationId) "DA_PFDWP"
    else value
  }

  private def formatJsonErrors(errors: Seq[(JsPath, Seq[JsonValidationError])]): String = {
    errors.map(p => p._1 + " - " + p._2.map(_.message).mkString(",")).mkString(" | ")
  }

  class JsonValidationException(message: String) extends Exception(message)
}

