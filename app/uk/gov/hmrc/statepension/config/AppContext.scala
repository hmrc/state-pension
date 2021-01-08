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

// TODO[REFACTOR] need to use configuration to get at rates and revaluation which could be pushed to separate classes
class AppContext @Inject()(configuration: Configuration, servicesConfig: ServicesConfig){
  import servicesConfig._

  // TODO can this be injected
  val appName = getString("appName")
  val apiGatewayContext = getString("api.gateway.context")

  val access = configuration.getOptional[Configuration]("api.access")
  val status = configuration.getOptional[String]("api.status")

  //TODO is there a better way of doing this. Does get[A] throw an error
  val rates: Configuration = configuration.getOptional[Configuration]("rates.statePension").getOrElse(throw new RuntimeException("rates.statePension is missing"))
  val revaluation: Option[Configuration] = configuration.getOptional[Configuration]("rates.revaluation")

  //TODO remove
//  val ifBaseUrl: String = baseUrl("if-hod")
//  val ifOriginatorIdKey: String = getConfString("if-hod.originatoridkey", "")
//  val ifOriginatorIdValue: String = getConfString("if-hod.originatoridvalue", "")
//  val ifEnvironment: String = getConfString("if-hod.environment", "")
//  val ifToken: String = getConfString("if-hod.token", "")

  val citizenDetailsBaseUrl: String = baseUrl("citizen-details")
  val desConnectorConfig: ConnectorConfig = connectorConfig("des-hod")
  val ifConnectorConfig: ConnectorConfig = connectorConfig("if-hod")

  //TODO test
  private def connectorConfig(serviceName: String): ConnectorConfig = {

    def getPrefixString(key: String) = getString(s"microservice.services.$key")

    new ConnectorConfig(
      serviceUrl = baseUrl(serviceName),
      serviceOriginatorIdKey = getPrefixString(s"$serviceName.originatoridkey"),
      serviceOriginatorIdValue = getPrefixString(s"$serviceName.originatoridvalue"),
      environment = getPrefixString(s"$serviceName.environment"),
      authorizationToken = getPrefixString(s"$serviceName.token")
    )

  }

}
