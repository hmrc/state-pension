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

import com.google.inject.{ImplementedBy, Inject}
import play.api.Logging
import play.api.mvc.*
import play.api.mvc.Results.{BadRequest, InternalServerError, Unauthorized}
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{nino, trustedHelper}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

class MdtpAuthActionImpl @Inject()(
                                    cc: ControllerComponents,
                                    val authConnector: AuthConnector,
                                    val parse: BodyParsers.Default,
                                    val ec: ExecutionContext
                                  )
  extends MdtpAuthAction with AuthorisedFunctions with Logging {

  val predicate: Predicate = EmptyPredicate
  //ask about val below.
  private val matchNinoInUriPattern: Regex = "[ni|cope]/mdtp/([^/]+)/?.*".r

  override def parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    println(s"this is request.uri ${request.uri}")

    val matches = matchNinoInUriPattern.findAllIn(request.uri)

    def check(nino: String): Future[Option[Result]] = {
      val uriNino = matches.group(1)
      println(s"this is URININO $uriNino and this is nino $nino")
      if (uriNino == nino) {
        Future.successful(None)
      } else {
        logger.warn("nino does not match nino in uri")
        Future.successful(Some(Unauthorized))
      }
    }

    if (matches.isEmpty) {
      Future.successful(Some(BadRequest))
    } else {
      authorised(predicate).retrieve(nino and trustedHelper) {
        case Some(nino) ~ _ => check(nino)
        case _ ~ Some(trusted) => check(trusted.principalNino.getOrElse(""))
        case _ => {
          println("mdtpAuthAction line 69")
          Future.successful(Some(Unauthorized))
        }
      } recover {
        case e: AuthorisationException =>
          logger.info("Debug info - " + e.getMessage, e)
          Some(Unauthorized)
        case e: Throwable =>
          logger.error(s"Unexpected Error $e", e)
          Some(InternalServerError)
      }
    }
  }

  override implicit protected def executionContext: ExecutionContext = cc.executionContext
}


@ImplementedBy(classOf[MdtpAuthActionImpl])
trait MdtpAuthAction extends ActionBuilder[Request, AnyContent] with ActionFilter[Request]