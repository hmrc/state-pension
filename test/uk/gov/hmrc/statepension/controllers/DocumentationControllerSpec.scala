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

package uk.gov.hmrc.statepension.controllers

import controllers.Assets
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.libs.json.{JsArray, JsDefined, JsString, JsUndefined}
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.Helpers.{contentAsJson, contentAsString, stubControllerComponents, _}
import play.api.test.{FakeRequest, Injecting}
import resource._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.controllers.documentation.DocumentationController
import uk.gov.hmrc.statepension.{CopeRepositoryHelper, StatePensionBaseSpec}

import scala.concurrent.Future
import scala.io.Source

class DocumentationControllerSpec
  extends StatePensionBaseSpec
    with GuiceOneAppPerSuite
    with Injecting
    with CopeRepositoryHelper {

  val controllerComponents: ControllerComponents = stubControllerComponents()
  val serviceConfig: ServicesConfig = inject[ServicesConfig]
  val assets: Assets = inject[Assets]

  def getDefinitionResultFromConfig(apiConfig: Option[Configuration] = None, apiStatus: Option[String] = None): Future[Result] = {

    val appContext = new AppConfig(app.configuration, serviceConfig) {
      override val appName: String = ""
      override val apiGatewayContext: String = ""
      override val access: Option[Configuration] = apiConfig
      override val status: Option[String] = apiStatus
      override val rates: Configuration = Configuration()
      override val revaluation: Option[Configuration] = None
    }

    new DocumentationController(appContext, controllerComponents, assets).definition()(FakeRequest())
  }

  "/definition access" should {

    "return PRIVATE and no Whitelist IDs if there is no application config" in {

      val result: Future[Result] = getDefinitionResultFromConfig(apiConfig = None)
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "access" \ "type" shouldBe JsDefined(JsString("PRIVATE"))
      (contentAsJson(result) \ "api" \ "versions") (0) \ "access" \ "whitelistedApplicationIds" shouldBe JsDefined(JsArray())
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

    "return No Whitelist IDs if the application config has an entry for whiteListIds but no Ids" in {

      val result = getDefinitionResultFromConfig(apiConfig = Some(Configuration.from(Map("type" -> "PRIVATE", "whitelist.applicationIds" -> Seq()))))
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "access" \ "whitelistedApplicationIds" shouldBe JsDefined(JsArray())

    }

    "return Whitelist IDs 'A', 'B', 'C' if the application config has an entry with 'A', 'B', 'C' " in {

      val result = getDefinitionResultFromConfig(apiConfig = Some(Configuration.from(Map("type" -> "PRIVATE", "whitelist.applicationIds" -> Seq("A", "B", "C")))))
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "access" \ "whitelistedApplicationIds" shouldBe JsDefined(JsArray(Seq(JsString("A"), JsString("B"), JsString("C"))))

    }

    "return no whitelistApplicationIds entry if it is not PRIVATE" in {

      val result = getDefinitionResultFromConfig(apiConfig = Some(Configuration.from(Map("type" -> "PUBLIC", "whitelist.applicationIds" -> Seq()))))
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "access" \ "whitelistedApplicationIds" shouldBe a [JsUndefined]

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
      managed(Source.fromInputStream(getClass.getResourceAsStream(path)))
        .acquireAndGet(_.getLines().mkString("\n"))
    }

    "return the correct conf for a given version and path" in {
      implicit val materializer = app.materializer
      val expectedRaml: String = getResource("/public/api/conf/1.0/application.raml")

      val documentationController = inject[DocumentationController]
      val result = documentationController.conf("1.0", "/application.raml")(FakeRequest())
      status(result) shouldBe OK
      contentAsString(result).trim shouldBe expectedRaml.trim
    }
  }
}
