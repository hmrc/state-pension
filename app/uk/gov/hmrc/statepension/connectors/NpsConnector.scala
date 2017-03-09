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

package uk.gov.hmrc.statepension.connectors

import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, Reads}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpReads, HttpResponse}
import uk.gov.hmrc.statepension.WSHttp
import uk.gov.hmrc.statepension.domain.nps._
import uk.gov.hmrc.statepension.services.Metrics

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait NpsConnector {
  def http: HttpGet
  def npsBaseUrl: String
  def serviceOriginatorId: (String, String)
  def metrics: Metrics

  def getSummary(nino: Nino)(implicit headerCarrier: HeaderCarrier): Future[NpsSummary] = {
    val urlToRead = s"$npsBaseUrl/nps-rest-service/services/nps/pensions/${ninoWithoutSuffix(nino)}/sp_summary"
    connectToNPS[NpsSummary](urlToRead, APIType.Summary)
  }

  def getLiabilities(nino: Nino)(implicit headerCarrier: HeaderCarrier): Future[List[NpsLiability]] = {
    val urlToRead = s"$npsBaseUrl/nps-rest-service/services/nps/pensions/${ninoWithoutSuffix(nino)}/liabilities"
    connectToNPS[NpsLiabilities](urlToRead, APIType.Liabilities).map(_.liabilities)
  }

  def getNIRecord(nino: Nino)(implicit headerCarrier: HeaderCarrier): Future[NpsNIRecord] = {
    val urlToRead = s"$npsBaseUrl/nps-rest-service/services/nps/pensions/${ninoWithoutSuffix(nino)}/ni_record"
    connectToNPS[NpsNIRecord](urlToRead, APIType.NIRecord)
  }

  private def connectToNPS[A](url: String, api: APIType)(implicit headerCarrier: HeaderCarrier, reads: Reads[A]): Future[A] = {
    val timerContext = metrics.startTimer(api)
    val responseF = http.GET[HttpResponse](url)(HttpReads.readRaw, headerCarrier.withExtraHeaders(serviceOriginatorId))

    responseF.map { httpResponse =>
      timerContext.stop()
      Try(httpResponse.json.validate[A]).flatMap( jsResult =>
        jsResult.fold(errs => Failure(new JsonValidationException(formatJsonErrors(errs))), valid => Success(valid))
      )
    } recover {
      // http-verbs throws exceptions, convert to Try
      case ex => Failure(ex)
    } flatMap (handleResult(api, url, _))
  }

  private final val ninoLengthWithoutSuffix = 8

  private def ninoWithoutSuffix(nino: Nino): String = nino.toString().take(ninoLengthWithoutSuffix)

  private def handleResult[A](api: APIType, url: String, tryResult: Try[A]): Future[A] = {
    tryResult match {
      case Failure(ex) =>
        metrics.incrementFailedCounter(api)
        Future.failed(ex)
      case Success(value) =>
        Future.successful(value)
    }
  }

  private def formatJsonErrors(errors: Seq[(JsPath, Seq[ValidationError])]): String = {
    errors.map(p => p._1 + " - " + p._2.map(_.message).mkString(",")).mkString(" | ")
  }

  class JsonValidationException(message: String) extends Exception(message)
}

object NpsConnector extends NpsConnector with ServicesConfig {
  override val http: HttpGet = WSHttp
  override val npsBaseUrl: String = baseUrl("nps-hod")
  override val serviceOriginatorId: (String, String) = (getConfString("nps-hod.originatoridkey", ""), getConfString("nps-hod.originatoridvalue", ""))
  override val metrics: Metrics = Metrics
}
