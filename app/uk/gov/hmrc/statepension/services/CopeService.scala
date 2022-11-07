/*
 * Copyright 2022 HM Revenue & Customs
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

import com.google.inject.Inject
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.statepension.controllers.ErrorResponses.ExclusionCopeProcessingFailed
import uk.gov.hmrc.statepension.controllers.{ErrorResponseCopeFailed, HashedNino}
import uk.gov.hmrc.statepension.models.CopeRecord
import uk.gov.hmrc.statepension.repositories.{CopeFailedCache, CopeProcessingRepository}

import scala.concurrent.{ExecutionContext, Future}

class CopeService @Inject()(copeRepository: CopeProcessingRepository, copeFailedCache: CopeFailedCache)(implicit ec: ExecutionContext) {


  def getCopeCase(nino: Nino): Future[Option[Either[ErrorResponseCopeFailed, CopeRecord]]] = {
    for {
      copeProcessing <- copeRepository.find(HashedNino(nino))
      copeFailed <- copeFailedCache.get(HashedNino(nino))
    } yield {
      (copeProcessing, copeFailed) match {
        case (Some(processing), None) => Some(Right(processing))
        case (None, Some(_)) => Some(Left(ExclusionCopeProcessingFailed))
        case _ => None
      }
    }
  }
}
