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

package uk.gov.hmrc.statepension.connectors

import com.google.inject.Inject
import play.api.Logging
import play.api.libs.json.{JsPath, JsonValidationError, Reads}
import uk.gov.hmrc.http.{HttpException, HttpResponse, UpstreamErrorResponse}

import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}

class ConnectorUtil @Inject()(
  implicit val executionContext: ExecutionContext
) extends Logging {

  def handleConnectorResponse[A](
    futureResponse: Future[Either[UpstreamErrorResponse, HttpResponse]]
  )(
    implicit reads: Reads[A]
  ): Future[Either[Exception, A]] = {
    futureResponse map {
      case Right(response) =>
        response.json.validate[A].fold(
          errs =>
            Left(new JsonValidationException(formatJsonErrors(errs.asInstanceOf[Seq[(JsPath, Seq[JsonValidationError])]]))),
          valid =>
            Right(valid)
        )
      case Left(error) =>
        Left(error)
    } recover {
      case ex: HttpException =>
        Left(ex)
      case ex =>
        Left(new Exception(ex))
    }
  }

  private def formatJsonErrors(errors: Seq[(JsPath, Seq[JsonValidationError])]): String =
    errors
      .map(p => s"${p._1.toString()} - ${p._2.map(_.message).mkString(",")}")
      .mkString(" | ")

  class JsonValidationException(message: String) extends Exception(message)
}
