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

import com.google.inject.{ImplementedBy, Inject}

import java.time.LocalDate
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Result}
import uk.gov.hmrc.api.controllers.{ErrorGenericBadRequest, ErrorInternalServerError, ErrorNotFound, ErrorResponse}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.UpstreamErrorResponse.{Upstream4xxResponse, Upstream5xxResponse, WithStatusCode}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.controllers.ExclusionFormats._
import uk.gov.hmrc.statepension.models.CopeRecord
import uk.gov.hmrc.statepension.repositories.{CopeFailedCache, CopeProcessingRepository}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[CopeErrorHandling])
trait ErrorHandling {
  self: BackendController =>

  val app: String = "State-Pension"

  def errorWrapper(func: => Future[Result], nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result]
}

class CopeErrorHandling @Inject()(cc: ControllerComponents, appConfig: AppConfig, copeRepository: CopeProcessingRepository, copeFailedCache: CopeFailedCache) extends BackendController(cc)
  with ErrorHandling with Logging {

  override def errorWrapper(func: => Future[Result], nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    func.recoverWith {
      case WithStatusCode(NOT_FOUND) => Future.successful(NotFound(Json.toJson[ErrorResponse](ErrorNotFound)))
      case _: NotFoundException => Future.successful(NotFound(Json.toJson[ErrorResponse](ErrorNotFound)))
      case e@WithStatusCode(GATEWAY_TIMEOUT) => logger.error(s"$app Gateway Timeout: ${e.getMessage}", e); Future.successful(GatewayTimeout)
      case e@WithStatusCode(BAD_GATEWAY) => logger.error(s"$app Bad Gateway: ${e.getMessage}", e); Future.successful(BadGateway)
      case WithStatusCode(BAD_REQUEST) =>
        Future.successful(BadRequest(Json.toJson(ErrorGenericBadRequest("Upstream Bad Request. Is this customer below State Pension Age?"))))
      case e@WithStatusCode(UNPROCESSABLE_ENTITY) if e.message.contains("NO_OPEN_COPE_WORK_ITEM") => defineCopeResponse(nino)
      case e@WithStatusCode(UNPROCESSABLE_ENTITY) if  e.message.contains("CLOSED_COPE_WORK_ITEM") =>
        copeFailedCache.insert(HashedNino(nino)) flatMap {
          _ =>
            copeRepository.delete(HashedNino(nino)) map { _ =>
              Forbidden(Json.toJson[ErrorResponseCopeFailed](ErrorResponses.ExclusionCopeProcessingFailed))
            }
        }
      case Upstream4xxResponse(e) => logger.error(s"$app Upstream4XX: ${e.getMessage}", e); Future.successful(BadGateway)
      case Upstream5xxResponse(e) => logger.error(s"$app Upstream5XX: ${e.getMessage}", e); Future.successful(BadGateway)
      case e: Throwable =>
        logger.error(s"$app Internal server error: ${e.getMessage}", e)
        Future.successful(InternalServerError(Json.toJson(ErrorInternalServerError)))
    }
  }

  private def defineCopeResponse(nino: Nino)(implicit ec: ExecutionContext): Future[Result] = {
    val today = LocalDate.now()

    copeRepository.find(HashedNino(nino)) map {
      case None => {
        copeRepository.insert(CopeRecord(HashedNino(nino).generateHash()(appConfig), today, today.plusWeeks(appConfig.returnToServiceWeeks), None))
        Forbidden(Json.toJson(ErrorResponses.ExclusionCopeProcessing(appConfig)))
      }
      case Some(entry) => {
        val availableDateFromEntry: LocalDate = entry.copeAvailableDate
        val availableDateFromConfig: LocalDate = entry.firstLoginDate.plusWeeks(appConfig.returnToServiceWeeks)

        if(availableDateFromEntry.isEqual(availableDateFromConfig)) {
          Forbidden(Json.toJson(ErrorResponseCopeProcessing(ErrorResponses.CODE_COPE_PROCESSING, entry.copeAvailableDate, entry.previousCopeAvailableDate)))
        } else {
          copeRepository.update(
            HashedNino(nino), entry.firstLoginDate.plusWeeks(appConfig.returnToServiceWeeks), entry.copeAvailableDate)
          Forbidden(Json.toJson(
            ErrorResponseCopeProcessing(
              ErrorResponses.CODE_COPE_PROCESSING,
              entry.firstLoginDate.plusWeeks(appConfig.returnToServiceWeeks),
              Some(entry.copeAvailableDate)
            )
          ))
        }

      }
    }
  }
}
