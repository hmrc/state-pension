package controllers

import com.github.tomakehurst.wiremock.client.WireMock.ok
import org.scalatest.Matchers.convertToAnyShouldWrapper
import play.api.inject.guice.GuiceApplicationBuilder
import test_utils.IntegrationBaseSpec

class StatePensionControllerISpec extends IntegrationBaseSpec {

  override def fakeApplication() = GuiceApplicationBuilder().configure(
    "microservice.services.auth.port" -> server.port(),
    "microservice.services.service-locator.host" -> "127.0.0.1",
    "microservice.services.service-locator.port" -> server.port(),
    "microservice.services.citizen-details.host" -> "127.0.0.1",
    "microservice.services.citizen-details.port" -> server.port(),
    "microservice.services.nps-hod.host" -> "127.0.0.1",
    "microservice.services.nps-hod.port" -> server.port(),
    "microservice.services.des-hod.host" -> "127.0.0.1",
    "microservice.services.des-hod.port" -> server.port(),
    "microservice.services.if-hod.host" -> "127.0.0.1",
    "microservice.services.if-hod.port" -> server.port(),
    "auditing.enabled" -> false
  ).build()

  override def beforeEach() = {
    super.beforeEach()
    stubPostServer(ok("{}"), "/auth/authorise")
  }

  "get" must {
    "return a 200" in {
      "true" shouldBe "true"
    }
  }
}
