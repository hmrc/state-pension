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
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.statepension.connectors.{IfConnector, ProxyCacheConnector}
import uk.gov.hmrc.statepension.domain.Exclusion
import uk.gov.hmrc.statepension.domain.nps.{Liabilities, NIRecord, ProxyCacheData, Summary}
import uk.gov.hmrc.statepension.fixtures.SummaryFixture
import uk.gov.hmrc.statepension.models.ProxyCacheToggle
import utils.StatePensionBaseSpec

import scala.concurrent.Future

class DashboardServiceSpec extends StatePensionBaseSpec with EitherValues {

  val mockIfConnector: IfConnector = mock[IfConnector]
  val mockProxyCacheConnector: ProxyCacheConnector = mock[ProxyCacheConnector]
  val mockMetrics: ApplicationMetrics = mock[ApplicationMetrics]
  val mockFeatureFlagService: FeatureFlagService = mock[FeatureFlagService]

  val sut: DashboardService = GuiceApplicationBuilder()
    .overrides(
      bind[IfConnector].toInstance(mockIfConnector),
      bind[ProxyCacheConnector].toInstance(mockProxyCacheConnector),
      bind[ApplicationMetrics].toInstance(mockMetrics),
      bind[AuditConnector].toInstance(mock[AuditConnector]),
      bind[FeatureFlagService].toInstance(mockFeatureFlagService),
    )
    .injector()
    .instanceOf[DashboardService]

  val summary: Summary = SummaryFixture.exampleSummary

  "getStatement when Proxy Cache Toggle disabled" should {
    when(mockIfConnector.getLiabilities(any())(any()))
      .thenReturn(Future.successful(List()))
    when(mockIfConnector.getNIRecord(any())(any()))
      .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))

    "return StatePension data" when {
      "Summary data has false for MCI check" in {
        when(mockFeatureFlagService.get(any()))
          .thenReturn(FeatureFlag(ProxyCacheToggle, isEnabled = false, description = None))
        when(mockIfConnector.getSummary(any())(any()))
          .thenReturn(Future.successful(summary.copy(manualCorrespondenceIndicator = Some(false))))

        val result = await(sut.getStatement(generateNino()))
        result shouldBe a[Right[_, _]]
      }
    }

    "return MCI exclusion" when {
      "summary data has true for MCI check" in {
        when(mockFeatureFlagService.get(any()))
          .thenReturn(FeatureFlag(ProxyCacheToggle, isEnabled = false, description = None))
        when(mockIfConnector.getSummary(any())(any()))
          .thenReturn(Future.successful(summary.copy(manualCorrespondenceIndicator = Some(true))))

        val result = await(sut.getStatement(generateNino()))
        result.left.value.exclusionReasons shouldBe List(Exclusion.ManualCorrespondenceIndicator)
      }
    }
  }

  "getStatement when Proxy Cache Toggle enabled" should {
    "return StatePension data" when {
      "Summary data has false for MCI check" in {
        when(mockFeatureFlagService.get(any()))
          .thenReturn(FeatureFlag(ProxyCacheToggle, isEnabled = true, description = None))
        when(mockProxyCacheConnector.getProxyCacheData(any())(any()))
          .thenReturn(Future.successful(ProxyCacheData(
            summary = summary.copy(manualCorrespondenceIndicator = Some(false)),
            nIRecord = NIRecord(qualifyingYears = 36, List()),
            liabilities = Liabilities(List())
          )))

        val result = await(sut.getStatement(generateNino()))
        result shouldBe a[Right[_, _]]
      }
    }

    "return MCI exclusion" when {
      "summary data has true for MCI check" in {
        when(mockFeatureFlagService.get(any()))
          .thenReturn(FeatureFlag(ProxyCacheToggle, isEnabled = true, description = None))
        when(mockProxyCacheConnector.getProxyCacheData(any())(any()))
          .thenReturn(Future.successful(ProxyCacheData(
            summary = summary.copy(manualCorrespondenceIndicator = Some(true)),
            nIRecord = NIRecord(qualifyingYears = 36, List()),
            liabilities = Liabilities(List())
          )))

        val result = await(sut.getStatement(generateNino()))
        result.left.value.exclusionReasons shouldBe List(Exclusion.ManualCorrespondenceIndicator)
      }
    }
  }
}
