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

package uk.gov.hmrc.statepension.controllers

import controllers.Assets
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.libs.json.{JsDefined, JsString}
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.controllers.documentation.DocumentationController
import uk.gov.hmrc.statepension.util.SystemLocalDate
import utils.{CopeRepositoryHelper, StatePensionBaseSpec}

import scala.concurrent.Future
import scala.io.Source
import scala.util.Using

class DocumentationControllerSpec
  extends StatePensionBaseSpec
    with GuiceOneAppPerSuite
    with Injecting
    with CopeRepositoryHelper {

  val controllerComponents: ControllerComponents = stubControllerComponents()
  val serviceConfig: ServicesConfig = inject[ServicesConfig]
  val assets: Assets = inject[Assets]
  val systemLocalDate: SystemLocalDate = inject[SystemLocalDate]

  def getDefinitionResultFromConfig(apiConfig: Option[Configuration] = None, apiStatus: Option[String] = None): Future[Result] = {

    val appContext = new AppConfig(app.configuration, serviceConfig, systemLocalDate) {
      override val appName: String = ""
      override val apiGatewayContext: String = ""
      override val access: Option[Configuration] = apiConfig
      override val status: Option[String] = apiStatus
    }

    new DocumentationController(appContext, controllerComponents, assets).definition()(FakeRequest())
  }

  "/definition access" should {

    "return PRIVATE if there is no application config" in {

      val result: Future[Result] = getDefinitionResultFromConfig(apiConfig = None)
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "access" \ "type" shouldBe JsDefined(JsString("PRIVATE"))
    }

    "return PRIVATE if the application config says PRIVATE" in {

      val result = getDefinitionResultFromConfig(apiConfig = Some(Configuration.from(Map("type" -> "PRIVATE"))))
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "access" \ "type" shouldBe JsDefined(JsString("PRIVATE"))
    }

    "return PUBLIC if the application config says PUBLIC" in {

      val result = getDefinitionResultFromConfig(apiConfig = Some(Configuration.from(Map("type" -> "PUBLIC"))))
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "access" \ "type" shouldBe JsDefined(JsString("PUBLIC"))
    }
  }

  "/definition status" should {

    "return BETA if there is no application config" in {

      val result = getDefinitionResultFromConfig(apiStatus = None)
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "status" shouldBe JsDefined(JsString("BETA"))
    }

    "return BETA if the application config says BETA" in {

      val result = getDefinitionResultFromConfig(apiStatus = Some("BETA"))
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "status" shouldBe JsDefined(JsString("BETA"))
    }

    "return PUBLISHED if the application config says STABLE" in {

      val result: Future[Result] = getDefinitionResultFromConfig(apiStatus = Some("STABLE"))
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "status" shouldBe JsDefined(JsString("STABLE"))
    }
  }

  "conf" should {

    def getResource(path: String): String = {
      Using(Source.fromInputStream(getClass.getResourceAsStream(path))) { _.getLines().mkString("\n") }.get
    }

    "return the correct conf for a given version and path" in {
      implicit val materializer = app.materializer
      val expectedYaml: String = getResource("/public/api/conf/1.0/application.yaml")

      val documentationController = inject[DocumentationController]
      val result = documentationController.conf("1.0", "/application.yaml")(FakeRequest())
      status(result) shouldBe OK
      contentAsString(result).trim shouldBe expectedYaml.trim
    }
  }
}
