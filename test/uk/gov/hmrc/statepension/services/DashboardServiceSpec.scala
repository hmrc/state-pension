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

package uk.gov.hmrc.statepension.services

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.statepension.builders.RateServiceBuilder
import uk.gov.hmrc.statepension.connectors.IfConnector
import uk.gov.hmrc.statepension.domain.Exclusion
import uk.gov.hmrc.statepension.domain.nps.{NIRecord, Summary}
import uk.gov.hmrc.statepension.fixtures.SummaryFixture
import uk.gov.hmrc.statepension.{NinoGenerator, UnitSpec}

import scala.concurrent.Future

class DashboardServiceSpec extends UnitSpec with NinoGenerator with EitherValues with BeforeAndAfterEach {
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val mockIfConnector: IfConnector = mock[IfConnector]
  val mockMetrics: ApplicationMetrics = mock[ApplicationMetrics]

  val sut: DashboardService = GuiceApplicationBuilder()
    .overrides(
      bind[IfConnector].toInstance(mockIfConnector),
      bind[ForecastingService].toInstance(new ForecastingService(rateService = RateServiceBuilder.default)),
      bind[RateService].toInstance(RateServiceBuilder.default),
      bind[ApplicationMetrics].toInstance(mockMetrics),
      bind[AuditConnector].toInstance(mock[AuditConnector])
    )
    .injector()
    .instanceOf[DashboardService]

  val summary: Summary = SummaryFixture.exampleSummary

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(mockIfConnector.getLiabilities(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(List()))
    when(mockIfConnector.getNIRecord(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))
  }

  "getStatement" should {
    "return StatePension data" when {
      "Summary data has false for MCI check" in {
        when(mockIfConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(summary.copy(manualCorrespondenceIndicator = Some(false))))

        val result = sut.getStatement(generateNino()).futureValue
        result shouldBe a[Right[_, _]]
      }
    }

    "return MCI exclusion" when {
      "summary data has true for MCI check" in {
        when(mockIfConnector.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(summary.copy(manualCorrespondenceIndicator = Some(true))))

        val result = sut.getStatement(generateNino()).futureValue
        result.left.value.exclusionReasons mustBe List(Exclusion.ManualCorrespondenceIndicator)
      }
    }
  }
}
