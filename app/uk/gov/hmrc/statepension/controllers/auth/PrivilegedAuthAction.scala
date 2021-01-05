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

package uk.gov.hmrc.statepension.controllers.auth

import com.google.inject.Inject
import play.api.Logger
import play.api.mvc.Results.{InternalServerError, Unauthorized}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisationException, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class PrivilegedAuthAction @Inject()(
                                      val authConnector: AuthConnector
                                    )(implicit executionContext: ExecutionContext)
  extends AuthAction with AuthorisedFunctions {

  private val logger = Logger(this.getClass)

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSessionAndRequest(request.headers, request = Some(request))

    authorised(AuthProviders(PrivilegedApplication)){
      Future.successful(None)
    } recover {
      case e: AuthorisationException =>
        logger.debug("Debug info - " + e.getMessage, e)
        Some(Unauthorized)
      case e: Throwable =>
        logger.error("Unexpected Error", e)
        Some(InternalServerError)
    }
  }
}
