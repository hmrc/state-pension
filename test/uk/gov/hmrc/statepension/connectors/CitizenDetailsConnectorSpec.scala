/*
 * Copyright 2019 HM Revenue & Customs
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
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeRequest
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http._
import uk.gov.hmrc.statepension.services.ApplicationMetrics
import uk.gov.hmrc.statepension.{StatePensionUnitSpec, WSHttp}

import scala.concurrent.Future

class CitizenDetailsConnectorSpec extends StatePensionUnitSpec with MockitoSugar with OneAppPerSuite{

  val nino = generateNino()
  lazy val fakeRequest = FakeRequest()
  implicit val hc = HeaderCarrier()

  val http: WSHttp = mock[WSHttp]
  val metrics: ApplicationMetrics = mock[ApplicationMetrics]
  val environment: Environment = app.injector.instanceOf[Environment]
  val configuration: Configuration = app.injector.instanceOf[Configuration]
  
  val citizenDetailsConnector = new CitizenDetailsConnector(http, metrics, environment, configuration) {
    override val serviceUrl: String = "/"
  }

  val context = mock[Timer.Context]
  when(context.stop()).thenReturn(0)
  when(metrics.startTimer(Matchers.any())).thenReturn{ context }

  "CitizenDetailsConnector" should {

    "return OK status when successful" in {
      when(http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(200))
      val resultF = citizenDetailsConnector.connectToGetPersonDetails(nino)(hc)
      await(resultF) shouldBe 200
    }

    "return 423 status when the Upstream is 423" in {
      when(http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn Future.failed(new Upstream4xxResponse(":(", 423, 423, Map()))
      val resultF = citizenDetailsConnector.connectToGetPersonDetails(nino)(hc)
      await(resultF) shouldBe 423
    }

    "return NotFoundException for invalid nino" in {
      when(http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn Future.failed(new NotFoundException("Not Found"))
      val resultF = citizenDetailsConnector.connectToGetPersonDetails(nino)(hc)
      await(resultF.failed) shouldBe a [NotFoundException]
    }

    "return 500 response code when there is Upstream is 4XX" in {
      when(http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn Future.failed(new InternalServerException("InternalServerError"))
      val resultF = citizenDetailsConnector.connectToGetPersonDetails(nino)(hc)
      await(resultF.failed) shouldBe a [InternalServerException]
    }
  }

}
