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

package uk.gov.hmrc.statepension.controllers.auth

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{verify, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, OK, UNAUTHORIZED}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, InternalError}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.statepension.StatePensionBaseSpec

import scala.concurrent.{ExecutionContext, Future}

class PrivilegedAuthActionSpec extends StatePensionBaseSpec {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val controllerComponents = Helpers.stubControllerComponents()

  val injector = new GuiceApplicationBuilder()
    .overrides(bind[AuthConnector].toInstance(mockAuthConnector))
    .injector()

  val authAction: PrivilegedAuthAction = injector.instanceOf[PrivilegedAuthAction]

  object Harness extends BackendController(controllerComponents) {
    def onPageLoad(): Action[AnyContent] = authAction(_ => Ok)
  }

  def setupAuthConnector(result: Future[Unit]): OngoingStubbing[Future[Unit]] =
    when(mockAuthConnector.authorise(
      ArgumentMatchers.eq(AuthProviders(PrivilegedApplication)),
      ArgumentMatchers.eq(EmptyRetrieval))(
      ArgumentMatchers.any[HeaderCarrier],
      ArgumentMatchers.any[ExecutionContext]
    )).thenReturn(result)


  val request: Request[AnyContent] = FakeRequest()
  implicit val hc: HeaderCarrier =
    HeaderCarrierConverter.fromRequest(request)

  "PrivilegedAuthAction" should {
    "make a call to auth for privileged application" in {
      setupAuthConnector(Future.successful(()))

      Harness.onPageLoad()(request).futureValue

      verify(mockAuthConnector).authorise(
        ArgumentMatchers.eq(AuthProviders(PrivilegedApplication)),
        ArgumentMatchers.eq(EmptyRetrieval))(
        ArgumentMatchers.any[HeaderCarrier],
        ArgumentMatchers.any[ExecutionContext]
      )
    }

    "return OK" when {
      "user is authenticated" in {
        setupAuthConnector(Future.successful(()))

        val result: Future[Result] = Harness.onPageLoad()(request)

        status(result) mustBe OK
      }
    }

    "return Unauthorized" when {
      "auth throws an authenticated exception" in {
        setupAuthConnector(Future.failed(InternalError()))

        val result = Harness.onPageLoad()(request)

        status(result) mustBe UNAUTHORIZED
      }
    }

    "return InternalServerError" when {
      "auth throws an exception which isn't an authenticated exception" in {
        setupAuthConnector(Future.failed(new Exception("Expected Exception")))

        val result = Harness.onPageLoad()(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
