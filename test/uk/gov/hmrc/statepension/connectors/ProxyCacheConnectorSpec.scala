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

package uk.gov.hmrc.statepension.connectors

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.Fault
import org.scalatest.RecoverMethods
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HeaderNames, RequestId, UpstreamErrorResponse}
import uk.gov.hmrc.statepension.config.AppConfig
import utils.TestData._
import utils.{StatePensionBaseSpec, WireMockHelper}

import scala.concurrent.ExecutionContext.Implicits.global

class ProxyCacheConnectorSpec
  extends StatePensionBaseSpec
    with GuiceOneAppPerSuite
    with WireMockHelper
    with ScalaFutures
    with RecoverMethods {

  server.start()

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.ni-and-sp-proxy-cache.host" -> "127.0.0.1",
      "microservice.services.ni-and-sp-proxy-cache.port" -> server.port(),
      "auditing.enabled" -> false
    )
    .build()

  private val connector: ProxyCacheConnector =
    app.injector.instanceOf[ProxyCacheConnector]
  private val appConfig: AppConfig =
    app.injector.instanceOf[AppConfig]
  private val connectorUtil: ConnectorUtil =
    app.injector.instanceOf[ConnectorUtil]

  override val headerCarrier: HeaderCarrier =
    HeaderCarrier(
      authorization = Some(Authorization(appConfig.internalAuthToken)),
      requestId     = Some(RequestId("requestId"))
    )

  private val nino: Nino =
    generateNino()

  private val url: String =
    s"/ni-and-sp-proxy-cache/${nino.nino}"

  private val requests: Seq[(ResponseDefinitionBuilder, String)] = Seq(
    serverError() -> "internalServerError",
    badRequest() -> "badRequest",
    aResponse().withStatus(502) -> "gatewayTimeout",
    serviceUnavailable() -> "serviceUnavailable"
  )

  "getProxyCacheData success" should {

    "return Right(ProxyCacheData) when json can be parsed" in {
      server.stubFor(get(urlEqualTo(url))
        .willReturn(ok(proxyCacheDataJson)))

      val result = await(connector.get(nino)(headerCarrier))

      result.liabilities shouldBe liabilities
      result.summary shouldBe summary
      result.niRecord shouldBe niRecord
    }
  }

  "getProxyCacheData failure" should {
    requests.foreach {
      case (errorResponse, description) =>

      s"return Left(UpstreamErrorResponse) for $description" in {
        server.stubFor(get(urlEqualTo(url))
          .willReturn(errorResponse))

        await(connector.get(nino)(headerCarrier).failed) shouldBe a[UpstreamErrorResponse]
      }
    }

    "return default Left(JsonValidationException)" in {
      server.stubFor(get(urlEqualTo(url))
        .willReturn(ok("""{"number": 456, "name": "def"}""")))

      val ex: Exception =
        await(recoverToExceptionIf[Exception](connector.get(nino)(headerCarrier)))

      ex shouldBe a[connectorUtil.JsonValidationException]
    }

    "return default Left(Exception)" in {
      server.stubFor(get(urlEqualTo(url))
        .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)))

      val ex: Exception =
        await(recoverToExceptionIf[Exception](connector.get(nino)(headerCarrier)))

      ex shouldBe a[Exception]
    }
  }

  "headers" should {
    "be present" in {

      server.stubFor(get(urlEqualTo(url))
        .willReturn(ok(proxyCacheDataJson)))

      await(connector.get(nino)(headerCarrier))

      server.verify(getRequestedFor(urlEqualTo(url))
        .withHeader(HeaderNames.authorisation, equalTo(appConfig.internalAuthToken))
        .withHeader("Originator-Id", equalTo("DA_PF"))
        .withHeader("CorrelationId", matching("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}"))
        .withHeader(HeaderNames.xRequestId, equalTo("requestId"))
      )
    }
  }
}
