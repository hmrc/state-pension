package test.config

import org.apache.pekko.Done
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, getRequestedFor, post, postRequestedFor, urlMatching}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.cache.AsyncCacheApi
import play.api.http.Status.{CREATED, NOT_FOUND, OK}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.statepension.config.InternalAuthTokenInitializer
import utils.{ResponseHelpers, StatePensionBaseSpec, WireMockHelper}

class InternalAuthInitializerISpec extends StatePensionBaseSpec
  with GuiceOneAppPerSuite
  with WireMockHelper
  with ResponseHelpers {
  "AuthTokenInitializer" should {
    "return Done with no requests sent to internal-auth" when {
      "isTestEndpoints is configured to false" in {

        val authToken = "authToken"
        val appName = "appName"

        server.stubFor(
          get(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(OK))
        )

        server.stubFor(
          post(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(CREATED))
        )

        val app = GuiceApplicationBuilder()
          .overrides(
            bind[AsyncCacheApi].toInstance(mockCacheApi)
          )
          .configure(
            "microservice.services.internal-auth.port" -> server.port(),
            "appName" -> appName,
            "internal-auth.token" -> authToken,
          )
          .build()

        app.injector.instanceOf[InternalAuthTokenInitializer].initializeToken.futureValue shouldBe Done

        server.verify(0, getRequestedFor(urlMatching("/test-only/token")))
        server.verify(0, postRequestedFor(urlMatching("/test-only/token")))
      }

      "isTestEndpoints is configured to true and token is already valid" in {
        val authToken = "authToken"
        val appName = "appName"

        server.stubFor(
          get(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(OK))
        )

        server.stubFor(
          post(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(CREATED))
        )

        val app = GuiceApplicationBuilder()
          .overrides(
            bind[AsyncCacheApi].toInstance(mockCacheApi)
          )
          .configure(
            "microservice.services.internal-auth.port" -> server.port(),
            "appName" -> appName,
            "internal-auth.token" -> authToken,
            "internal-auth.isTestOnlyEndpoint" -> true
          )
          .build()

        app.injector.instanceOf[InternalAuthTokenInitializer].initializeToken.futureValue shouldBe Done

        server.verify(1, getRequestedFor(urlMatching("/test-only/token")))
        server.verify(0, postRequestedFor(urlMatching("/test-only/token")))
      }

      "isTestEndpoints is configured to true and token needs intitializing" in {
        val authToken = "authToken"
        val appName = "appName"

        server.stubFor(
          get(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(NOT_FOUND))
        )

        server.stubFor(
          post(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(CREATED))
        )

        val app = GuiceApplicationBuilder()
          .overrides(
            bind[AsyncCacheApi].toInstance(mockCacheApi)
          )
          .configure(
            "microservice.services.internal-auth.port" -> server.port(),
            "appName" -> appName,
            "internal-auth.token" -> authToken,
            "internal-auth.isTestOnlyEndpoint" -> true
          )
          .build()

        app.injector.instanceOf[InternalAuthTokenInitializer].initializeToken.futureValue shouldBe Done

        server.verify(1, getRequestedFor(urlMatching("/test-only/token")))
        server.verify(1, postRequestedFor(urlMatching("/test-only/token")))
      }
    }
  }
}
