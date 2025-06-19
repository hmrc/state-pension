/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.mvc.BodyParsers
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.AuthConnector
import com.google.inject.{ImplementedBy, Inject}
import uk.gov.hmrc.statepension.connectors.FandFConnector

import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

class MdtpAuthActionImpl @Inject() (
                      val authConn: AuthConnector,
                      val parse: BodyParsers.Default,
                      val ec: ExecutionContext,
                      val fandFConnector: FandFConnector             
                    )
  extends AuthActionImpl(authConn, parse, fandFConnector)(ec) with MdtpAuthAction {

  override val predicate: Predicate = EmptyPredicate
  override val matchNinoInUriPattern: Regex = "[ni|cope]/mdtp/([^/]+)/?.*".r
}


@ImplementedBy(classOf[MdtpAuthActionImpl])
trait MdtpAuthAction extends AuthAction