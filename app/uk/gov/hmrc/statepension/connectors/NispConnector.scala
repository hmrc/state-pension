/*
 * Copyright 2016 HM Revenue & Customs
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

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpReads, HttpResponse}
import uk.gov.hmrc.statepension.WSHttp
import uk.gov.hmrc.statepension.domain.Exclusion.Exclusion
import uk.gov.hmrc.statepension.domain.{StatePension, StatePensionExclusion}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait NispConnector {
  def http: HttpGet

  def nispBaseUrl: String

  def getStatePension(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExclusion, StatePension]] = {

    val response = http.GET[HttpResponse](s"$nispBaseUrl/state-pension/$nino")(rds = HttpReads.readRaw, hc)
    response.flatMap { httpResponse =>
      val isExclusion = (httpResponse.json \ "exclusionReasons").asOpt[List[Exclusion]].isDefined

      if(isExclusion) {
        Try(httpResponse.json.validate[StatePensionExclusion]).flatMap(
          jsResult =>
            jsResult.fold(errs => Failure(new Exception(errs.toString())), valid => Success(Left(valid)))
        ) match {
          case Success(s: Either[StatePensionExclusion, StatePension]) => Future.successful(s)
          case Failure(ex) => Future.failed(ex)
        }
      } else {
        Try(httpResponse.json.validate[StatePension]).flatMap(
          jsResult =>
            jsResult.fold(errs => Failure(new Exception(errs.toString())), valid => Success(Right(valid)))
        ) match {
          case Success(s: Either[StatePensionExclusion, StatePension]) => Future.successful(s)
          case Failure(ex) => Future.failed(ex)
        }
      }
    }
  }
}

object SandboxNispConnector extends NispConnector with ServicesConfig {
  override val nispBaseUrl: String = baseUrl("nisp")
  override def http: HttpGet = WSHttp
}

object NispConnector extends NispConnector with ServicesConfig {
  override val nispBaseUrl: String = baseUrl("nisp")
  override def http: HttpGet = WSHttp
}