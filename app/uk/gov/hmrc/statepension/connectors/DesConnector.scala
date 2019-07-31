/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.Mode.Mode
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, Reads}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.statepension.WSHttp
import uk.gov.hmrc.statepension.domain.nps._
import uk.gov.hmrc.statepension.services.ApplicationMetrics

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class DesConnector @Inject()(http: WSHttp,
                             metrics: ApplicationMetrics,
                             environment: Environment,
                             val runModeConfiguration: Configuration) extends ServicesConfig {

  val desBaseUrl: String = baseUrl("des-hod")
  val serviceOriginatorId: (String, String) = (getConfString("des-hod.originatoridkey", ""), getConfString("des-hod.originatoridvalue", ""))
  val desEnvironment: (String, String) = ("Environment", getConfString("des-hod.environment", ""))
  val token: String = getConfString("des-hod.token", "")

  protected def mode: Mode = environment.mode

  def getSummary(nino: Nino)(implicit headerCarrier: HeaderCarrier): Future[DesSummary] = {
    val urlToRead = s"$desBaseUrl/individuals/${ninoWithoutSuffix(nino)}/pensions/summary"
    connectToDES[DesSummary](urlToRead, APIType.Summary)
  }

  def getLiabilities(nino: Nino)(implicit headerCarrier: HeaderCarrier): Future[List[DesLiability]] = {
    val urlToRead = s"$desBaseUrl/individuals/${ninoWithoutSuffix(nino)}/pensions/liabilities"
    connectToDES[DesLiabilities](urlToRead, APIType.Liabilities).map(_.liabilities)
  }

  def getNIRecord(nino: Nino)(implicit headerCarrier: HeaderCarrier): Future[DesNIRecord] = {
    val urlToRead = s"$desBaseUrl/individuals/${ninoWithoutSuffix(nino)}/pensions/ni"
    connectToDES[DesNIRecord](urlToRead, APIType.NIRecord)
  }

  private def connectToDES[A](url: String, api: APIType)(implicit headerCarrier: HeaderCarrier, reads: Reads[A]): Future[A] = {
    val timerContext = metrics.startTimer(api)
    val responseF = http.GET[HttpResponse](url)(HttpReads.readRaw, HeaderCarrier(Some(Authorization(s"Bearer $token"))).withExtraHeaders(serviceOriginatorId, desEnvironment),  ec=global)

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

