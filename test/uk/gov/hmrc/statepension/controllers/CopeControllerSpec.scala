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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.FORBIDDEN
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.statepension.controllers.ErrorResponses.{CODE_COPE_PROCESSING, CODE_COPE_PROCESSING_FAILED}
import uk.gov.hmrc.statepension.controllers.auth.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.statepension.controllers.statepension.CopeController
import uk.gov.hmrc.statepension.services.CopeService
import utils.{CopeRepositoryHelper, StatePensionBaseSpec}

import java.time.LocalDate
import scala.concurrent.Future
import scala.util.Random

class CopeControllerSpec extends StatePensionBaseSpec with GuiceOneAppPerSuite with Injecting with CopeRepositoryHelper {

  val nino: Nino = new Generator(new Random()).nextNino
  val mockCopeService: CopeService = mock[CopeService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[CopeService].toInstance(mockCopeService),
      bind[AuthAction].to[FakeAuthAction]
    ).build()

  val controller = inject[CopeController]

  "get" should {
    "return Forbidden and Json payload" when {
      "Right cope processing is returned from copeService" in {
        val today = LocalDate.now()
        val copeProcessing = ErrorResponseCopeProcessing(CODE_COPE_PROCESSING, today, Some(today))

        when(mockCopeService.getCopeCase(any())).thenReturn(Future.successful(Some(copeProcessing)))

        val result = controller.get(nino)(FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json"))

        status(result) shouldBe FORBIDDEN
        contentAsJson(result) shouldBe Json.toJson(copeProcessing)
      }

      "Left cope failed is returned from copeService" in {
        val copeFailed = ErrorResponseCopeFailed(CODE_COPE_PROCESSING_FAILED)

        when(mockCopeService.getCopeCase(any())).thenReturn(Future.successful(Some(copeFailed)))

        val result = controller.get(nino)(FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json"))

        status(result) shouldBe FORBIDDEN
        contentAsJson(result) shouldBe Json.toJson(copeFailed)
      }
    }
    "Return NotFound with message" when {
      "None is returned form copeService" in {
        when(mockCopeService.getCopeCase(any())).thenReturn(Future.successful(None))

        val result = controller.get(nino)(FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json"))

        status(result) shouldBe NOT_FOUND
        contentAsString(result) shouldBe "User is not a cope case"
      }
    }
  }
}
