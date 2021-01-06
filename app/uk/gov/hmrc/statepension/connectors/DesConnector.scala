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
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.statepension.domain.nps.APIType.{Liabilities, NIRecord, Summary}
import uk.gov.hmrc.statepension.domain.nps._
import uk.gov.hmrc.statepension.services.ApplicationMetrics

class DesConnector @Inject()(val http: HttpClient,
                             val metrics: ApplicationMetrics,
                             environment: Environment,
                             val runModeConfiguration: Configuration) extends NpsConnector with ServicesConfig {

  override val serviceOriginatorId: (String, String) = (getConfString("des-hod.originatoridkey", ""), getConfString("des-hod.originatoridvalue", ""))
  override val environmentHeader: (String, String) = ("Environment", getConfString("des-hod.environment", ""))
  override val token: String = getConfString("des-hod.token", "")

  override val summaryMetricType: APIType = Summary
  override val liabilitiesMetricType: APIType = Liabilities
  override val niRecordMetricType: APIType = NIRecord

  val desBaseUrl: String = baseUrl("des-hod")
  override def summaryUrl(nino: Nino): String = s"$desBaseUrl/individuals/${nino.withoutSuffix}/pensions/summary"
  override def liabilitiesUrl(nino: Nino): String = s"$desBaseUrl/individuals/${nino.withoutSuffix}/pensions/liabilities"
  override def niRecordUrl(nino: Nino): String = s"$desBaseUrl/individuals/${nino.withoutSuffix}/pensions/ni"

  protected def mode: Mode = environment.mode
}

