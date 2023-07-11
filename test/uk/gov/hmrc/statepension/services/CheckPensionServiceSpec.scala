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

package uk.gov.hmrc.statepension.services

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.statepension.connectors.DesConnector
import uk.gov.hmrc.statepension.domain.Exclusion
import uk.gov.hmrc.statepension.domain.nps.{NIRecord, Summary}
import uk.gov.hmrc.statepension.fixtures.SummaryFixture
import utils.StatePensionBaseSpec

import scala.concurrent.Future

class CheckPensionServiceSpec extends StatePensionBaseSpec with EitherValues {

  val mockCitizenDetailsService: CitizenDetailsService = mock[CitizenDetailsService]
  val mockDesConnector: DesConnector = mock[DesConnector]
  val mockMetrics: ApplicationMetrics = mock[ApplicationMetrics]

  val sut: CheckPensionService = GuiceApplicationBuilder()
    .overrides(
      bind[CitizenDetailsService].toInstance(mockCitizenDetailsService),
      bind[DesConnector].toInstance(mockDesConnector),
      bind[ApplicationMetrics].toInstance(mockMetrics),
      bind[AuditConnector].toInstance(mock[AuditConnector])
    )
    .injector()
    .instanceOf[CheckPensionService]

  val summary: Summary = SummaryFixture.exampleSummary

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(mockDesConnector.getSummary(any())(any()))
      .thenReturn(Future.successful(summary))
    when(mockDesConnector.getLiabilities(any())(any()))
      .thenReturn(Future.successful(List()))
    when(mockDesConnector.getNIRecord(any())(any()))
      .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))
  }

  "getStatement" should {
    "return StatePension data" when {
      "citizen details returns false for MCI check" in {
        when(mockCitizenDetailsService.checkManualCorrespondenceIndicator(any())(any(), any()))
          .thenReturn(Future.successful(false))

        val result = sut.getStatement(generateNino()).futureValue
        result shouldBe a[Right[_, _]]
      }
    }

    "return MCI exclusion" when {
      "citizen details returns true for MCI check" in {
        when(mockCitizenDetailsService.checkManualCorrespondenceIndicator(any())(any(), any()))
          .thenReturn(Future.successful(true))

        val result = sut.getStatement(generateNino()).futureValue
        result.left.value.exclusionReasons shouldBe List(Exclusion.ManualCorrespondenceIndicator)
      }
    }
  }
}
