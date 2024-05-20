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

import com.mongodb.{DuplicateKeyException, ServerAddress, WriteConcernResult}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.IntegrationPatience
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.controllers.HashedNino
import uk.gov.hmrc.statepension.models.CopeRecord
import utils.StatePensionBaseSpec

import java.time.LocalDate
import scala.concurrent.ExecutionContext.global
import scala.concurrent.ExecutionContextExecutor

class CopeProcessingRepositorySpec
  extends StatePensionBaseSpec
    with PlayMongoRepositorySupport[CopeRecord]
    with IntegrationPatience {

  implicit val ec: ExecutionContextExecutor = global

  def builder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()

  private val nino: Nino =
    Nino("XL166965A")

  private val hashedNino: HashedNino =
    HashedNino(nino)

  private val today: LocalDate =
    LocalDate.now()

  val app: Application =
    builder.build()

  implicit val appConfig: AppConfig =
    app.injector.instanceOf[AppConfig]

  override protected val repository: CopeProcessingRepository =
    new CopeProcessingRepository(mongoComponent)

  private val copeRecord: CopeRecord =
    CopeRecord(hashedNino.generateHash(), today, today)

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.collection
  }

  "CopeProcessingRepository" should {
    "insert, find, update and delete" in {
      running(app) {
        whenReady(for {
          _       <- repository.collection.deleteMany(Filters.empty()).toFuture()
          insert  <- repository.insert(copeRecord)
          find    <- repository.find(hashedNino)
          update  <- repository.update(hashedNino, today.plusDays(1L), today.plusDays(2L))
          updated <- repository.find(hashedNino)
          delete  <- repository.delete(hashedNino)
        } yield {
          (insert, find, update, updated, delete)
        }) {
          res: (Boolean, Option[CopeRecord], Option[CopeRecord], Option[CopeRecord], CopeRecord) =>
            val (insert, find, updateReturnValue, updatedRecord, deleteReturnValue) =
              (res._1, res._2, res._3, res._4, res._5)

            insert shouldBe true

            find.map {
              copeRecord =>
                copeRecord.nino shouldBe hashedNino.generateHash()
                copeRecord.firstLoginDate shouldBe today
                copeRecord.copeAvailableDate shouldBe today
                copeRecord.previousCopeAvailableDate shouldBe None
            }

            updateReturnValue.map {
              copeRecord =>
                copeRecord.nino shouldBe hashedNino.generateHash()
                copeRecord.firstLoginDate shouldBe today
                copeRecord.copeAvailableDate shouldBe today
                copeRecord.previousCopeAvailableDate shouldBe None
            }

            updatedRecord.map {
              copeRecord =>
                copeRecord.nino shouldBe hashedNino.generateHash()
                copeRecord.firstLoginDate shouldBe today
                copeRecord.copeAvailableDate shouldBe today.plusDays(1L)
                copeRecord.previousCopeAvailableDate shouldBe Some(today.plusDays(2L))
            }

            deleteReturnValue.nino shouldBe hashedNino.generateHash()
            deleteReturnValue.firstLoginDate shouldBe today
            deleteReturnValue.copeAvailableDate shouldBe today.plusDays(1L)
            deleteReturnValue.previousCopeAvailableDate shouldBe Some(today.plusDays(2L))

            repository.collection.estimatedDocumentCount().map(_ shouldBe 0L)
        }
      }
    }

    "return false when record already exists" in {
      running(app) {
        whenReady(for {
          _   <- repository.collection.deleteMany(Filters.empty()).toFuture()
          _   <- repository.insert(copeRecord)
          res <- repository.insert(copeRecord)
        } yield res) {
          _ shouldBe false
        }
      }
    }

    "return false for DuplicateKeyException" in {
      val mongo = mock[MongoCollection[CopeRecord]]

      when(mongo.insertOne(any()))
        .thenThrow(new DuplicateKeyException(new BsonDocument(), new ServerAddress(), WriteConcernResult.unacknowledged()))

      val repository = app.injector.instanceOf[CopeProcessingRepository]

      running(app) {
        val ex = await(repository.insert(copeRecord))
        ex shouldBe false
      }
    }
  }
}
