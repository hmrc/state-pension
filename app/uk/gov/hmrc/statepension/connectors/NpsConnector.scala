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
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpReads, HttpResponse}
import uk.gov.hmrc.statepension.domain.nps.{NpsLiabilities, NpsLiability, NpsNIRecord, NpsSummary}

import scala.concurrent.Future

trait NpsConnector {
  def http: HttpGet
  def npsBaseUrl: String
  def serviceOriginatorId: (String, String)

  def getSummary(nino: Nino)(implicit headerCarrier: HeaderCarrier): Future[NpsSummary] = {
    val urlToRead = s"$npsBaseUrl/nps-rest-service/services/nps/pensions/${ninoWithoutSuffix(nino)}/sp_summary"
    connectToNPS[NpsSummary](urlToRead)
  }
  def getLiabilities(nino: Nino)(implicit headerCarrier: HeaderCarrier): Future[List[NpsLiability]] = {
    val urlToRead = s"$npsBaseUrl/nps-rest-service/services/nps/pensions/${ninoWithoutSuffix(nino)}/liabilities"
    connectToNPS[NpsLiabilities](urlToRead).map(_.liabilities)
  }
  def getNIRecord: Future[NpsNIRecord]

  private def connectToNPS[A](url: String)(implicit headerCarrier: HeaderCarrier, reads: Reads[A]): Future[A] = {
    val responseF = http.GET[HttpResponse](url)(HttpReads.readRaw, headerCarrier.withExtraHeaders(serviceOriginatorId))
    responseF.flatMap[A] { httpResponse =>
      httpResponse.json.validate[A].fold(
        invalid => Future.failed(new JsonValidationException(formatJsonErrors(invalid))),
        valid => Future.successful(valid)
      )
    }
  }

  private final val ninoLengthWithoutSuffix = 7
  private def ninoWithoutSuffix(nino: Nino): String = nino.toString().take(ninoLengthWithoutSuffix)

  private def formatJsonErrors(errors: Seq[(JsPath, Seq[ValidationError])]): String = {
    errors.map(p => p._1 + " - " + p._2.map(_.message).mkString(",")).mkString(" | ")
  }

  class JsonValidationException(message: String) extends Exception(message)
}
