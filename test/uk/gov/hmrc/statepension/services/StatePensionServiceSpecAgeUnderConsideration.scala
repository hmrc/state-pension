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
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.builders.RateServiceBuilder
import uk.gov.hmrc.statepension.connectors.{DesConnector, StatePensionAuditConnector}
import uk.gov.hmrc.statepension.domain.MQPScenario.ContinueWorking
import uk.gov.hmrc.statepension.domain.nps._
import uk.gov.hmrc.statepension.domain.{Exclusion, StatePension, _}

import scala.concurrent.Future

class StatePensionServiceSpecAgeUnderConsideration extends StatePensionUnitSpec
  with OneAppPerSuite
  with ScalaFutures
  with MockitoSugar
  with BeforeAndAfterEach{

  private val dummyStatement: StatePension = StatePension(
    // scalastyle:off magic.number
    earningsIncludedUpTo = new LocalDate(2015, 4, 5),
    amounts = StatePensionAmounts(
      protectedPayment = false,
      current = StatePensionAmount(
        None,
        None,
        133.41
      ),
      forecast = StatePensionAmount(
        yearsToWork = Some(3),
        None,
        146.76
      ),
      maximum = StatePensionAmount(
        yearsToWork = Some(3),
        gapsToFill = Some(2),
        weeklyAmount = 155.65
      ),
      cope = StatePensionAmount(
        None,
        None,
        0.00
      ),
      starting = StatePensionAmount(
        None,
        None,
        160.18
      ),
      OldRules(basicStatePension = 119.30,
        additionalStatePension = 38.90,
        graduatedRetirementBenefit = 10.00
      ),
      NewRules(grossStatePension = 155.65,
        rebateDerivedAmount = 0.00)
    ),
    pensionAge = 64,
    pensionDate = new LocalDate(2018, 7, 6),
    finalRelevantYear = "2017-18",
    numberOfQualifyingYears = 30,
    pensionSharingOrder = false,
    currentFullWeeklyPensionAmount = 155.65,
    reducedRateElection = false,
    reducedRateElectionCurrentWeeklyAmount = None,
    abroadAutoCredit = false,
    statePensionAgeUnderConsideration = false
  )

//  override def beforeEach: Unit = {
//    Mockito.reset(mockMetrics, mockDesConnector, mockCitizenDetails, mockDefaultForecasting)
//  }

  "StatePensionService with a HOD Connection" when {
    "the customer has state pension age under consideration flag set to false as the date of birth is before the required range " should {

      val mockDesConnector: DesConnector = mock[DesConnector]
      val mockMetrics: ApplicationMetrics = mock[ApplicationMetrics]
      val mockCitizenDetails: CitizenDetailsService = mock[CitizenDetailsService]
      val mockDefaultForecasting = mock[ForecastingService]

      val service: StatePensionService = new StatePensionService(mockDesConnector,
        mockCitizenDetails,
        mockDefaultForecasting,
        RateServiceBuilder.default,
        mockMetrics,
        mock[StatePensionAuditConnector]) {
        override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
      }

      when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any())).thenReturn(Future.successful(false))

      val regularStatement = DesSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "F",
        statePensionAgeDate = new LocalDate(2034, 4, 5),
        finalRelevantStartYear = 2018,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1970, 4, 5),
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

      when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        regularStatement
      ))

      when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        List()
      ))

      when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        DesNIRecord(qualifyingYears = 36, List())
      ))

      //TODO - Go through getStatement to find where dropping out and compare to master.
      //AmountDissonance error being thrown

      print("%%%%%%%%%%%%%%%%%%%%%%%% -- " + service.getStatement(generateNino()).left)

      lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get

      "statePension have statePensionAgeUnderConsideration flag as false" in {
        whenReady(statePensionF) { statePension =>
          statePension.statePensionAgeUnderConsideration shouldBe false
        }
      }

      "log a summary metric" in {
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

//    "the customer has state pension age under consideration flag set to true as the date of birth is at the minimum of the required range " should {
//
//
//      val mockDesConnector: DesConnector = mock[DesConnector]
//  val mockMetrics: Metrics = mock[Metrics]
//  val mockCitizenDetails: CitizenDetailsService = mock[CitizenDetailsService]
//  val mockDefaultForecasting = mock[ForecastingService]
//
//  val service: StatePensionService = new StatePensionService(mockDesConnector,
//    mockCitizenDetails,
//    mockDefaultForecasting,
//    RateServiceBuilder.default,
//    mockMetrics,
//    mock[StatePensionAuditConnector]) {
//    override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
//  }
//
//    when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any())).thenReturn(Future.successful(false))
//
//      val regularStatement = DesSummary(
//        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
//        sex = "F",
//        statePensionAgeDate = new LocalDate(2034, 4, 6),
//        finalRelevantStartYear = 2018,
//        pensionSharingOrderSERPS = false,
//        dateOfBirth = new LocalDate(1970, 4, 6),
//        amounts = DesStatePensionAmounts(
//          pensionEntitlement = 161.18,
//          startingAmount2016 = 161.18,
//          protectedPayment2016 = 5.53,
//          DesAmountA2016(
//            basicStatePension = 119.3,
//            pre97AP = 17.79,
//            post97AP = 6.03,
//            post02AP = 15.4,
//            pre88GMP = 0,
//            post88GMP = 0,
//            pre88COD = 0,
//            post88COD = 0,
//            graduatedRetirementBenefit = 2.66
//          ),
//          DesAmountB2016(
//            mainComponent = 155.65,
//            rebateDerivedAmount = 0
//          )
//        )
//      )
//
//      when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
//        regularStatement
//      ))
//
//      when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
//        List()
//      ))
//
//      when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
//        DesNIRecord(qualifyingYears = 36, List())
//      ))
//
//      lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get
//
//      "statePension have statePensionAgeUnderConsideration flag as true" in {
//        whenReady(statePensionF) { statePension =>
//          statePension.statePensionAgeUnderConsideration shouldBe true
//        }
//      }
//
//      "log a summary metric" in {
//        verify(mockMetrics, Mockito.atLeastOnce()).summary(
//          Matchers.eq[BigDecimal](161.18),
//          Matchers.eq[BigDecimal](161.18),
//          Matchers.eq(false),
//          Matchers.eq(Scenario.Reached),
//          Matchers.eq[BigDecimal](161.18),
//          Matchers.eq(0),
//          Matchers.eq(None),
//          Matchers.eq[BigDecimal](161.18),
//          Matchers.eq[BigDecimal](119.3),
//          Matchers.eq[BigDecimal](39.22),
//          Matchers.eq[BigDecimal](2.66),
//          Matchers.eq[BigDecimal](155.65),
//          Matchers.eq[BigDecimal](0),
//          Matchers.eq(false),
//          Matchers.eq(None),
//          Matchers.eq(false),
//          Matchers.eq(true)
//        )
//      }
//    }
//
//    "the customer has state pension age under consideration flag set to true as the date of birth is in the middle of the required range " should {
//
//      val mockDesConnector: DesConnector = mock[DesConnector]
//  val mockMetrics: Metrics = mock[Metrics]
//  val mockCitizenDetails: CitizenDetailsService = mock[CitizenDetailsService]
//  val mockDefaultForecasting = mock[ForecastingService]
//
//  val service: StatePensionService = new StatePensionService(mockDesConnector,
//    mockCitizenDetails,
//    mockDefaultForecasting,
//    RateServiceBuilder.default,
//    mockMetrics,
//    mock[StatePensionAuditConnector]) {
//    override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
//  }
//
//    when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any())).thenReturn(Future.successful(false))
//
//
//      val summary = DesSummary(
//        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
//        sex = "F",
//        statePensionAgeDate = new LocalDate(2038, 1, 1),
//        finalRelevantStartYear = 2049,
//        pensionSharingOrderSERPS = false,
//        dateOfBirth = new LocalDate(1976, 7, 7),
//        dateOfDeath = None,
//        reducedRateElection = true,
//        countryCode = 1,
//        amounts = DesStatePensionAmounts(
//          pensionEntitlement = 32.61,
//          startingAmount2016 = 35.58,
//          protectedPayment2016 = 0,
//          DesAmountA2016(
//            basicStatePension = 31.81,
//            pre97AP = 0,
//            post97AP = 0,
//            post02AP = 0,
//            pre88GMP = 0,
//            post88GMP = 0,
//            pre88COD = 0,
//            post88COD = 0,
//            graduatedRetirementBenefit = 0
//          ),
//          DesAmountB2016(
//            mainComponent = 35.58,
//            rebateDerivedAmount = 0
//          )
//        )
//      )
//
//      when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
//        summary
//      ))
//
//      when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
//        List()
//      ))
//
//      when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
//        DesNIRecord(qualifyingYears = 9, List(DesNITaxYear(Some(2000), Some(false), Some(false), Some(true)), DesNITaxYear(Some(2001), Some(false), Some(false), Some(true))))
//      ))
//
//      lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get
//
//      lazy val summaryF: Future[DesSummary] = mockDesConnector.getSummary(Matchers.any())(Matchers.any())
//
//      "statePension have statePensionAgeUnderConsideration flag as true" in {
//        whenReady(statePensionF) { statePension =>
//          statePension.statePensionAgeUnderConsideration shouldBe true
//        }
//      }
//
//      "log a summary metric" in {
//        verify(mockMetrics, times(1)).summary(
//          Matchers.eq[BigDecimal](155.65),
//          Matchers.eq[BigDecimal](0),
//          Matchers.eq(false),
//          Matchers.eq(Scenario.ContinueWorkingMax),
//          Matchers.eq[BigDecimal](155.65),
//          Matchers.eq(28),
//          Matchers.eq(Some(ContinueWorking)),
//          Matchers.eq[BigDecimal](35.58),
//          Matchers.eq[BigDecimal](31.81),
//          Matchers.eq[BigDecimal](0),
//          Matchers.eq[BigDecimal](0),
//          Matchers.eq[BigDecimal](35.58),
//          Matchers.eq[BigDecimal](0),
//          Matchers.eq(true),
//          Matchers.eq(Some(32.61)),
//          Matchers.eq(false),
//          Matchers.eq(true)
//        )
//      }
//    }
//
//    "the customer has state pension age under consideration flag set to true as the date of birth is at the maximum of the required range " should {
//
//
//      val mockDesConnector: DesConnector = mock[DesConnector]
//  val mockMetrics: Metrics = mock[Metrics]
//  val mockCitizenDetails: CitizenDetailsService = mock[CitizenDetailsService]
//  val mockDefaultForecasting = mock[ForecastingService]
//
//  val service: StatePensionService = new StatePensionService(mockDesConnector,
//    mockCitizenDetails,
//    mockDefaultForecasting,
//    RateServiceBuilder.default,
//    mockMetrics,
//    mock[StatePensionAuditConnector]) {
//    override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
//  }
//
//    when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any())).thenReturn(Future.successful(false))
//
//      val regularStatement = DesSummary(
//        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
//        sex = "F",
//        statePensionAgeDate = new LocalDate(2042, 4, 5),
//        finalRelevantStartYear = 2018,
//        pensionSharingOrderSERPS = false,
//        dateOfBirth = new LocalDate(1978, 4, 5),
//        amounts = DesStatePensionAmounts(
//          pensionEntitlement = 161.18,
//          startingAmount2016 = 161.18,
//          protectedPayment2016 = 5.53,
//          DesAmountA2016(
//            basicStatePension = 119.3,
//            pre97AP = 17.79,
//            post97AP = 6.03,
//            post02AP = 15.4,
//            pre88GMP = 0,
//            post88GMP = 0,
//            pre88COD = 0,
//            post88COD = 0,
//            graduatedRetirementBenefit = 2.66
//          ),
//          DesAmountB2016(
//            mainComponent = 155.65,
//            rebateDerivedAmount = 0
//          )
//        )
//      )
//
//      when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
//        regularStatement
//      ))
//
//      when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
//        List()
//      ))
//
//      when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
//        DesNIRecord(qualifyingYears = 36, List())
//      ))
//
//      lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get
//
//      "statePension have statePensionAgeUnderConsideration flag as true" in {
//        whenReady(statePensionF) { statePension =>
//          statePension.statePensionAgeUnderConsideration shouldBe true
//        }
//      }
//
//      "log a summary metric" in {
//        verify(mockMetrics, Mockito.atLeastOnce()).summary(
//          Matchers.eq[BigDecimal](161.18),
//          Matchers.eq[BigDecimal](161.18),
//          Matchers.eq(false),
//          Matchers.eq(Scenario.Reached),
//          Matchers.eq[BigDecimal](161.18),
//          Matchers.eq(0),
//          Matchers.eq(None),
//          Matchers.eq[BigDecimal](161.18),
//          Matchers.eq[BigDecimal](119.3),
//          Matchers.eq[BigDecimal](39.22),
//          Matchers.eq[BigDecimal](2.66),
//          Matchers.eq[BigDecimal](155.65),
//          Matchers.eq[BigDecimal](0),
//          Matchers.eq(false),
//          Matchers.eq(None),
//          Matchers.eq(false),
//          Matchers.eq(true)
//        )
//      }
//    }
//
//    "the customer has state pension age under consideration flag set to false as the date of birth is after the required range " should {
//
//        val NEWmockDesConnector: DesConnector = mock[DesConnector]
//  val NEWmockMetrics: Metrics = mock[Metrics]
//  val NEWmockCitizenDetails: CitizenDetailsService = mock[CitizenDetailsService]
//  val NEWdefaultForecasting = new ForecastingService(rateService = RateServiceBuilder.default)
//
//  val NEWService: StatePensionService = new StatePensionService(NEWmockDesConnector,
//    NEWmockCitizenDetails,
//    NEWdefaultForecasting,
//    RateServiceBuilder.default,
//    NEWmockMetrics,
//    mock[StatePensionAuditConnector]) {
//    override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
//  }
//
//      val regularStatement = DesSummary(
//        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
//        sex = "F",
//        statePensionAgeDate = new LocalDate(2042, 4, 6),
//        finalRelevantStartYear = 2018,
//        pensionSharingOrderSERPS = false,
//        dateOfBirth = new LocalDate(1978, 4, 6),
//        amounts = DesStatePensionAmounts(
//          pensionEntitlement = 161.18,
//          startingAmount2016 = 161.18,
//          protectedPayment2016 = 5.53,
//          DesAmountA2016(
//            basicStatePension = 119.3,
//            pre97AP = 17.79,
//            post97AP = 6.03,
//            post02AP = 15.4,
//            pre88GMP = 0,
//            post88GMP = 0,
//            pre88COD = 0,
//            post88COD = 0,
//            graduatedRetirementBenefit = 2.66
//          ),
//          DesAmountB2016(
//            mainComponent = 155.65,
//            rebateDerivedAmount = 0
//          )
//        )
//      )
//
//      when(NEWmockDesConnector.getSummary(Matchers.any())(Matchers.any()))
//        .thenReturn(Future.successful(regularStatement))
//
//      when(NEWmockDesConnector.getLiabilities(Matchers.any())(Matchers.any()))
//        .thenReturn(Future.successful(List()))
//
//      when(NEWmockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
//        .thenReturn(Future.successful(DesNIRecord(qualifyingYears = 36, List())))
//
//      when(NEWmockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any()))
//        .thenReturn(Future.successful(false))
//
//      lazy val statePensionF: Future[StatePension] = NEWService.getStatement(generateNino()).right.get
//
//      "statePension have statePensionAgeUnderConsideration flag as false" in {
//        whenReady(statePensionF) { statePension =>
//          statePension.statePensionAgeUnderConsideration shouldBe false
//        }
//      }
//
//      "log a summary metric" in {
//        verify(NEWmockMetrics, Mockito.atLeastOnce()).summary(
//          Matchers.eq[BigDecimal](161.18),
//          Matchers.eq[BigDecimal](161.18),
//          Matchers.eq(false),
//          Matchers.eq(Scenario.Reached),
//          Matchers.eq[BigDecimal](161.18),
//          Matchers.eq(0),
//          Matchers.eq(None),
//          Matchers.eq[BigDecimal](161.18),
//          Matchers.eq[BigDecimal](119.3),
//          Matchers.eq[BigDecimal](39.22),
//          Matchers.eq[BigDecimal](2.66),
//          Matchers.eq[BigDecimal](155.65),
//          Matchers.eq[BigDecimal](0),
//          Matchers.eq(false),
//          Matchers.eq(None),
//          Matchers.eq(false),
//          Matchers.eq(false)
//        )
//      }
//    }
  }
}
