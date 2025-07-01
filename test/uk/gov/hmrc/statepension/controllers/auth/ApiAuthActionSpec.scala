/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.statepension.controllers.auth

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.*
import play.api.inject
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.{AuthConnector, UnsupportedCredentialRole}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.StatePensionBaseSpec

import scala.concurrent.Future

class ApiAuthActionSpec extends StatePensionBaseSpec with GuiceOneAppPerSuite with BeforeAndAfter with Matchers {

  val controllerComponents: ControllerComponents = Helpers.stubControllerComponents()
  private val ninoGenerator: Generator = new Generator()
  private val testNino: String = ninoGenerator.nextNino.nino
  private val goodUriWithNino: String = s"/ni/$testNino/"

  class AuthActionTestHarness(apiAuthActionImpl: ApiAuthAction) extends BackendController(controllerComponents) {
    def onPageLoad(): Action[AnyContent] = apiAuthActionImpl { request =>
      Ok
    }
  }

  private def newMockConnectorWithAuthResult[T](authoriseResult: Future[T]): AuthConnector = {
    val connector = mock[AuthConnector]

    when(connector.authorise[T](any(), any())(any(), any()))
      .thenReturn(authoriseResult)

    connector
  }

  private def testApiAuthActionWith[T](authResult: Future[T],
                                       uri: String = goodUriWithNino): (Future[Result], AuthConnector) = {
    val mockAuthConnector = newMockConnectorWithAuthResult(authResult)

    val injector = new GuiceApplicationBuilder()
      .overrides(inject.bind[AuthConnector].toInstance(mockAuthConnector))
      .injector()

    val authAction = injector.instanceOf[ApiAuthAction]

    val testHarness = new AuthActionTestHarness(authAction)

    (testHarness.onPageLoad()(FakeRequest(method = "", path = uri)),
      mockAuthConnector)
  }

  "ApiAuthAction" should {
    "allows the user through when they have a valid client id" in {
      val (result, _) = testApiAuthActionWith(Future.successful(Some("")))
      status(result) mustBe OK
    }

    "not allow the user through when they have an invalid client id" in {
      val (result, _) = testApiAuthActionWith(Future.successful(None))
      status(result) mustBe UNAUTHORIZED
    }

    "return INTERNAL_SERVER_ERROR" when {
      "auth returns an unexpected exception" in {
        val (result, _) = testApiAuthActionWith(Future.failed(new Exception("")))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "auth returns an unexpected authorisation error" in {
      val (result, _) = testApiAuthActionWith(Future.failed(UnsupportedCredentialRole()))
      status(result) mustBe UNAUTHORIZED
    }

  }


}
