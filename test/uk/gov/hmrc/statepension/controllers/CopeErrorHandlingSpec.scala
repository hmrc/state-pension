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

package uk.gov.hmrc.statepension.controllers

import org.scalatest.Matchers._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.api.controllers.{ErrorGenericBadRequest, ErrorInternalServerError, ErrorNotFound, ErrorResponse}
import uk.gov.hmrc.http._
import uk.gov.hmrc.statepension.StatePensionBaseSpec
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.controllers.ErrorResponses.ExclusionCopeProcessing
import uk.gov.hmrc.statepension.controllers.ExclusionFormats._

import scala.concurrent.Future

class CopeErrorHandlingSpec extends StatePensionBaseSpec with GuiceOneAppPerSuite with Injecting with MockitoSugar {

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure("cope.feature.enabled" -> true).build()

  val copeErrorHandling = inject[CopeErrorHandling]

  "errorWrapper" must {
    "return NotFound when NotFoundException is passed" in  {
      val result = copeErrorHandling.errorWrapper(Future.failed(Upstream4xxResponse("NOT_FOUND", 404, 404)))

      status(result) shouldBe 404
      contentAsJson(result) shouldBe Json.toJson[ErrorResponse](ErrorNotFound)
    }

    "return GateWayTimeout when GatewayTimeoutException is passed" in  {
      val result = copeErrorHandling.errorWrapper(Future.failed(new GatewayTimeoutException("Gateway Timeout")))

      status(result) shouldBe 504
    }

    "return BadGateway" when {
      List(new BadGatewayException("BadGateway"),
        new Upstream4xxResponse("4xx Response", 401, 502),
        new Upstream5xxResponse("5xx Response", 500, 500)
      ) foreach {
        exception =>
          s"${exception.getMessage} is passed" in  {
            val result = copeErrorHandling.errorWrapper(Future.failed(exception))

            status(result) shouldBe 502
          }
      }
    }

    "return Bad Request when BadRequestException is passed" in  {
      val result = copeErrorHandling.errorWrapper(Future.failed(new BadRequestException("Bad Request")))

      status(result) shouldBe 400
      contentAsJson(result) shouldBe Json.toJson(ErrorGenericBadRequest("Upstream Bad Request. Is this customer below State Pension Age?"))
    }

    "return Forbidden" when {
      "Upstream4xxResponse is returned with 422 status and NO_OPEN_COPE_WORK_ITEM message from DES" in  {
        val result = copeErrorHandling.errorWrapper(Future.failed(new Upstream4xxResponse("NO_OPEN_COPE_WORK_ITEM", 422, 500)))

        status(result) shouldBe 403
        contentAsJson(result) shouldBe Json.toJson(ExclusionCopeProcessing(inject[AppConfig]))
      }

      "Upstream4xxResponse is returned with 422 status and CLOSED_COPE_WORK_ITEM message from DES" in  {
        val result = copeErrorHandling.errorWrapper(Future.failed(new Upstream4xxResponse("CLOSED_COPE_WORK_ITEM", 422, 500)))

        status(result) shouldBe 403
        contentAsJson(result) shouldBe Json.toJson[ErrorResponseCopeFailed](ErrorResponses.ExclusionCopeProcessingFailed)
      }
    }

    "return InternalServerError when Throwable that isn't matched is passed" in  {
      val result = copeErrorHandling.errorWrapper(Future.failed(new UnauthorizedException("Unauthorized")))

      status(result) shouldBe 500
      contentAsJson(result) shouldBe Json.toJson(ErrorInternalServerError)
    }
  }

}
