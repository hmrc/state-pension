/*
 * Copyright 2020 HM Revenue & Customs
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

import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.statepension.NinoGenerator
import uk.gov.hmrc.statepension.builders.RateServiceBuilder
import uk.gov.hmrc.statepension.connectors.{DesConnector, StatePensionAuditConnector}
import uk.gov.hmrc.statepension.domain.nps.{AmountA2016, AmountB2016, NIRecord, PensionAmounts, Summary}
import play.api.test.Helpers._
import uk.gov.hmrc.statepension.domain.{Exclusion, StatePensionExclusion}

import scala.concurrent.Future

class CheckPensionServiceSpec extends PlaySpec with MockitoSugar with NinoGenerator with EitherValues {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val mockCitizenDetailsService: CitizenDetailsService = mock[CitizenDetailsService]
  val mockDesConnector: DesConnector = mock[DesConnector]
  val mockMetrics: ApplicationMetrics = mock[ApplicationMetrics]

  val sut: CheckPensionService = GuiceApplicationBuilder()
    .overrides(
      bind[CitizenDetailsService].toInstance(mockCitizenDetailsService),
      bind[DesConnector].toInstance(mockDesConnector),
      bind[ForecastingService].toInstance(new ForecastingService(rateService = RateServiceBuilder.default)),
      bind[RateService].toInstance(RateServiceBuilder.default),
      bind[ApplicationMetrics].toInstance(mockMetrics),
      bind[StatePensionAuditConnector].toInstance(mock[StatePensionAuditConnector])
    )
    .injector()
    .instanceOf[CheckPensionService]

  val summary: Summary = Summary(
    earningsIncludedUpTo = new LocalDate(2016, 4, 5),
    statePensionAgeDate = LocalDate.now().plusYears(1),
    finalRelevantStartYear = 2018,
    pensionSharingOrderSERPS = false,
    dateOfBirth = new LocalDate(1954, 3, 9),
    amounts = PensionAmounts(
      pensionEntitlement = 40.53,
      startingAmount2016 = 40.53,
      protectedPayment2016 = 0,
      AmountA2016(
        basicStatePension = 35.79,
        pre97AP = 0,
        post97AP = 0,
        post02AP = 4.74,
        pre88GMP = 0,
        post88GMP = 0,
        pre88COD = 0,
        post88COD = 0,
        graduatedRetirementBenefit = 0
      ),
      AmountB2016(
        mainComponent = 40.02,
        rebateDerivedAmount = 0
      )
    ),
    manualCorrespondenceIndicator = Some(true)
  )

  "getStatement" must {
    "return StatePension data" when {
      "citizen details returns false for MCI check" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(List()))
        when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))
        when(mockCitizenDetailsService.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(false))
        val result = await(sut.getStatement(generateNino()))
        result mustBe a [Right[_, _]]
      }
    }

    "return MCI exclusion" when {
      "citizen details returns true for MCI check" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(List()))
        when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(NIRecord(qualifyingYears = 36, List())))
        when(mockCitizenDetailsService.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(true))
        val result = await(sut.getStatement(generateNino()))
        result must matchPattern {
          case Left(StatePensionExclusion(
            List(Exclusion.ManualCorrespondenceIndicator),
            _,
            _,
            _)) =>
        }
      }
    }
  }
}
