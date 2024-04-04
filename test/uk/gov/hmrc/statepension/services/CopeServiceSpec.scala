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

package uk.gov.hmrc.statepension.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.statepension.controllers.{ErrorResponseCopeFailed, ErrorResponseCopeProcessing}
import uk.gov.hmrc.statepension.controllers.ErrorResponses.{CODE_COPE_PROCESSING, CODE_COPE_PROCESSING_FAILED}
import uk.gov.hmrc.statepension.models.CopeRecord
import uk.gov.hmrc.statepension.repositories.{CopeFailedCache, CopeProcessingRepository}
import utils.StatePensionBaseSpec

import java.time.LocalDate
import scala.concurrent.Future

class CopeServiceSpec extends StatePensionBaseSpec {

  val nino: Nino = generateNino()

  val mockCopeProcessingRepository: CopeProcessingRepository = mock[CopeProcessingRepository]
  val mockCopeFailedCache: CopeFailedCache = mock[CopeFailedCache]

  val sut: CopeService = GuiceApplicationBuilder()
    .overrides(
      bind[CopeProcessingRepository].toInstance(mockCopeProcessingRepository),
      bind[CopeFailedCache].toInstance(mockCopeFailedCache)
    )
    .injector()
    .instanceOf[CopeService]


  "getCopeCase" should {
    "return a Right CopeRecord when a record is found from CopeProcessing Repository" in {
      val today: LocalDate = LocalDate.now

      when(mockCopeFailedCache.get(any())).thenReturn(Future.successful(None))
      when(mockCopeProcessingRepository.find(any())).thenReturn(Future.successful(Some(CopeRecord("Nino", today, today))))

      val result = await(sut.getCopeCase(nino))

      result shouldBe Some(ErrorResponseCopeProcessing(CODE_COPE_PROCESSING, today, None))
    }

    "return a Left CopeFailed when a record is found in CopeFailedCache" in {
      when(mockCopeFailedCache.get(any())).thenReturn(Future.successful(Some("HashedNino")))
      when(mockCopeProcessingRepository.find(any())).thenReturn(Future.successful(None))

      val result = await(sut.getCopeCase(nino))

      result shouldBe Some(ErrorResponseCopeFailed(CODE_COPE_PROCESSING_FAILED))
    }

    "return None when both cache and repository return None" in {
      when(mockCopeFailedCache.get(any())).thenReturn(Future.successful(None))
      when(mockCopeProcessingRepository.find(any())).thenReturn(Future.successful(None))

      val result = await(sut.getCopeCase(nino))

      result shouldBe None
    }
  }
}
