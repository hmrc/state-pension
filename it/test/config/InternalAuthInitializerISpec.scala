/*
 * Copyright 2024 HM Revenue & Customs
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

package config

import com.github.tomakehurst.wiremock.client.WireMock._
import org.apache.pekko.Done
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.{CREATED, NOT_FOUND, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.statepension.config.InternalAuthTokenInitializer
import utils.StatePensionBaseSpec

class InternalAuthInitializerISpec
  extends StatePensionBaseSpec
    with WireMockSupport {

  private def builder(isTestOnlyEndpoint: Boolean): GuiceApplicationBuilder =
    GuiceApplicationBuilder()
      .configure(
        "internal-auth.isTestOnlyEndpoint"         -> isTestOnlyEndpoint,
        "microservice.services.internal-auth.port" -> wireMockPort
      )

  "AuthTokenInitializer" should {
    "return Done with no requests sent to internal-auth" when {
      "isTestEndpoints is configured to false" in {

        wireMockServer.stubFor(
          get(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(OK))
        )

        wireMockServer.stubFor(
          post(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(CREATED))
        )

        val app = builder(isTestOnlyEndpoint = false).build()

        running(app) {
          app.injector.instanceOf[InternalAuthTokenInitializer].initializeToken.futureValue shouldBe Done

          wireMockServer.verify(0, getRequestedFor(urlMatching("/test-only/token")))
          wireMockServer.verify(0, postRequestedFor(urlMatching("/test-only/token")))
        }
      }
    }

    "return Done with one request sent to internal-auth" when {
      "isTestEndpoints is configured to true and token is already valid" in {

        wireMockServer.stubFor(
          get(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(OK))
        )

        wireMockServer.stubFor(
          post(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(CREATED))
        )

        val app = builder(isTestOnlyEndpoint = true).build()

        running(app) {
          app.injector.instanceOf[InternalAuthTokenInitializer].initializeToken.futureValue shouldBe Done

          wireMockServer.verify(1, getRequestedFor(urlMatching("/test-only/token")))
          wireMockServer.verify(0, postRequestedFor(urlMatching("/test-only/token")))
        }
      }
    }

    "return Done with two requests sent to internal-auth" when {
      "isTestEndpoints is configured to true and token needs intitializing" in {

        wireMockServer.stubFor(
          get(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(NOT_FOUND))
        )

        wireMockServer.stubFor(
          post(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(CREATED))
        )

        val app = builder(isTestOnlyEndpoint = true).build()

        running(app) {
          app.injector.instanceOf[InternalAuthTokenInitializer].initializeToken.futureValue shouldBe Done

          wireMockServer.verify(1, getRequestedFor(urlMatching("/test-only/token")))
          wireMockServer.verify(1, postRequestedFor(urlMatching("/test-only/token")))
        }
      }
    }
  }
}
