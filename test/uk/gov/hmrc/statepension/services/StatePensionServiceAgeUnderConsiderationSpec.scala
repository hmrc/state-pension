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

import org.joda.time.LocalDate
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{Matchers, Mockito}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.statepension.StatePensionBaseSpec
import uk.gov.hmrc.statepension.builders.RateServiceBuilder
import uk.gov.hmrc.statepension.connectors.NpsConnector
import uk.gov.hmrc.statepension.domain.MQPScenario.ContinueWorking
import uk.gov.hmrc.statepension.domain.nps._
import uk.gov.hmrc.statepension.domain.{Scenario, StatePension}

import scala.concurrent.Future

class StatePensionServiceAgeUnderConsiderationSpec extends StatePensionBaseSpec
  with OneAppPerSuite
  with ScalaFutures
  with MockitoSugar {

  val mockNpsConnector: NpsConnector = mock[NpsConnector]
  val mockMetrics: ApplicationMetrics = mock[ApplicationMetrics]
  val defaultForecasting = new ForecastingService(rateService = RateServiceBuilder.default)

  lazy val service: StatePensionService = new StatePensionService {
    override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
    override val nps: NpsConnector = mockNpsConnector
    override val forecastingService: ForecastingService = defaultForecasting
    override val rateService: RateService = RateServiceBuilder.default
    override val metrics: ApplicationMetrics = mockMetrics
    override val customAuditConnector: AuditConnector = mock[AuditConnector]
    override def getMCI(summary: Summary, nino: Nino)(implicit hc: HeaderCarrier): Future[Boolean] =
      Future.successful(false)
  }

  when(mockNpsConnector.getLiabilities(Matchers.any())(Matchers.any()))
    .thenReturn(Future.successful(
      List()
    ))

  def regularStatementWithDateOfBirth(dateOfBirth: LocalDate, statePensionAgeDate: LocalDate): Summary = {
    Summary(
      earningsIncludedUpTo = new LocalDate(2016, 4, 5),
      statePensionAgeDate = statePensionAgeDate,
      finalRelevantStartYear = 2018,
      pensionSharingOrderSERPS = false,
      dateOfBirth = dateOfBirth,
      amounts = PensionAmounts(
        pensionEntitlement = 161.18,
        startingAmount2016 = 161.18,
        protectedPayment2016 = 5.53,
        AmountA2016(
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
        AmountB2016(
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

      when(mockNpsConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        List()
      ))

      when(mockNpsConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        NIRecord(qualifyingYears = 36, List())
      ))

      lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get

      "statePension have statePensionAgeUnderConsideration flag as false" in {
        when(mockNpsConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          regularStatement
        ))

        whenReady(statePensionF) { statePension =>
          statePension.statePensionAgeUnderConsideration shouldBe false
        }
      }

      "log a summary metric" in {
        when(mockNpsConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
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
          Matchers.eq(false)
        )
      }
    }

    "the customer has state pension age under consideration flag set to true as the date of birth is at the minimum of the required range " should {

      val statePensionAgeDate = new LocalDate(2034, 4, 6)
      val dateOfBirth = new LocalDate(1970, 4, 6)
      val regularStatement = regularStatementWithDateOfBirth(dateOfBirth, statePensionAgeDate)

      when(mockNpsConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        List()
      ))

      when(mockNpsConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        NIRecord(qualifyingYears = 36, List())
      ))

      lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get

      "statePension have statePensionAgeUnderConsideration flag as true" in {
        when(mockNpsConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          regularStatement
        ))

        whenReady(statePensionF) { statePension =>
          statePension.statePensionAgeUnderConsideration shouldBe true
        }
      }

      "log a summary metric" in {
        when(mockNpsConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
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
          Matchers.eq(true)
        )
      }
    }

    "the customer has state pension age under consideration flag set to true as the date of birth is in the middle of the required range " should {

      val summary = Summary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        statePensionAgeDate = new LocalDate(2038, 1, 1),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1976, 7, 7),
        dateOfDeath = None,
        reducedRateElection = true,
        countryCode = 1,
        amounts = PensionAmounts(
          pensionEntitlement = 32.61,
          startingAmount2016 = 35.58,
          protectedPayment2016 = 0,
          AmountA2016(
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
          AmountB2016(
            mainComponent = 35.58,
            rebateDerivedAmount = 0
          )
        ),
        None
      )

      lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get

      "statePension have statePensionAgeUnderConsideration flag as true" in {

        when(mockNpsConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))

        when(mockNpsConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          NIRecord(qualifyingYears = 9, List(NITaxYear(Some(2000), Some(false), Some(false), Some(true)), NITaxYear(Some(2001), Some(false), Some(false), Some(true))))
        ))

        whenReady(statePensionF) { statePension =>
          statePension.statePensionAgeUnderConsideration shouldBe true
        }
      }

      "log a summary metric" in {

        when(mockNpsConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))

        when(mockNpsConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          NIRecord(qualifyingYears = 9, List(NITaxYear(Some(2000), Some(false), Some(false), Some(true)), NITaxYear(Some(2001), Some(false), Some(false), Some(true))))
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
        when(mockNpsConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        when(mockNpsConnector.getNIRecord(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(
            NIRecord(qualifyingYears = 36, List())
          ))

        whenReady(statePensionF) { statePension =>
          statePension.statePensionAgeUnderConsideration shouldBe true
        }
      }

      "log a summary metric" in {
        when(mockNpsConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        when(mockNpsConnector.getNIRecord(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(
            NIRecord(qualifyingYears = 36, List())
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
        when(mockNpsConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        when(mockNpsConnector.getNIRecord(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(
            NIRecord(qualifyingYears = 36, List())
          ))

        whenReady(statePensionF) { statePension =>
          statePension.statePensionAgeUnderConsideration shouldBe false
        }
      }

      "log a summary metric" in {
        when(mockNpsConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(regularStatement))

        when(mockNpsConnector.getNIRecord(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(
            NIRecord(qualifyingYears = 36, List())
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
          Matchers.eq(false)
        )
      }
    }
  }
}
