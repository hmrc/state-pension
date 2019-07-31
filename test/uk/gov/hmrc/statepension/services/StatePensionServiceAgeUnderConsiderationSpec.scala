/*
 * Copyright 2019 HM Revenue & Customs
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
import org.mockito.{Matchers, Mockito}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.builders.RateServiceBuilder
import uk.gov.hmrc.statepension.connectors.{DesConnector, StatePensionAuditConnector}
import uk.gov.hmrc.statepension.domain.MQPScenario.ContinueWorking
import uk.gov.hmrc.statepension.domain.{Scenario, StatePension}
import uk.gov.hmrc.statepension.domain.nps.{DesAmountA2016, DesAmountB2016, DesNIRecord, DesNITaxYear, DesStatePensionAmounts, DesSummary}

import scala.concurrent.Future

class StatePensionServiceAgeUnderConsiderationSpec extends StatePensionUnitSpec
  with OneAppPerSuite
  with ScalaFutures
  with MockitoSugar {

  val mockDesConnector: DesConnector = mock[DesConnector]
  val mockMetrics: ApplicationMetrics = mock[ApplicationMetrics]
  val mockCitizenDetails: CitizenDetailsService = mock[CitizenDetailsService]
  val defaultForecasting = new ForecastingService(rateService = RateServiceBuilder.default)

  lazy val service: StatePensionService = new StatePensionService(mockDesConnector,
    mockCitizenDetails,
    defaultForecasting,
    RateServiceBuilder.default,
    mockMetrics,
    mock[StatePensionAuditConnector]) {
    override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
  }

  when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any()))
    .thenReturn(Future.successful(
      List()
    ))

  when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any()))
    .thenReturn(Future.successful(false))

  def regularStatementWithDateOfBirth(dateOfBirth: LocalDate, statePensionAgeDate: LocalDate): DesSummary = {
    DesSummary(
      earningsIncludedUpTo = new LocalDate(2016, 4, 5),
      sex = "F",
      statePensionAgeDate = statePensionAgeDate,
      finalRelevantStartYear = 2018,
      pensionSharingOrderSERPS = false,
      dateOfBirth = dateOfBirth,
      amounts = DesStatePensionAmounts(
        pensionEntitlement = 161.18,
        startingAmount2016 = 161.18,
        protectedPayment2016 = 5.53,
        DesAmountA2016(
          basicStatePension = 119.3,
          pre97AP = 17.79,
          post97AP = 6.03,
          post02AP = 15.4,
          pre88GMP = 0,
          post88GMP = 0,
          pre88COD = 0,
          post88COD = 0,
          graduatedRetirementBenefit = 2.66
        ),
        DesAmountB2016(
          mainComponent = 155.65,
          rebateDerivedAmount = 0
        )
      )
    )
  }

  "StatePensionService with a HOD Connection" when {

    "the customer has state pension age under consideration flag set to false as the date of birth is before the required range " should {

      val statePensionAgeDate = new LocalDate(2034, 4, 5)
      val dateOfBirth = new LocalDate(1970, 4, 5)
      val regularStatement = regularStatementWithDateOfBirth(dateOfBirth, statePensionAgeDate)

      when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        List()
      ))

      when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        DesNIRecord(qualifyingYears = 36, List())
      ))

      lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get

      "statePension have statePensionAgeUnderConsideration flag as false" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          regularStatement
        ))

        whenReady(statePensionF) { statePension =>
          statePension.statePensionAgeUnderConsideration shouldBe false
        }
      }

      "log a summary metric" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          regularStatement
        ))

        verify(mockMetrics, Mockito.atLeastOnce()).summary(
          Matchers.eq[BigDecimal](161.18),
          Matchers.eq[BigDecimal](161.18),
          Matchers.eq(false),
          Matchers.eq(Scenario.Reached),
          Matchers.eq[BigDecimal](161.18),
          Matchers.eq(0),
          Matchers.eq(None),
          Matchers.eq[BigDecimal](161.18),
          Matchers.eq[BigDecimal](119.3),
          Matchers.eq[BigDecimal](39.22),
          Matchers.eq[BigDecimal](2.66),
          Matchers.eq[BigDecimal](155.65),
          Matchers.eq[BigDecimal](0),
          Matchers.eq(false),
          Matchers.eq(None),
          Matchers.eq(false),
          Matchers.eq(false)
        )
      }
    }

    "the customer has state pension age under consideration flag set to true as the date of birth is at the minimum of the required range " should {

      val statePensionAgeDate = new LocalDate(2034, 4, 6)
      val dateOfBirth = new LocalDate(1970, 4, 6)
      val regularStatement = regularStatementWithDateOfBirth(dateOfBirth, statePensionAgeDate)

      when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        List()
      ))

      when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        DesNIRecord(qualifyingYears = 36, List())
      ))

      lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get

      "statePension have statePensionAgeUnderConsideration flag as true" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          regularStatement
        ))

        whenReady(statePensionF) { statePension =>
          statePension.statePensionAgeUnderConsideration shouldBe true
        }
      }

      "log a summary metric" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          regularStatement
        ))

        verify(mockMetrics, Mockito.atLeastOnce()).summary(
          Matchers.eq[BigDecimal](161.18),
          Matchers.eq[BigDecimal](161.18),
          Matchers.eq(false),
          Matchers.eq(Scenario.Reached),
          Matchers.eq[BigDecimal](161.18),
          Matchers.eq(0),
          Matchers.eq(None),
          Matchers.eq[BigDecimal](161.18),
          Matchers.eq[BigDecimal](119.3),
          Matchers.eq[BigDecimal](39.22),
          Matchers.eq[BigDecimal](2.66),
          Matchers.eq[BigDecimal](155.65),
          Matchers.eq[BigDecimal](0),
          Matchers.eq(false),
          Matchers.eq(None),
          Matchers.eq(false),
          Matchers.eq(true)
        )
      }
    }

    "the customer has state pension age under consideration flag set to true as the date of birth is in the middle of the required range " should {

      val summary = DesSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "F",
        statePensionAgeDate = new LocalDate(2038, 1, 1),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1976, 7, 7),
        dateOfDeath = None,
        reducedRateElection = true,
        countryCode = 1,
        amounts = DesStatePensionAmounts(
          pensionEntitlement = 32.61,
          startingAmount2016 = 35.58,
          protectedPayment2016 = 0,
          DesAmountA2016(
            basicStatePension = 31.81,
            pre97AP = 0,
            post97AP = 0,
            post02AP = 0,
            pre88GMP = 0,
            post88GMP = 0,
            pre88COD = 0,
            post88COD = 0,
            graduatedRetirementBenefit = 0
          ),
          DesAmountB2016(
            mainComponent = 35.58,
            rebateDerivedAmount = 0
          )
        )
      )

      lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get

      lazy val summaryF: Future[DesSummary] = mockDesConnector.getSummary(Matchers.any())(Matchers.any())

      "statePension have statePensionAgeUnderConsideration flag as true" in {

        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))

        when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          DesNIRecord(qualifyingYears = 9, List(DesNITaxYear(Some(2000), Some(false), Some(false), Some(true)), DesNITaxYear(Some(2001), Some(false), Some(false), Some(true))))
        ))

        whenReady(statePensionF) { statePension =>
          statePension.statePensionAgeUnderConsideration shouldBe true
        }
      }

      "log a summary metric" in {

        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))

        when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          DesNIRecord(qualifyingYears = 9, List(DesNITaxYear(Some(2000), Some(false), Some(false), Some(true)), DesNITaxYear(Some(2001), Some(false), Some(false), Some(true))))
        ))

        verify(mockMetrics, times(1)).summary(
          Matchers.eq[BigDecimal](155.65),
          Matchers.eq[BigDecimal](0),
          Matchers.eq(false),
          Matchers.eq(Scenario.ContinueWorkingMax),
          Matchers.eq[BigDecimal](155.65),
          Matchers.eq(28),
          Matchers.eq(Some(ContinueWorking)),
          Matchers.eq[BigDecimal](35.58),
          Matchers.eq[BigDecimal](31.81),
          Matchers.eq[BigDecimal](0),
          Matchers.eq[BigDecimal](0),
          Matchers.eq[BigDecimal](35.58),
          Matchers.eq[BigDecimal](0),
          Matchers.eq(true),
          Matchers.eq(Some(32.61)),
          Matchers.eq(false),
          Matchers.eq(true)
        )
      }
    }

    "the customer has state pension age under consideration flag set to true as the date of birth is at the maximum of the required range " should {

      val dateOfBirth = new LocalDate(1978, 4, 5)

      val statePensionAgeDate = new LocalDate(2042, 4, 5)

      val regularStatement = regularStatementWithDateOfBirth(dateOfBirth, statePensionAgeDate)

      lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get

      "statePension have statePensionAgeUnderConsideration flag as true" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(
            DesNIRecord(qualifyingYears = 36, List())
          ))

        whenReady(statePensionF) { statePension =>
          statePension.statePensionAgeUnderConsideration shouldBe true
        }
      }

      "log a summary metric" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(
            DesNIRecord(qualifyingYears = 36, List())
          ))

        verify(mockMetrics, Mockito.atLeastOnce()).summary(
          Matchers.eq[BigDecimal](161.18),
          Matchers.eq[BigDecimal](161.18),
          Matchers.eq(false),
          Matchers.eq(Scenario.Reached),
          Matchers.eq[BigDecimal](161.18),
          Matchers.eq(0),
          Matchers.eq(None),
          Matchers.eq[BigDecimal](161.18),
          Matchers.eq[BigDecimal](119.3),
          Matchers.eq[BigDecimal](39.22),
          Matchers.eq[BigDecimal](2.66),
          Matchers.eq[BigDecimal](155.65),
          Matchers.eq[BigDecimal](0),
          Matchers.eq(false),
          Matchers.eq(None),
          Matchers.eq(false),
          Matchers.eq(true)
        )
      }
    }

    "the customer has state pension age under consideration flag set to false as the date of birth is after the required range " should {

      val dateOfBirth = new LocalDate(1978, 4, 6)

      val statePensionAgeDate = new LocalDate(2042, 4, 6)

      val regularStatement = regularStatementWithDateOfBirth(dateOfBirth, statePensionAgeDate)

      lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get

      "statePension have statePensionAgeUnderConsideration flag as false" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(
            DesNIRecord(qualifyingYears = 36, List())
          ))

        whenReady(statePensionF) { statePension =>
          statePension.statePensionAgeUnderConsideration shouldBe false
        }
      }

      "log a summary metric" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(
            DesNIRecord(qualifyingYears = 36, List())
          ))

        verify(mockMetrics, Mockito.atLeastOnce()).summary(
          Matchers.eq[BigDecimal](161.18),
          Matchers.eq[BigDecimal](161.18),
          Matchers.eq(false),
          Matchers.eq(Scenario.Reached),
          Matchers.eq[BigDecimal](161.18),
          Matchers.eq(0),
          Matchers.eq(None),
          Matchers.eq[BigDecimal](161.18),
          Matchers.eq[BigDecimal](119.3),
          Matchers.eq[BigDecimal](39.22),
          Matchers.eq[BigDecimal](2.66),
          Matchers.eq[BigDecimal](155.65),
          Matchers.eq[BigDecimal](0),
          Matchers.eq(false),
          Matchers.eq(None),
          Matchers.eq(false),
          Matchers.eq(false)
        )
      }
    }
  }
}
