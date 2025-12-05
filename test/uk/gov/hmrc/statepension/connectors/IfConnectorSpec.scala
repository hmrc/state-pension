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

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.mockito.Mockito
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Injector, bind}
import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, RequestId, SessionId, UpstreamErrorResponse}
import uk.gov.hmrc.statepension.fixtures.{LiabilitiesFixture, NIRecordFixture, SummaryFixture}
import uk.gov.hmrc.statepension.services.ApplicationMetrics
import utils.{GenerateNino, StatePensionBaseSpec, WireMockHelper}

class IfConnectorSpec
  extends StatePensionBaseSpec
    with ScalaFutures
    with IntegrationPatience
    with GenerateNino
    with WireMockHelper {

  val mockApplicationMetrics: ApplicationMetrics = mock[ApplicationMetrics](Mockito.RETURNS_DEEP_STUBS)

  server.start()

  val originatorIdKey: String = "originatorIdKey"
  val ifOriginatorIdValue: String = "ifOriginatorIdValue"
  val ifEnvironment: String = "ifEnvironment"
  val ifToken: String = "ifToken"

  lazy val injector: Injector = GuiceApplicationBuilder()
    .configure(
      "microservice.services.if-hod.port" -> server.port(),
      "microservice.services.if-hod.token" -> ifToken,
      "microservice.services.if-hod.originatoridkey" -> originatorIdKey,
      "microservice.services.if-hod.originatoridvalue" -> ifOriginatorIdValue,
      "microservice.services.if-hod.environment" -> ifEnvironment,
      "internal-auth.isTestOnlyEndpoint" -> false
    )
    .overrides(
      bind[ApplicationMetrics].toInstance(mockApplicationMetrics)
    ).injector()

  val ifConnector: IfConnector = injector.instanceOf[IfConnector]
  val connectorUtil: ConnectorUtil = injector.instanceOf[ConnectorUtil]

  implicit val hc: HeaderCarrier =
    HeaderCarrier(
      sessionId = Some(SessionId("testSessionId")),
      requestId = Some(RequestId("testRequestId"))
    )
  val nino: Nino = generateNino()

  def stub(url: String, status: Int, body: String): StubMapping = server.stubFor(
    get(urlEqualTo(url))
      .willReturn(
        aResponse
          .withStatus(status)
          .withBody(body)
      )
  )

  "getSummary" should {
    def stubSummary(status: Int = OK, body: String = "{}"): StubMapping =
      stub(s"/individuals/state-pensions/nino/${nino.withoutSuffix}/summary", status, body)
    "make a request to the correct URI with Environment, serviceOriginatorId and Authorization headers" in {
      stubSummary(body = SummaryFixture.exampleSummaryJson)

      ifConnector.getSummary(nino).futureValue

      server.verify(1,
        getRequestedFor(urlEqualTo(s"/individuals/state-pensions/nino/${nino.withoutSuffix}/summary"))
          .withHeader("Authorization", equalTo(s"Bearer $ifToken"))
          .withHeader(originatorIdKey, equalTo(ifOriginatorIdValue))
          .withHeader("Environment", equalTo(ifEnvironment))
          .withHeader("X-Request-ID", equalTo("testRequestId"))
          .withHeader("X-Session-ID", equalTo("testSessionId"))
      )
    }

    "return the response object" when {
      "response json is valid" in {
        stubSummary(body = SummaryFixture.exampleSummaryJson)

        ifConnector.getSummary(nino).futureValue shouldBe SummaryFixture.exampleSummary
      }
    }

    "return JsonValidationException" when {
      "response json is invalid" in {
        stubSummary()

        val thrown: Throwable = ifConnector.getSummary(nino).failed.futureValue

        assert(thrown.isInstanceOf[connectorUtil.JsonValidationException])
      }
    }

    "return UpstreamErrorResponse" when {
      "response status is UpstreamErrorResponse" in {
        stubSummary(INTERNAL_SERVER_ERROR)

        val thrown: Throwable = ifConnector.getSummary(nino).failed.futureValue

        assert(thrown.isInstanceOf[UpstreamErrorResponse])
      }
    }
  }

  "getLiabilities" should {
    def stubLiabilities(status: Int = OK, body: String = "{}"): StubMapping =
      stub(s"/individuals/state-pensions/nino/${nino.withoutSuffix}/liabilities", status, body)

    "make a request to the correct URI with Environment, serviceOriginatorId and Authorization headers" in {
      stubLiabilities(body = LiabilitiesFixture.exampleLiabilitiesJson(nino.nino))

      ifConnector.getLiabilities(nino).futureValue

      server.verify(1,
        getRequestedFor(urlEqualTo(s"/individuals/state-pensions/nino/${nino.withoutSuffix}/liabilities"))
          .withHeader("Authorization", equalTo(s"Bearer $ifToken"))
          .withHeader(originatorIdKey, equalTo(ifOriginatorIdValue))
          .withHeader("Environment", equalTo(ifEnvironment))
          .withHeader("X-Request-ID", equalTo("testRequestId"))
          .withHeader("X-Session-ID", equalTo("testSessionId"))
      )
    }

    "return the response object" when {
      "response json is valid" in {
        stubLiabilities(body = LiabilitiesFixture.exampleLiabilitiesJson(nino.nino))

        ifConnector.getLiabilities(nino).futureValue shouldBe LiabilitiesFixture.exampleLiabilities
      }
    }

    "return JsonValidationException" when {
      "response json is invalid" in {
        stubLiabilities(body = """{"liabilities": {}}""")

        val thrown: Throwable = ifConnector.getLiabilities(nino).failed.futureValue

        assert(thrown.isInstanceOf[connectorUtil.JsonValidationException])
      }
    }

    "return UpstreamErrorResponse" when {
      "response status is UpstreamErrorResponse" in {
        stubLiabilities(status = INTERNAL_SERVER_ERROR)

        val thrown: Throwable = ifConnector.getLiabilities(nino).failed.futureValue

        assert(thrown.isInstanceOf[UpstreamErrorResponse])
      }
    }
  }

  "getNIRecord" should {
    def stubNiRecord(status: Int = OK, body: String = "{}"): StubMapping =
      stub(s"/individuals/state-pensions/nino/${nino.withoutSuffix}/ni-details", status, body)
    "make a request to the correct URI with Environment, serviceOriginatorId and Authorization headers" in {
      stubNiRecord(body = NIRecordFixture.exampleDesNiRecordJson(nino.nino).stripMargin)

      ifConnector.getNIRecord(nino).futureValue

      server.verify(1,
        getRequestedFor(urlEqualTo(s"/individuals/state-pensions/nino/${nino.withoutSuffix}/ni-details"))
          .withHeader("Authorization", equalTo(s"Bearer $ifToken"))
          .withHeader(originatorIdKey, equalTo(ifOriginatorIdValue))
          .withHeader("Environment", equalTo(ifEnvironment))
          .withHeader("X-Request-ID", equalTo("testRequestId"))
          .withHeader("X-Session-ID", equalTo("testSessionId")))
    }

    "return the response object" when {
      "response json is valid" in {
        stubNiRecord(body = NIRecordFixture.exampleDesNiRecordJson(nino.nino).stripMargin)

        ifConnector.getNIRecord(nino).futureValue shouldBe NIRecordFixture.exampleDesNiRecord
      }
    }

    "return JsonValidationException" when {
      "response json is invalid" in {
        stubNiRecord(body = """{"taxYears":{}}""")

        val thrown: Throwable = ifConnector.getNIRecord(nino).failed.futureValue

        assert(thrown.isInstanceOf[connectorUtil.JsonValidationException])
      }
    }

    "return UpstreamErrorResponse" when {
      "response status is UpstreamErrorResponse" in {
        stubNiRecord(status = INTERNAL_SERVER_ERROR)

        val thrown: Throwable = ifConnector.getNIRecord(nino).failed.futureValue

        assert(thrown.isInstanceOf[UpstreamErrorResponse])
      }
    }
  }
}
