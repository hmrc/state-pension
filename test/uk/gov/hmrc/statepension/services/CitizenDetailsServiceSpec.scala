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

package uk.gov.hmrc.statepension.services

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.{LOCKED, OK}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.statepension.StatePensionBaseSpec
import uk.gov.hmrc.statepension.connectors.CitizenDetailsConnector

import scala.concurrent.Future

class CitizenDetailsServiceSpec extends StatePensionBaseSpec {

  val nino: Nino = generateNino()
  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  lazy val citizenDetailsService: CitizenDetailsService = new CitizenDetailsService(citizenDetailsConnector = mockCitizenDetailsConnector)

  "CitizenDetailsService" should {
    "return ManualCorrespondenceIndicator status is false when Response is 200" in {
      when(mockCitizenDetailsConnector.connectToGetPersonDetails(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(
        Future.successful(OK)
      )
      val resultF = citizenDetailsService.checkManualCorrespondenceIndicator(nino)(hc)
      resultF.futureValue shouldBe false
    }
    "return ManualCorrespondenceIndicator status is true when Response is 423" in {
      when(mockCitizenDetailsConnector.connectToGetPersonDetails(ArgumentMatchers.any())(ArgumentMatchers.any())) thenReturn
        Future.successful(LOCKED)

      val resultF = citizenDetailsService.checkManualCorrespondenceIndicator(nino)(hc)
      resultF.futureValue shouldBe true
    }
  }
}
