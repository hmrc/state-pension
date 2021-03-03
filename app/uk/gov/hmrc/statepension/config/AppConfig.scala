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

package uk.gov.hmrc.statepension.config

import com.google.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class AppConfig @Inject()(configuration: Configuration, servicesConfig: ServicesConfig){
  import servicesConfig._

  val appName: String = getString("appName")
  val apiGatewayContext: String = getString("api.gateway.context")
  val access: Option[Configuration] = configuration.getOptional[Configuration]("api.access")
  val status: Option[String] = configuration.getOptional[String]("api.status")
  val rates: Configuration = configuration.getOptional[Configuration]("rates.statePension")
    .getOrElse(throw new RuntimeException("rates.statePension is missing"))
  val revaluation: Option[Configuration] = configuration.getOptional[Configuration]("rates.revaluation")

  val citizenDetailsBaseUrl: String = baseUrl("citizen-details")
  val desConnectorConfig: ConnectorConfig = connectorConfig("des-hod")
  val ifConnectorConfig: ConnectorConfig = connectorConfig("if-hod")

  def dwpApplicationId:Option[Seq[String]] = APIAccessConfig(access).whiteListedApplicationIds

  val dwpOriginatorId: String = configuration.get[String]("cope.dwp.originatorId")
  val copeFeatureEnabled: Boolean = configuration.get[Boolean]("cope.feature.enabled")
  val copeReturnToServiceDays: Int = configuration.get[Int]("cope.returnToServiceDays")

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
