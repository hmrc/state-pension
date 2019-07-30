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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import play.api.Mode.Mode
import play.api.{Configuration, Play}
import uk.gov.hmrc.domain.Nino
import play.api.http.Status.LOCKED
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.statepension.WSHttp
import uk.gov.hmrc.statepension.domain.nps.APIType
import uk.gov.hmrc.statepension.services.ApplicationMetrics

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpResponse, Upstream4xxResponse}

trait CitizenDetailsConnector {
  def http: HttpGet
  def serviceUrl: String
  def metrics: ApplicationMetrics

  private def url(nino: Nino) = s"$serviceUrl/citizen-details/$nino/designatory-details/"

  def connectToGetPersonDetails(nino: Nino)(implicit hc: HeaderCarrier): Future[Int] = {
    val timerContext = metrics.startTimer(APIType.CitizenDetails)
    http.GET[HttpResponse](url(nino)) map {
      personResponse =>
        timerContext.stop()
        Success(personResponse.status)
    } recover {
      case ex: Upstream4xxResponse if ex.upstreamResponseCode == LOCKED => timerContext.stop(); Success(ex.upstreamResponseCode)
      case ex: Throwable => timerContext.stop(); Failure(ex)
    } flatMap (handleResult(url(nino), _))
  }

  private def handleResult[A](url: String, tryResult: Try[A]): Future[A] = {
    tryResult match {
      case Failure(ex) =>
        Future.failed(ex)
      case Success(value) =>
        Future.successful(value)
    }
  }
}

object CitizenDetailsConnector extends CitizenDetailsConnector with ServicesConfig {
  override val http: HttpGet = new HttpGet with WSHttp {
    override protected def actorSystem: ActorSystem = Play.current.actorSystem
    override protected def configuration: Option[Config] = Option(Play.current.configuration.underlying)
    override protected def appNameConfiguration: Configuration = Play.current.configuration
  }
  override val serviceUrl: String = baseUrl("citizen-details")
  override val metrics: ApplicationMetrics = Play.current.injector.instanceOf[ApplicationMetrics]
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
