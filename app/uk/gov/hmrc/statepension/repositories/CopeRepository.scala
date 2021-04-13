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

package uk.gov.hmrc.statepension.repositories

import com.google.inject.Inject
import com.mongodb.{DuplicateKeyException, MongoException}
import org.joda.time.LocalDate
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Updates}
import play.api.Logging
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.statepension.domain.CopeMongo

import scala.concurrent.{ExecutionContext, Future}

class CopeRepository @Inject()(mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[CopeMongo](
    collectionName = "cope",
    mongoComponent = mongoComponent,
    domainFormat = CopeMongo.format,
    indexes = Seq(
      IndexModel(ascending("nino"),
      IndexOptions().name("indexed_nino").unique(true))
    )
  ) with Logging {

  def put(copeMongo: CopeMongo): Future[Boolean] = {
    (for {
      insertOneResult <- collection.insertOne(copeMongo).toFuture
    } yield {
      insertOneResult.wasAcknowledged
    }) recover {
      case de: DuplicateKeyException =>
        logger.error("Duplicate Key Exception: attempted to save document with non unique NINO", de)
        false
      case de: MongoException =>
        logger.error("Mongo exception", de)
        false
      case e: Exception => logger.error(s"Exception when saving ErrorResponseCopeProcessing: $e", e)
        false
    }
  }

  def find(nino: Nino): Future[Option[CopeMongo]] =
    collection.find(Filters.equal("nino", nino.nino)).headOption()

  def update(nino: Nino, newCopeDate: LocalDate): Future[CopeMongo] =
    collection.findOneAndUpdate(
      filter = Filters.equal("nino", nino.nino),
      update = Updates.set("copeDataAvailableDate", newCopeDate)
    ).toFuture

  def delete(nino: Nino): Future[CopeMongo] =
    collection.findOneAndDelete(Filters.equal("nino", nino.nino)).toFuture

}
