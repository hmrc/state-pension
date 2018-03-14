/*
 * Copyright 2018 HM Revenue & Customs
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
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.domain.nps.APIType
import uk.gov.hmrc.statepension.services.Metrics

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet, HttpResponse, InternalServerException, NotFoundException, Upstream4xxResponse }

class CitizenDetailsConnectorSpec extends StatePensionUnitSpec with MockitoSugar {

  val nino = generateNino()
  lazy val fakeRequest = FakeRequest()
  implicit val hc = HeaderCarrier()
  val citizenDetailsConnector = new CitizenDetailsConnector {
    override val serviceUrl: String = "/"
    override val http: HttpGet = mock[HttpGet]
    override val metrics: Metrics = mock[Metrics]
  }

  val context = mock[Timer.Context]
  when(context.stop()).thenReturn(0)
  when(citizenDetailsConnector.metrics.startTimer(Matchers.any())).thenReturn{ context }

  "CitizenDetailsConnector" should {

    "return OK status when successful" in {
      when(citizenDetailsConnector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(200))
      val resultF = citizenDetailsConnector.connectToGetPersonDetails(nino)(hc)
      await(resultF) shouldBe 200
    }

    "return 423 status when the Upstream is 423" in {
      when(citizenDetailsConnector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn Future.failed(new Upstream4xxResponse(":(", 423, 423, Map()))
      val resultF = citizenDetailsConnector.connectToGetPersonDetails(nino)(hc)
      await(resultF) shouldBe 423
    }

    "return NotFoundException for invalid nino" in {
      when(citizenDetailsConnector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn Future.failed(new NotFoundException("Not Found"))
      val resultF = citizenDetailsConnector.connectToGetPersonDetails(nino)(hc)
      await(resultF.failed) shouldBe a [NotFoundException]
    }

    "return 500 response code when there is Upstream is 4XX" in {
      when(citizenDetailsConnector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn Future.failed(new InternalServerException("InternalServerError"))
      val resultF = citizenDetailsConnector.connectToGetPersonDetails(nino)(hc)
      await(resultF.failed) shouldBe a [InternalServerException]
    }
  }

}
