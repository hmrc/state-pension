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
import play.api.test.Helpers.running
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.controllers.HashedNino
import utils.StatePensionBaseSpec

import scala.concurrent.ExecutionContext.global
import scala.concurrent.ExecutionContextExecutor

class CopeFailedCacheSpec
  extends StatePensionBaseSpec
    with MongoSupport
    with GuiceOneAppPerSuite {

  implicit val ec: ExecutionContextExecutor = global

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(bind[MongoComponent].toInstance(mongoComponent))
    .build()

  private val hashedNino: HashedNino =
    HashedNino(generateNino())

  private val cache: CopeFailedCache =
    app.injector.instanceOf[CopeFailedCache]

  implicit val appConfig: AppConfig =
    app.injector.instanceOf[AppConfig]

  "CopeFailedCache" should {
    "insert and get" in {
      running(app) {
        whenReady(
          for {
            _      <- cache.insert(hashedNino)
            result <- cache.get(hashedNino)
          } yield result
        ) {
          _ shouldBe Some(hashedNino.generateHash())
        }
      }
    }
  }
}
