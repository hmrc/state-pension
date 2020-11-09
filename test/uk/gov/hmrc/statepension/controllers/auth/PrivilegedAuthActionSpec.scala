/*
 * Copyright 2020 HM Revenue & Customs
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

import org.mockito.Matchers
import org.mockito.Mockito.{verify, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.{Action, AnyContent, Controller}
import play.api.test.FakeRequest
import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, OK, UNAUTHORIZED, await, defaultAwaitTimeout, status}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, InternalError}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class PrivilegedAuthActionSpec extends PlaySpec with MockitoSugar {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val authAction: PrivilegedAuthAction = new PrivilegedAuthAction(mockAuthConnector)

  object Harness extends Controller {
    def onPageLoad(): Action[AnyContent] = authAction(_ => Ok)
  }

  def setupAuthConnector(result: Future[Unit]): OngoingStubbing[Future[Unit]] =
    when(mockAuthConnector.authorise(
      Matchers.eq(AuthProviders(PrivilegedApplication)),
      Matchers.eq(EmptyRetrieval))(
      Matchers.any[HeaderCarrier],
      Matchers.any[ExecutionContext]
    )).thenReturn(result)

  "PrivilegedAuthAction" must {
    "make a call to auth for privileged application" in {
      val request = FakeRequest()
      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromHeadersAndSessionAndRequest(request.headers, request = Some(request))

      setupAuthConnector(Future.successful(()))

      await(Harness.onPageLoad()(request))

      verify(mockAuthConnector).authorise(
        Matchers.eq(AuthProviders(PrivilegedApplication)),
        Matchers.eq(EmptyRetrieval))(
        Matchers.any[HeaderCarrier],
        Matchers.any[ExecutionContext]
      )
    }

    "return OK" when {
      "user is authenticated" in {
        val request = FakeRequest()
        implicit val hc: HeaderCarrier =
          HeaderCarrierConverter.fromHeadersAndSessionAndRequest(request.headers, request = Some(request))

        setupAuthConnector(Future.successful(()))

        val result = Harness.onPageLoad()(request)

        status(result) mustBe OK
      }
    }

    "return Unauthorized" when {
      "auth throws an authenticated exception" in {
        val request = FakeRequest()
        implicit val hc: HeaderCarrier =
          HeaderCarrierConverter.fromHeadersAndSessionAndRequest(request.headers, request = Some(request))

        setupAuthConnector(Future.failed(InternalError()))

        val result = Harness.onPageLoad()(request)

        status(result) mustBe UNAUTHORIZED
      }
    }

    "return InternalServerError" when {
      "auth throws an exception which isn't an authenticated exception" in {
        val request = FakeRequest()
        implicit val hc: HeaderCarrier =
          HeaderCarrierConverter.fromHeadersAndSessionAndRequest(request.headers, request = Some(request))

        setupAuthConnector(Future.failed(new Exception("Expected Exception")))

        val result = Harness.onPageLoad()(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
