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

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.api.controllers.{ErrorGenericBadRequest, ErrorInternalServerError, ErrorNotFound, ErrorResponse}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.controllers.ErrorResponses.ExclusionCopeProcessing
import uk.gov.hmrc.statepension.controllers.ExclusionFormats._
import uk.gov.hmrc.statepension.models.CopeRecord
import uk.gov.hmrc.statepension.repositories.CopeProcessingRepository
import utils.StatePensionBaseSpec

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CopeErrorHandlingSpec extends StatePensionBaseSpec with GuiceOneAppPerSuite with Injecting {

  val nino: Nino = generateNino()
  val mockCopeRepository: CopeProcessingRepository = mock[CopeProcessingRepository]
  val appConfig: AppConfig = inject[AppConfig]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure("cope.feature.enabled" -> true)
    .overrides(bind[CopeProcessingRepository].toInstance(mockCopeRepository)).build()

  val copeErrorHandling: CopeErrorHandling = inject[CopeErrorHandling]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCopeRepository)
  }

  "errorWrapper" must {
    "return NotFound when NotFoundException is passed" in {
      List(
        new NotFoundException("NOT_FOUND"),
        UpstreamErrorResponse("NOT_FOUND", NOT_FOUND)
      ) foreach {
        exception =>
          val result = copeErrorHandling.errorWrapper(Future.failed(exception), nino)

          status(result) shouldBe 404
          contentAsJson(result) shouldBe Json.toJson[ErrorResponse](ErrorNotFound)
      }

    }

    "return GateWayTimeout when GatewayTimeoutException is passed" in {
      val result = copeErrorHandling.errorWrapper(Future.failed(UpstreamErrorResponse("GATEWAY_TIMEOUT", GATEWAY_TIMEOUT)), nino)

      status(result) shouldBe 504
    }

    "return BadGateway" when {
      List(
        UpstreamErrorResponse("4xx Response", 401, 502),
        UpstreamErrorResponse("5xx Response", 500, 500)
      ) foreach {
        exception =>
          s"${exception.getMessage} is passed" in {
            val result = copeErrorHandling.errorWrapper(Future.failed(exception), nino)

            status(result) shouldBe 502
          }
      }
    }

    "return Bad Request when BadRequestException is passed" in {
      val result = copeErrorHandling.errorWrapper(Future.failed(UpstreamErrorResponse("BAD_REQUEST", BAD_REQUEST)), nino)

      status(result) shouldBe 400
      contentAsJson(result) shouldBe Json.toJson(ErrorGenericBadRequest("Upstream Bad Request. Is this customer below State Pension Age?"))
    }

    "return Forbidden" when {
      "UpstreamErrorResponse is returned with 422 status and NO_OPEN_COPE_WORK_ITEM message from DES" when {

        "there is no CopeRecord in the database" when {
          "return ExclusionCopeProcessing and insert a CopeRecord to the db" in {
            when(mockCopeRepository.find(HashedNino(nino))).thenReturn(Future.successful(None))

            val result = copeErrorHandling.errorWrapper(Future.failed(UpstreamErrorResponse("NO_OPEN_COPE_WORK_ITEM", 422, 500)), nino)

            status(result) shouldBe 403
            contentAsJson(result) shouldBe Json.toJson(ExclusionCopeProcessing(appConfig))
            verify(mockCopeRepository, times(1))
              .insert(ArgumentMatchers.eq(CopeRecord(HashedNino(nino).generateHash()(appConfig), LocalDate.now(), LocalDate.now().plusWeeks(appConfig.returnToServiceWeeks))))
          }
        }

        "there is a CopeRecord in the database" when {
          "return ErrorResponseCopeProcessing when CopeAvailableDate from the DB entry is equal to the one from the AppConfig" in {
            when(mockCopeRepository.find(HashedNino(nino)))
              .thenReturn(Future.successful(Some(CopeRecord(nino.value, LocalDate.now(), LocalDate.now().plusWeeks(3)))))

            val result = copeErrorHandling.errorWrapper(Future.failed(UpstreamErrorResponse("NO_OPEN_COPE_WORK_ITEM", 422, 500)), nino)

            status(result) shouldBe 403
            contentAsJson(result) shouldBe Json.toJson(
              ErrorResponseCopeProcessing(
                ErrorResponses.CODE_COPE_PROCESSING,
                LocalDate.now().plusWeeks(appConfig.returnToServiceWeeks),
                Some(LocalDate.now().plusWeeks(3))
              )
            )
          }

          "the CopeAvailableDate from the DB entry is different to the one from the AppConfig" when {

            "the new CopeAvailableDate from DB entry is after the one from AppConfig, update DB CopeRecord and return ErrorResponseCopeProcessing" in {
              val initialLoginDate = LocalDate.now()

              when(mockCopeRepository.find(HashedNino(nino)))
                .thenReturn(Future.successful(Some(CopeRecord(nino.value, initialLoginDate, initialLoginDate.plusWeeks(123)))))

              val result = copeErrorHandling.errorWrapper(Future.failed(UpstreamErrorResponse("NO_OPEN_COPE_WORK_ITEM", 422, 500)), nino)

              status(result) shouldBe 403
              contentAsJson(result) shouldBe
                Json.toJson(
                  ErrorResponseCopeProcessing(
                    ErrorResponses.CODE_COPE_PROCESSING,
                    initialLoginDate.plusWeeks(appConfig.returnToServiceWeeks),
                    Some(initialLoginDate.plusWeeks(123))
                  )
                )

              verify(mockCopeRepository, times(1))
                .update(
                  ArgumentMatchers.eq(HashedNino(nino)),
                  ArgumentMatchers.eq(initialLoginDate.plusWeeks(appConfig.returnToServiceWeeks)),
                  ArgumentMatchers.eq(initialLoginDate.plusWeeks(123))
                )
            }

            "the new CopeAvailableDate from DB entry is before the one from AppConfig, update DB CopeRecord and return ErrorResponseCopeProcessing" in {
              val initialLoginDate = LocalDate.now()

              when(mockCopeRepository.find(HashedNino(nino)))
                .thenReturn(Future.successful(Some(CopeRecord(nino.value, initialLoginDate, initialLoginDate.plusWeeks(1)))))

              val result = copeErrorHandling.errorWrapper(Future.failed(UpstreamErrorResponse("NO_OPEN_COPE_WORK_ITEM", 422, 500)), nino)

              status(result) shouldBe 403
              contentAsJson(result) shouldBe
                Json.toJson(
                  ErrorResponseCopeProcessing(
                    ErrorResponses.CODE_COPE_PROCESSING,
                    initialLoginDate.plusWeeks(appConfig.returnToServiceWeeks),
                    Some(initialLoginDate.plusWeeks(1))
                  )
                )

              verify(mockCopeRepository, times(1))
                .update(
                  ArgumentMatchers.eq(HashedNino(nino)),
                  ArgumentMatchers.eq(initialLoginDate.plusWeeks(appConfig.returnToServiceWeeks)),
                  ArgumentMatchers.eq(initialLoginDate.plusWeeks(1))
                )
            }

          }

        }
      }

      "UpstreamErrorResponse is returned with 422 status and CLOSED_COPE_WORK_ITEM message from DES" in {

        when(mockCopeRepository.delete(HashedNino(nino))).thenReturn(Future.successful(CopeRecord("Nino", LocalDate.now(), LocalDate.now())))
        
        val result = copeErrorHandling.errorWrapper(Future.failed(UpstreamErrorResponse("CLOSED_COPE_WORK_ITEM", 422, 500)), nino)

        status(result) shouldBe 403
        contentAsJson(result) shouldBe Json.toJson[ErrorResponseCopeFailed](ErrorResponses.ExclusionCopeProcessingFailed)
      }
    }

    "return InternalServerError when Throwable that isn't matched is passed" in {
      val result = copeErrorHandling.errorWrapper(Future.failed(new UnauthorizedException("Unauthorized")), nino)

      status(result) shouldBe 500
      contentAsJson(result) shouldBe Json.toJson(ErrorInternalServerError)
    }
  }

}
