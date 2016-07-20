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

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HttpResponse, HttpReads, HeaderCarrier, HttpGet}
import uk.gov.hmrc.statepension.WSHttp
import uk.gov.hmrc.statepension.domain.{StatePensionExclusion, StatePension}

import scala.concurrent.Future

trait NispConnector {
  def http: HttpGet
  def nispBaseUrl: String
  private def ninoWithoutSuffix(nino: Nino): String = nino.value.substring(0, 8)

  def getStatePension(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExclusion, StatePension]] = {
    val response = http.GET[HttpResponse](s"/$nispBaseUrl/${ninoWithoutSuffix(nino)}") (rds = HttpReads.readRaw, hc)
    val x = StatePensionExclusion(Nil,1,new LocalDate)
    Future.successful(Left(x))
   //TODO
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