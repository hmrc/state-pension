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

package uk.gov.hmrc.statepension.services

import org.mockito.Matchers
import org.mockito.Mockito.when
import play.api.http.Status.{LOCKED, OK}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.statepension.connectors.CitizenDetailsConnector

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

//TODO change to proper extensions
class CitizenDetailsServiceSpec extends StatePensionServiceSpec {

  val nino: Nino = generateNino()
  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val citizenDetailsService: CitizenDetailsService = new CitizenDetailsService {
    override val citizenDetailsConnector: CitizenDetailsConnector = mockCitizenDetailsConnector
  }

  "CitizenDetailsService" should {
    "return ManualCorrespondenceIndicator status is false when Response is 200" in {
      when(mockCitizenDetailsConnector.connectToGetPersonDetails(Matchers.any())(Matchers.any())).thenReturn(
        Future.successful(OK)
      )
      val resultF = citizenDetailsService.checkManualCorrespondenceIndicator(nino)(hc)
      await(resultF) shouldBe false
    }
    "return ManualCorrespondenceIndicator status is true when Response is 423" in {
      when(mockCitizenDetailsConnector.connectToGetPersonDetails(Matchers.any())(Matchers.any())) thenReturn
        Future.successful(LOCKED)

      val resultF = citizenDetailsService.checkManualCorrespondenceIndicator(nino)(hc)
      await(resultF) shouldBe true
    }
  }
}
