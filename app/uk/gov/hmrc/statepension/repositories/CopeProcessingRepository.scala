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

package uk.gov.hmrc.statepension.repositories

import com.google.inject.Inject
import org.apache.pekko.Done
import org.mongodb.scala.gridfs.SingleObservableFuture
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.controllers.HashedNino
import uk.gov.hmrc.statepension.models.CopeRecord

import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class CopeProcessingRepository @Inject()(mongo: MongoComponent)(implicit ec: ExecutionContext, appConfig: AppConfig)
  extends PlayMongoRepository[CopeRecord](
    collectionName = "cope",
    mongoComponent = mongo,
    domainFormat = CopeRecord.format,
    indexes = Seq(
      IndexModel(
        ascending("nino"),
        IndexOptions()
          .name("indexed_nino")
          .unique(true)
      ),
      IndexModel(
        ascending("firstLoginDate"),
        IndexOptions()
          .name("index_ttl")
          .expireAfter(appConfig.ttlInWeeks * 7L, TimeUnit.DAYS)
      ),
    )
  ) with Logging {

  def insert(copeRecord: CopeRecord): Future[Done] =
    collection
      .insertOne(copeRecord)
      .toFuture()
      .map(_ => Done)

  def find(hashedNino: HashedNino): Future[Option[CopeRecord]] =
    collection
      .find(equal("nino", hashedNino.generateHash()))
      .headOption()

  def update(hashedNino: HashedNino, newCopeAvailableDate: LocalDate, previousCopeAvailableDate: LocalDate): Future[Option[CopeRecord]] =
    collection
      .findOneAndUpdate(
        equal("nino", hashedNino.generateHash()),
        Seq(
          set("copeAvailableDate", Codecs.toBson(newCopeAvailableDate)(using MongoJavatimeFormats.localDateWrites)),
          set("previousCopeAvailableDate", Codecs.toBson(previousCopeAvailableDate)(using MongoJavatimeFormats.localDateWrites))
        )
      )
      .toFutureOption()

  def delete(hashedNino: HashedNino): Future[Done] =
    collection
      .findOneAndDelete(
        equal("nino", hashedNino.generateHash())
      )
      .toFuture()
      .map(_ => Done)

}
