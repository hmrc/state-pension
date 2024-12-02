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

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.controllers.HashedNino
import uk.gov.hmrc.statepension.models.CopeRecord
import utils.StatePensionBaseSpec

import java.time.LocalDate
import scala.concurrent.ExecutionContext.global
import scala.concurrent.ExecutionContextExecutor

class CopeProcessingRepositorySpec
  extends StatePensionBaseSpec
    with DefaultPlayMongoRepositorySupport[CopeRecord]
    with GuiceOneAppPerSuite
    with WireMockSupport {

  implicit val ec: ExecutionContextExecutor = global

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "internal-auth.isTestOnlyEndpoint"         -> false,
      "microservice.services.internal-auth.port" -> wireMockPort,
      "microservice.services.internal-auth.host" -> wireMockHost
    )
    .overrides(bind[MongoComponent].toInstance(mongoComponent))
    .build()

  private val nino: Nino =
    Nino("XL166965A")

  private val hashedNino: HashedNino =
    HashedNino(nino)

  private val today: LocalDate =
    LocalDate.now()

  implicit val appConfig: AppConfig =
    app.injector.instanceOf[AppConfig]

  override protected val repository: CopeProcessingRepository =
    app.injector.instanceOf[CopeProcessingRepository]

  private val copeRecord: CopeRecord =
    CopeRecord(hashedNino.generateHash(), today, today)

  "CopeProcessingRepository" should {
    "insert, find, update and delete" in {
      whenReady(for {
        _       <- repository.insert(copeRecord)
        find    <- repository.find(hashedNino)
        update  <- repository.update(hashedNino, today.plusDays(1L), today.plusDays(2L))
        updated <- repository.find(hashedNino)
        _       <- repository.delete(hashedNino)
      } yield {
        (find, update, updated)
      }) {
        res: (Option[CopeRecord], Option[CopeRecord], Option[CopeRecord]) =>
          val (find, updateReturnValue, updatedRecord) = (res._1, res._2, res._3)

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

          repository.collection.estimatedDocumentCount().map(_ shouldBe 0L)
      }
    }
  }
}
