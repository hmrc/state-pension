package controllers

import com.github.tomakehurst.wiremock.client.WireMock.{notFound, ok}
import controllers.Assets.OK
import org.scalatest.Matchers.convertToAnyShouldWrapper
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, status => statusResult, _}
import test_utils.IntegrationBaseSpec

class StatePensionControllerISpec extends IntegrationBaseSpec {

  val nino = generateNino

  def defaultHeaders: (String, String) = {
    "Accept" -> "application/vnd.hmrc.1.0+json"
  }

  def generateAuthHeaderResponse = {
      s"""
         |{
         |  "nino": "$nino",
         |  "trustedHelper": {
         |    "principalName": "Mr Test",
         |    "attorneyName": "Mr Test",
         |    "returnLinkUrl": "http://test.com/",
         |    "principalNino": "$nino"
         |  },
         |  "authProviderId": {
         |    "ggCredId": "xyz"
         |  }
         |}"""
        .stripMargin
  }

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
    stubPostServer(ok(generateAuthHeaderResponse), "/auth/authorise")
  }

  "get" must {
    s"return $OK" in {
      val controllerUrl: String = s"/ni/$nino"
      val npsSummaryUrl: String = s"/individuals/${nino.withoutSuffix}/pensions/summary"
      val npsLiabilitiesUrl: String = s"/individuals/${nino.withoutSuffix}/pensions/liabilities"
      val npsNiRecordUrl: String = s"/individuals/${nino.withoutSuffix}/pensions/ni"
      val citizenDetailsUrl: String = s"/citizen-details/$nino/designatory-details/"

      stubGetServer(notFound(), npsSummaryUrl)
      stubGetServer(notFound(), npsLiabilitiesUrl)
      stubGetServer(notFound(), npsNiRecordUrl)
      stubGetServer(notFound(), citizenDetailsUrl)

      val request = FakeRequest(GET, controllerUrl).withHeaders(defaultHeaders)
      val result = route(fakeApplication(), request)

      result.map(statusResult) shouldBe Some(NOT_FOUND)
    }
  }
}
