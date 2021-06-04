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

package uk.gov.hmrc.statepension.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, RequestId, SessionId, UpstreamErrorResponse}
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.fixtures.{LiabilitiesFixture, NIRecordFixture, SummaryFixture}
import uk.gov.hmrc.statepension.services.ApplicationMetrics
import uk.gov.hmrc.statepension.{NinoGenerator, WireMockHelper}

class IfConnectorSpec extends PlaySpec
  with MockitoSugar
  with ScalaFutures
  with IntegrationPatience
  with NinoGenerator
  with WireMockHelper
  with BeforeAndAfterEach {

  val mockAppContext: AppConfig = mock[AppConfig](Mockito.RETURNS_DEEP_STUBS)
  val mockApplicationMetrics: ApplicationMetrics = mock[ApplicationMetrics](Mockito.RETURNS_DEEP_STUBS)

  lazy val ifConnector: IfConnector = GuiceApplicationBuilder()
    .overrides(
      bind[AppConfig].toInstance(mockAppContext),
      bind[ApplicationMetrics].toInstance(mockApplicationMetrics)
    ).injector().instanceOf[IfConnector]

  val originatorIdKey: String = "originatorIdKey"
  val ifOriginatorIdValue: String = "ifOriginatorIdValue"
  val ifEnvironment: String = "ifEnvironment"
  val ifToken: String = "ifToken"

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockAppContext.ifConnectorConfig.serviceUrl).thenReturn(s"http://localhost:${server.port()}")
    when(mockAppContext.ifConnectorConfig.serviceOriginatorIdKey).thenReturn(originatorIdKey)
    when(mockAppContext.ifConnectorConfig.serviceOriginatorIdValue).thenReturn(ifOriginatorIdValue)
    when(mockAppContext.ifConnectorConfig.environment).thenReturn(ifEnvironment)
    when(mockAppContext.ifConnectorConfig.authorizationToken).thenReturn(ifToken)
  }

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("testSessionId")),
    requestId = Some(RequestId("testRequestId")))
  val nino: Nino = generateNino()

  def stub(url: String, status: Int, body: String): StubMapping = server.stubFor(
    get(urlEqualTo(url))
      .willReturn(
        aResponse
          .withStatus(status)
          .withBody(body)
      )
  )

  "getSummary" must {
    def stubGetSummary(status: Int = OK, body: String = "{}"): StubMapping =
      stub(s"/individuals/state-pensions/nino/${nino.withoutSuffix}/summary", status, body)
    "make a request to the correct URI with Environment, serviceOriginatorId and Authorization headers" in {
      stubGetSummary(body = SummaryFixture.exampleSummaryJson)

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
        stubGetSummary(body = SummaryFixture.exampleSummaryJson)

        ifConnector.getSummary(nino).futureValue mustBe SummaryFixture.exampleSummary
      }
    }

    "return JsonValidationException" when {
      "response json is invalid" in {
        stubGetSummary()

        val thrown: Throwable = ifConnector.getSummary(nino).failed.futureValue

        assert(thrown.isInstanceOf[ifConnector.JsonValidationException])
      }
    }

    "return UpstreamErrorResponse" when {
      "response status is UpstreamErrorResponse" in {
        stubGetSummary(INTERNAL_SERVER_ERROR)

        val thrown: Throwable = ifConnector.getSummary(nino).failed.futureValue

        assert(thrown.isInstanceOf[UpstreamErrorResponse])
      }
    }
  }

  "getLiabilities" must {
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

        ifConnector.getLiabilities(nino).futureValue mustBe LiabilitiesFixture.exampleLiabilities
      }
    }

    "return JsonValidationException" when {
      "response json is invalid" in {
        stubLiabilities(body = """{"liabilities": {}}""")

        val thrown: Throwable = ifConnector.getLiabilities(nino).failed.futureValue

        assert(thrown.isInstanceOf[ifConnector.JsonValidationException])
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

  "getNIRecord" must {
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

        ifConnector.getNIRecord(nino).futureValue mustBe NIRecordFixture.exampleDesNiRecord
      }
    }

    "return JsonValidationException" when {
      "response json is invalid" in {
        stubNiRecord(body = """{"taxYears":{}}""")

        val thrown: Throwable = ifConnector.getNIRecord(nino).failed.futureValue

        assert(thrown.isInstanceOf[ifConnector.JsonValidationException])
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
