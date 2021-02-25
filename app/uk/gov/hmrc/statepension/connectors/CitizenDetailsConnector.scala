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

import com.google.inject.Inject
import play.api.http.Status.LOCKED
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, Upstream4xxResponse}
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.domain.nps.APIType
import uk.gov.hmrc.statepension.services.ApplicationMetrics
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class CitizenDetailsConnector @Inject()(http: HttpClient,
                                        metrics: ApplicationMetrics,
                                        appContext: AppConfig)(implicit ec: ExecutionContext){

  val serviceUrl: String = appContext.citizenDetailsBaseUrl

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
    } flatMap (handleResult(_))
  }

  private def handleResult[A](tryResult: Try[A]): Future[A] = {
    tryResult match {
      case Failure(ex) =>
        Future.failed(ex)
      case Success(value) =>
        Future.successful(value)
    }
  }
}
