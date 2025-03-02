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
import play.api.http.Status.{LOCKED, NOT_FOUND}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, NotFoundException, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.domain.nps.APIType
import uk.gov.hmrc.statepension.services.ApplicationMetrics

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class CitizenDetailsConnector @Inject()(http: HttpClientV2,
                                        metrics: ApplicationMetrics,
                                        appContext: AppConfig)(implicit ec: ExecutionContext){

  implicit val legacyRawReads: HttpReads[HttpResponse] = HttpReads.Implicits.throwOnFailure(HttpReads.Implicits.readEitherOf( using HttpReads.Implicits.readRaw))

  val serviceUrl: String = appContext.citizenDetailsBaseUrl

  private def url(nino: Nino) = url"$serviceUrl/citizen-details/$nino/designatory-details/"

  def connectToGetPersonDetails(nino: Nino)(implicit hc: HeaderCarrier): Future[Int] = {
    val timerContext = metrics.startTimer(APIType.CitizenDetails)
    http
      .get(url(nino))
      .execute[HttpResponse] map {
      personResponse =>
        timerContext.stop()
        Success(personResponse.status)
    } recover {
      case ex: UpstreamErrorResponse if ex.statusCode == LOCKED =>
        timerContext.stop(); Success(ex.statusCode)
      case ex: UpstreamErrorResponse if ex.statusCode == NOT_FOUND =>
        timerContext.stop()
        Failure(new NotFoundException("Nino was not found."))
      case ex: Throwable =>
        timerContext.stop()
        Failure(ex)
    } flatMap handleResult
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
