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

import org.apache.pekko.dispatch.ExecutionContexts.global
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.controllers.HashedNino
import utils.StatePensionBaseSpec

import scala.concurrent.Future

class CopeFailedCacheSpec extends StatePensionBaseSpec with BeforeAndAfter
  with BeforeAndAfterAll with ScalaFutures {

  class FakeCopeFailedCache extends CopeFailedCache(
    mock[MongoComponent],
    mock[TimestampSupport]
  )(global, mock[AppConfig])

  implicit val mockAppConfig: AppConfig = mock[AppConfig]

  "CopeFailedCache" should {
    "insert a HashedNino and return CacheItem" in {
      val cache = new FakeCopeFailedCache
      val mockedNino = mock[HashedNino]
      val expectedCacheItem = CacheItem("testKey", Json.obj("value" -> "testValue"), java.time.Instant.now(), java.time.Instant.now())

      when(mockedNino.generateHash()).thenReturn("testHash")
      when(cache.put("testHash")(DataKey("copeUser"), "testHash")).thenReturn(Future.successful(expectedCacheItem))

      val result = cache.insert(mockedNino)

      whenReady(result) { res =>
        res shouldBe expectedCacheItem
      }
    }

    "get a HashedNino" in {
      val cache = new FakeCopeFailedCache
      val mockedNino = mock[HashedNino]
      val expectedValue = Some("testValue")

      when(mockedNino.generateHash()).thenReturn("testHash")
      when(cache.get[String]("testHash")(DataKey("copeUser"))).thenReturn(Future.successful(expectedValue))

      val result = cache.get(mockedNino)

      whenReady(result) { res =>
        res shouldBe expectedValue
      }
    }
  }
}
