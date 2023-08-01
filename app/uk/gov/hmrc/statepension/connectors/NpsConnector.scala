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
import play.api.libs.json.{JsPath, JsonValidationError, Reads}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpReadsInstances.readEitherOf
import uk.gov.hmrc.http._
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.domain.nps._
import uk.gov.hmrc.statepension.models.ProxyCacheToggle
import uk.gov.hmrc.statepension.services.ApplicationMetrics

import java.util.UUID.randomUUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

abstract class NpsConnector @Inject()(
  appConfig: AppConfig,
  featureFlagService: FeatureFlagService
)(
  implicit ec: ExecutionContext
) extends Logging {

  val http: HttpClient
  val metrics: ApplicationMetrics
  val token: String
  val originatorIdKey: String
  val originatorIdValue: String
  val environmentHeader: (String, String)
  def summaryUrl(nino: Nino): Future[String]
  def liabilitiesUrl(nino: Nino): Future[String]
  def niRecordUrl(nino: Nino): Future[String]

  val summaryMetricType: APIType
  val liabilitiesMetricType: APIType
  val niRecordMetricType: APIType

  private val serviceOriginatorId: String  => (String, String) = (originatorIdKey, _)

  def getSummary(nino: Nino)(implicit headerCarrier: HeaderCarrier): Future[Summary] =
    summaryUrl(nino) flatMap {
      url =>

        connectToHOD[Summary](
          url          = url,
          api          = summaryMetricType,
          originatorId = serviceOriginatorId(setServiceOriginatorId(originatorIdValue))
        )
    }

  def getLiabilities(nino: Nino): Future[List[Liability]] =
    liabilitiesUrl(nino) flatMap {
      url =>
        connectToHOD[Liabilities](
          url          = url,
          api          = liabilitiesMetricType,
          originatorId = serviceOriginatorId(originatorIdValue)
        ).map(_.liabilities)
    }

  def getNIRecord(nino: Nino): Future[NIRecord] =
    niRecordUrl(nino) flatMap {
      url =>
        connectToHOD[NIRecord](
          url          = url,
          api          = niRecordMetricType,
          originatorId = serviceOriginatorId(originatorIdValue)
        )
    }

  private def connectToHOD[A](
    url: String,
    api: APIType,
    originatorId: (String, String)
  )(
    implicit reads: Reads[A]
  ): Future[A] = {
    val timerContext = metrics.startTimer(api)

    featureFlagService.get(ProxyCacheToggle) flatMap{
      proxyCache =>
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val authToken = if (proxyCache.isEnabled) appConfig.internalAuthToken
          else s"Bearer $token"

        val headers = Seq(
          HeaderNames.authorisation -> authToken,
          "CorrelationId" -> randomUUID().toString,
          environmentHeader,
          originatorId
        )

        http
          .GET[Either[UpstreamErrorResponse, HttpResponse]](url, Seq(), headers)
          .transform {
            result =>
              timerContext.stop()
              result
          }.map {
          case Right(httpResponse) =>
            Try(httpResponse.json.validate[A]).flatMap {
              _.fold(
                errs =>
                  Failure(new JsonValidationException(formatJsonErrors(errs.asInstanceOf[scala.collection.immutable.Seq[(JsPath, scala.collection.immutable.Seq[JsonValidationError])]]))),
                valid =>
                  Success(valid)
              )
            }
          case Left(error) =>
            Failure(error)
        } recover {
          // http-verbs throws exceptions, convert to Try
          case ex =>
            Failure(ex)
        } flatMap(handleResult(api, _))
    }
  }

  private def handleResult[A](api: APIType, tryResult: Try[A]): Future[A] =
    tryResult match {
      case Failure(ex) =>
        metrics.incrementFailedCounter(api)
        Future.failed(ex)
      case Success(value) =>
        Future.successful(value)
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

  private def formatJsonErrors(errors: scala.collection.immutable.Seq[(JsPath, scala.collection.immutable.Seq[JsonValidationError])]): String =
    errors
      .map(p => s"${p._1.toString()} - ${p._2.map(_.message).mkString(",")}")
      .mkString(" | ")

  class JsonValidationException(message: String) extends Exception(message)
}

