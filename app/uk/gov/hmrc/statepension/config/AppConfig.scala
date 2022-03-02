/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.statepension.config

import com.google.inject.Inject
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.statepension.models.TaxRates
import uk.gov.hmrc.statepension.services.TaxYearResolver
import uk.gov.hmrc.statepension.util.SystemLocalDate

import java.time.LocalDate
import scala.io.Source

class AppConfig @Inject()(configuration: Configuration, servicesConfig: ServicesConfig, systemLocalDate: SystemLocalDate){
  import servicesConfig._

  val appName: String = configuration.get[String]("appName")
  val apiGatewayContext: String = configuration.get[String]("api.gateway.context")
  val access: Option[Configuration] = configuration.getOptional[Configuration]("api.access")
  val status: Option[String] = configuration.getOptional[String]("api.status")
  val effectiveFromDate: LocalDate = LocalDate.parse(configuration.get[String]("rates.effectiveFromDate"))

  def taxRates(year: Int) = {
    if (systemLocalDate.currentLocalDate.isBefore(effectiveFromDate)) getTaxRatesByTaxYear(year - 1)
    else getTaxRatesByTaxYear(year)
  }

  private def getTaxRatesByTaxYear(year: Int): TaxRates = {
    val content = Source.fromURL(getClass.getResource(s"/resources/TaxRates/$year.json")).mkString

    Json.parse(content).as[TaxRates]
  }

  val rates: Configuration = configuration.getOptional[Configuration]("rates.statePension")
    .getOrElse(throw new RuntimeException("rates.statePension is missing"))
  val revaluation: Option[Configuration] = configuration.getOptional[Configuration]("rates.revaluation")

  val ninoHashingKey: String = configuration.get[String]("ninoHashingKey")

  val citizenDetailsBaseUrl: String = baseUrl("citizen-details")
  val desConnectorConfig: ConnectorConfig = connectorConfig("des-hod")
  val ifConnectorConfig: ConnectorConfig = connectorConfig("if-hod")

  def dwpApplicationId:Option[Seq[String]] = APIAccessConfig(access).whiteListedApplicationIds

  val dwpOriginatorId: String = configuration.get[String]("cope.dwp.originatorId")
  val returnToServiceWeeks: Int = configuration.get[Int]("cope.returnToServiceWeeks")
  val ttlInWeeks: Int = configuration.get[Int]("cope.ttlInWeeks")

  private def connectorConfig(serviceName: String): ConnectorConfig = {
    val empty = ""

    new ConnectorConfig(
      serviceUrl = baseUrl(serviceName),
      serviceOriginatorIdKey = getConfString(s"$serviceName.originatoridkey", empty),
      serviceOriginatorIdValue = getConfString(s"$serviceName.originatoridvalue", empty),
      environment = getConfString(s"$serviceName.environment", empty),
      authorizationToken = getConfString(s"$serviceName.token", empty)
    )
  }
}
