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

import com.github.tomakehurst.wiremock.client.WireMock.{unauthorized, _}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, status => statusResult, _}
import uk.gov.hmrc.domain.Nino
import utils.{NinoGenerator, ResponseHelpers, UnitSpec, WireMockHelper}

class StatePensionControllerISpec
  extends UnitSpec
    with GuiceOneAppPerSuite
    with WireMockHelper
    with ResponseHelpers
    with NinoGenerator {

  private val nino: Nino = generateNino()
  private val npsSummaryUrl: String = s"/individuals/${nino.withoutSuffix}/pensions/summary"
  private val npsLiabilitiesUrl: String = s"/individuals/${nino.withoutSuffix}/pensions/liabilities"
  private val npsNiRecordUrl: String = s"/individuals/${nino.withoutSuffix}/pensions/ni"
  private val checkPensionControllerUrl: String = s"/ni/$nino"

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
       |  }
       |}"""
      .stripMargin

  override def fakeApplication(): Application =
    GuiceApplicationBuilder().configure(
      "microservice.services.auth.port" -> server.port(),
      "microservice.services.nps-hod.host" -> "127.0.0.1",
      "microservice.services.nps-hod.port" -> server.port(),
      "microservice.services.des-hod.host" -> "127.0.0.1",
      "microservice.services.des-hod.port" -> server.port(),
      "microservice.services.if-hod.host" -> "127.0.0.1",
      "microservice.services.if-hod.port" -> server.port(),
      "play.ws.timeout.request" -> "1000ms",
      "play.ws.timeout.connection" -> "500ms",
      "auditing.enabled" -> false
    ).build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    stubPostServer(ok(generateAuthHeaderResponse), "/auth/authorise")
  }

  "get" must {

    List(
      notFound() -> NOT_FOUND -> "NOT_FOUND",
      gatewayTimeout() -> GATEWAY_TIMEOUT -> "GATEWAY_TIMEOUT",
      badGateway() -> BAD_GATEWAY -> "BAD_GATEWAY",
      badRequest() -> BAD_REQUEST -> "BAD_REQUEST",
      noOpenCopeWorkItem() -> FORBIDDEN -> "NO_OPEN_COPE_WORK_ITEM",
      closedCopeWorkItem() -> FORBIDDEN -> "CLOSED_COPE_WORK_ITEM",
      unauthorized() -> BAD_GATEWAY -> "BAD_GATEWAY from 4xx",
      serviceUnavailable() -> BAD_GATEWAY -> "BAD_GATEWAY from 5xx",
      httpClientTimeout(25000) -> INTERNAL_SERVER_ERROR -> "INTERNAL_SERVER_ERROR",
    ).foreach {case ((response, statusCode), errorDescription) =>

      s"return $statusCode $errorDescription" in {
        stubGetServer(response, npsSummaryUrl)
        stubGetServer(response, npsLiabilitiesUrl)
        stubGetServer(response, npsNiRecordUrl)

        val request = FakeRequest(GET, checkPensionControllerUrl)
          .withHeaders(defaultHeaders:_*)
        val result = route(app, request)

        result.map(statusResult) shouldBe Some(statusCode)
      }
    }
  }
}
