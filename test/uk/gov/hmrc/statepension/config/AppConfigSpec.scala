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

import org.scalatest.Assertion
import org.scalatestplus.play.PlaySpec
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder

class AppConfigSpec extends PlaySpec {

  val injector: Injector = new GuiceApplicationBuilder()
    .configure(
      ("microservice.services.if-hod.host", "testLocalHost"),
      ("microservice.services.if-hod.port", "1234"),
      ("microservice.services.if-hod.environment", "test"),
      ("microservice.services.if-hod.token", "testABC123"),
      ("microservice.services.if-hod.originatoridkey", "testOriginatorId"),
      ("microservice.services.if-hod.originatoridvalue", "testOriginatorIdValue")
    )
    .injector()

  val appConfig = injector.instanceOf[AppConfig]

  "AppConfig" must {
    "return the correct values for a ConnectorConfig" in {
      val ifConnectorConfig: ConnectorConfig = appConfig.ifConnectorConfig

      behave like serviceUrl(ifConnectorConfig.serviceUrl)
      ifConnectorConfig.authorizationToken mustBe "testABC123"
      ifConnectorConfig.environment mustBe "test"
      ifConnectorConfig.serviceOriginatorIdValue mustBe "testOriginatorIdValue"
      ifConnectorConfig.serviceOriginatorIdKey mustBe "testOriginatorId"
    }
  }

  def serviceUrl(serviceUrl: String): Assertion = {
    serviceUrl must include("testLocalHost")
    serviceUrl must include("1234")
  }
}
