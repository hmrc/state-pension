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

package uk.gov.hmrc.statepension.config

import com.google.inject.Inject
import java.time.LocalDate
import play.api.{ConfigLoader, Configuration}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.statepension.models.TaxRates
import uk.gov.hmrc.statepension.services.TaxYearResolver
import uk.gov.hmrc.statepension.util.FileReader.getTaxRatesByTaxYear
import uk.gov.hmrc.statepension.util.SystemLocalDate


class AppConfig @Inject()(
  configuration: Configuration,
  servicesConfig: ServicesConfig,
  systemLocalDate: SystemLocalDate
) {
  import servicesConfig._

  implicit val dateLoader: ConfigLoader[LocalDate] = ConfigLoader(_.getString).map(LocalDate.parse(_))

  val appName: String = configuration.get[String]("appName")
  val apiGatewayContext: String = configuration.get[String]("api.gateway.context")
  val access: Option[Configuration] = configuration.getOptional[Configuration]("api.access")
  val status: Option[String] = configuration.getOptional[String]("api.status")
  val effectiveFromDate: LocalDate = configuration.getOptional[LocalDate]("rates.effectiveFromDate")
    .getOrElse(TaxYearResolver.startOfCurrentTaxYear)

  def taxRates: TaxRates = {
    val today = systemLocalDate.currentLocalDate
    if (today.isBefore(effectiveFromDate.withYear(today.getYear))) getTaxRatesByTaxYear(today.getYear - 1)
    else getTaxRatesByTaxYear(today.getYear)
  }

  val ninoHashingKey: String = configuration.get[String]("ninoHashingKey")

  val citizenDetailsBaseUrl: String = baseUrl("citizen-details")
  val desConnectorConfig: ConnectorConfig = connectorConfig("des-hod")
  val proxyCacheUrl: String = baseUrl("ni-and-sp-proxy-cache")
  val ifConnectorConfig: ConnectorConfig = connectorConfig("if-hod")

  val internalAuthToken: String = configuration.get[String]("internal-auth.token")
  val internalAuthBaseUrl: String = baseUrl("internal-auth")

  def dwpApplicationId:Option[Seq[String]] = APIAccessConfig(access).whiteListedApplicationIds

  val dwpOriginatorId: String = configuration.get[String]("cope.dwp.originatorId")
  val returnToServiceWeeks: Int = configuration.get[Int]("cope.returnToServiceWeeks")
  val ttlInWeeks: Int = configuration.get[Int]("cope.ttlInWeeks")

  def statePensionExclusionOffset: StatePensionExclusionOffset =
    StatePensionExclusionOffset(
      years = getConfInt("statePensionExclusion.offset.years", 0),
      months = getConfInt("statePensionExclusion.offset.months", 0),
      weeks = getConfInt("statePensionExclusion.offset.weeks", 0),
      days = getConfInt("statePensionExclusion.offset.days", 0),
    )

  def pertaxBaseUrl: String = baseUrl("pertax")

  private def connectorConfig(serviceName: String): ConnectorConfig = {
    new ConnectorConfig(
      serviceUrl = baseUrl(serviceName),
      serviceOriginatorIdKey = getConfString(s"$serviceName.originatoridkey", ""),
      serviceOriginatorIdValue = getConfString(s"$serviceName.originatoridvalue", ""),
      environment = getConfString(s"$serviceName.environment", ""),
      authorizationToken = getConfString(s"$serviceName.token", "")
    )
  }

}
