/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.statepension.services

import com.codahale.metrics.{Counter, MetricRegistry, Timer}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.statepension.domain.nps.APIType

class ApplicationMetricsSpec extends AnyWordSpec with Matchers with MockitoSugar {

  "ApplicationMetrics" should {
    "start the correct timer for a given APIType" in {
      val mockMetrics = mock[MetricRegistry]
      val mockTimer = mock[Timer]
      val mockContext = mock[Timer.Context]

      when(mockMetrics.timer(ArgumentMatchers.any[String])).thenReturn(mockTimer)
      when(mockTimer.time()).thenReturn(mockContext)

      val metrics = new ApplicationMetrics(mockMetrics)

      val timerContext = metrics.startTimer(APIType.Summary)

      verify(mockMetrics).timer("summary-response-timer")
      verify(mockTimer).time()
      timerContext shouldBe mockContext
    }

    "increment the correct failed counter for a given APIType" in {
      val mockMetrics = mock[MetricRegistry]
      val mockCounter = mock[Counter]

      when(mockMetrics.counter(ArgumentMatchers.any[String])).thenReturn(mockCounter)

      val metrics = new ApplicationMetrics(mockMetrics)

      metrics.incrementFailedCounter(APIType.Summary)

      verify(mockMetrics).counter("summary-failed-counter")
      verify(mockCounter).inc()
    }
  }
}

