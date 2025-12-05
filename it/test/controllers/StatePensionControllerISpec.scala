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

package controllers

import com.github.tomakehurst.wiremock.client.WireMock.{unauthorized, *}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, status as statusResult, *}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import utils.{GenerateNino, ResponseHelpers, StatePensionBaseSpec, WireMockHelper}

import scala.concurrent.{ExecutionContext, Future}

trait StatePensionControllerISpec
  extends StatePensionBaseSpec
    with GuiceOneAppPerSuite
    with WireMockHelper
    with ResponseHelpers
    with GenerateNino {

  val FIXED_DELAY = 25000

  private val nino: Nino = Nino("HS191148D")
  private val proxyCacheUrl: String = s"/ni-and-sp-proxy-cache/${nino.nino}"
  def checkPensionControllerUrl(nino: Nino): String

  private val defaultHeaders: Seq[(String, String)] = Seq(
    "Accept" -> "application/vnd.hmrc.1.0+json",
    "Authorization" -> "Bearer 123"
  )

  private def generateAuthHeaderResponse: String =
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
       |  },
       |  "clientId": "$nino"
       |}"""
      .stripMargin

  private def pertaxAuthResponse: String =
    s"""
       |{
       | "code": "ACCESS_GRANTED",
       | "message": ""
       |}
       |""".stripMargin

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> server.port(),
        "microservice.services.pertax.host" -> "127.0.0.1",
        "microservice.services.pertax.port" -> server.port(),
        "microservice.services.nps-hod.host" -> "127.0.0.1",
        "microservice.services.nps-hod.port" -> server.port(),
        "microservice.services.des-hod.host" -> "127.0.0.1",
        "microservice.services.des-hod.port" -> server.port(),
        "microservice.services.if-hod.host" -> "127.0.0.1",
        "microservice.services.if-hod.port" -> server.port(),
        "microservice.services.ni-and-sp-proxy-cache.host" -> "127.0.0.1",
        "microservice.services.ni-and-sp-proxy-cache.port" -> server.port(),
        "play.ws.timeout.request" -> "1000ms",
        "play.ws.timeout.connection" -> "500ms",
        "auditing.enabled" -> false,
        "internal-auth.isTestOnlyEndpoint" -> false
      ).build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    stubPostServer(ok(generateAuthHeaderResponse), "/auth/authorise")
    stubPostServer(ok(pertaxAuthResponse), "/pertax/authorise")
  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  private val requests = List(
    notFound() -> NOT_FOUND -> "NOT_FOUND",
    gatewayTimeout() -> GATEWAY_TIMEOUT -> "GATEWAY_TIMEOUT",
    badGateway() -> BAD_GATEWAY -> "BAD_GATEWAY",
    badRequest() -> BAD_REQUEST -> "BAD_REQUEST",
    noOpenCopeWorkItem() -> FORBIDDEN -> "NO_OPEN_COPE_WORK_ITEM",
    closedCopeWorkItem() -> FORBIDDEN -> "CLOSED_COPE_WORK_ITEM",
    unauthorized() -> BAD_GATEWAY -> "BAD_GATEWAY from 4xx",
    serviceUnavailable() -> BAD_GATEWAY -> "BAD_GATEWAY from 5xx",
    httpClientTimeout(FIXED_DELAY) -> INTERNAL_SERVER_ERROR -> "INTERNAL_SERVER_ERROR",
  )
  
  "get" must {

    requests.foreach {
      case ((response, statusCode), errorDescription) =>

        s"return $statusCode $errorDescription" in {

          stubGetServer(response, proxyCacheUrl)

          val request = FakeRequest(GET, checkPensionControllerUrl(nino))
            .withHeaders(defaultHeaders *)

          val result = route(app, request)

          result.map(statusResult) shouldBe Some(statusCode)
        }
    }
  }

}
