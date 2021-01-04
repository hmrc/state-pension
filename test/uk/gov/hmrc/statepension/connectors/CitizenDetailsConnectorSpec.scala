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

import com.codahale.metrics.Timer
import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.Mockito.{reset => mockReset}
import org.mockito.{Matchers, Mockito}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.LOCKED
import uk.gov.hmrc.http._
import uk.gov.hmrc.statepension.domain.nps.APIType
import uk.gov.hmrc.statepension.services.ApplicationMetrics
import uk.gov.hmrc.statepension.{StatePensionBaseSpec, WireMockHelper}

//TODO[Update Tests] return the correct payload
class CitizenDetailsConnectorSpec extends StatePensionBaseSpec with MockitoSugar with GuiceOneAppPerSuite with WireMockHelper {

  val nino = generateNino()
  val context = mock[Timer.Context]
  val url = s"/citizen-details/$nino/designatory-details/"
  val mockMetrics: ApplicationMetrics = mock[ApplicationMetrics](Mockito.RETURNS_DEEP_STUBS)

  override def beforeEach(): Unit = {
    super.beforeEach()
    mockReset(mockMetrics)
  }

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure("microservice.services.citizen-details.port" -> server.port())
    .overrides(
      bind[ApplicationMetrics].toInstance(mockMetrics)
    ).build()

  lazy val citizenDetailsConnector: CitizenDetailsConnector = app.injector.instanceOf[CitizenDetailsConnector]

  "CitizenDetailsConnector" should {
    "return OK status when successful" in {
      server.stubFor(
        get(urlEqualTo(url)).willReturn(ok())
      )

      val resultF = citizenDetailsConnector.connectToGetPersonDetails(nino)
      await(resultF) shouldBe 200

      withClue("timer did not stop") {
        Mockito.verify(mockMetrics.startTimer(Matchers.eq(APIType.CitizenDetails))).stop()
      }
    }

    "return 423 status when the Upstream is 423" in {
      server.stubFor(
        get(urlEqualTo(url)).willReturn(aResponse().withStatus(LOCKED))
      )

      val resultF = citizenDetailsConnector.connectToGetPersonDetails(nino)
      await(resultF) shouldBe LOCKED

      withClue("timer did not stop") {
        Mockito.verify(mockMetrics.startTimer(Matchers.eq(APIType.CitizenDetails))).stop()
      }
    }

    "return NotFoundException for invalid nino" in {
      server.stubFor(
        get(urlEqualTo(url)).willReturn(notFound())
      )

      val resultF = citizenDetailsConnector.connectToGetPersonDetails(nino)
      await(resultF.failed) shouldBe a[NotFoundException]

      withClue("timer did not stop") {
        Mockito.verify(mockMetrics.startTimer(Matchers.eq(APIType.CitizenDetails))).stop()
      }
    }

    "return 500 response code when the Upstream is 5XX" in {
      server.stubFor(
        get(urlEqualTo(url)).willReturn(serverError())
      )

      val resultF = citizenDetailsConnector.connectToGetPersonDetails(nino)
      await(resultF.failed) shouldBe a[Upstream5xxResponse]

      withClue("timer did not stop") {
        Mockito.verify(mockMetrics.startTimer(Matchers.eq(APIType.CitizenDetails))).stop()
      }
    }
  }
}
