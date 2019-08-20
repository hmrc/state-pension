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
import org.mockito.Mockito.{never, times, verify, when}
import org.mockito.{Matchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.builders.RateServiceBuilder
import uk.gov.hmrc.statepension.connectors.{DesConnector, StatePensionAuditConnector}
import uk.gov.hmrc.statepension.domain.MQPScenario.ContinueWorking
import uk.gov.hmrc.statepension.domain.nps._
import uk.gov.hmrc.statepension.domain._

import scala.concurrent.Future

class StatePensionServiceCustomerSpec extends StatePensionUnitSpec
  with OneAppPerSuite
  with ScalaFutures
  with MockitoSugar
  with BeforeAndAfterEach {


  val mockDesConnector: DesConnector = mock[DesConnector]
  val mockStatePensionAuditConnector = mock[StatePensionAuditConnector]
  val mockMetrics: ApplicationMetrics = mock[ApplicationMetrics]
  val mockCitizenDetails: CitizenDetailsService = mock[CitizenDetailsService]
  val defaultForecasting = new ForecastingService(rateService = RateServiceBuilder.default)

  lazy val service: StatePensionService = new StatePensionService(mockDesConnector,
    mockCitizenDetails,
    defaultForecasting,
    RateServiceBuilder.default,
    mockMetrics,
    mockStatePensionAuditConnector) {
    override lazy val now: LocalDate = new LocalDate(2017, 2, 16)
  }

  val summary = DesSummary(
    earningsIncludedUpTo = new LocalDate(2016, 4, 5),
    sex = "M",
    statePensionAgeDate = new LocalDate(2018, 1, 1),
    finalRelevantStartYear = 2049,
    pensionSharingOrderSERPS = false,
    dateOfBirth = new LocalDate(1956, 7, 7)
  )

  override def beforeEach: Unit = {
    Mockito.reset(mockDesConnector, mockMetrics, mockCitizenDetails, mockStatePensionAuditConnector)

    when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(false))

    when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(DesNIRecord(
        qualifyingYears = 35,
        List(
          DesNITaxYear(Some(2000), Some(false), Some(false), Some(true)),
          DesNITaxYear(Some(2001), Some(false), Some(false), Some(true))
        )
      )))

    when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(List()))

  }

  "StatePensionService with a HOD Connection" when {

    "the customer is dead" should {

      val summary = DesSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "F",
        statePensionAgeDate = new LocalDate(2050, 7, 7),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1983, 7, 7),
        dateOfDeath = Some(new LocalDate(2000, 9, 13)),
        reducedRateElection = false,
        countryCode = 1,
        DesStatePensionAmounts()
      )


      "return dead exclusion" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))

        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          exclusion.exclusionReasons shouldBe List(Exclusion.Dead)
        }
      }

      "have a pension age of 67" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))

        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionAge shouldBe 67
        }
      }

      "have a pension date of 2050-7-7" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))

        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionDate shouldBe new LocalDate(2050, 7, 7)
        }
      }

      "not have the statePensionAgeUnderConsideration flag enabled" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))

        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          exclusion.statePensionAgeUnderConsideration shouldBe false
        }
      }

      "log an exclusion metric" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))

        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          verify(mockMetrics, times(1)).exclusion(
            Matchers.eq(Exclusion.Dead)
          )
        }
      }

      "not log a summary metric" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))

        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        verify(mockMetrics, never).summary(Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any())
      }

    }

    "the customer is over state pension age" should {

      val summary = DesSummary(
        earningsIncludedUpTo = new LocalDate(1954, 4, 5),
        sex = "F",
        statePensionAgeDate = new LocalDate(2016, 1, 1),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1954, 7, 7),
        dateOfDeath = None,
        reducedRateElection = false,
        countryCode = 1,
        DesStatePensionAmounts()
      )

      "return post state pension age exclusion" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))

        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          exclusion.exclusionReasons shouldBe List(Exclusion.PostStatePensionAge)
        }
      }

      "have a pension age of 61" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionAge shouldBe 61
        }
      }

      "have a pension date of 2016-1-1" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionDate shouldBe new LocalDate(2016, 1, 1)
        }
      }

      "not have the statePensionAgeUnderConsideration flag enabled" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          exclusion.statePensionAgeUnderConsideration shouldBe false
        }
      }

      "log an exclusion metric" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))

        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          verify(mockMetrics, times(1)).exclusion(
            Matchers.eq(Exclusion.PostStatePensionAge)
          )
        }
      }

      "not log a summary metric" in {
        verify(mockMetrics, never).summary(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
      }
    }

    "the customer has married women's reduced rate election" should {

      val summary1 = DesSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "F",
        statePensionAgeDate = new LocalDate(2018, 1, 1),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1956, 7, 7),
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

      "summary have RRE flag as true" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary1
        ))
        lazy val summaryF: Future[DesSummary] = mockDesConnector.getSummary(Matchers.any())(Matchers.any())
        whenReady(summaryF) { summary =>
          summary.reducedRateElection shouldBe true
        }
      }

      "statePension have RRE flag as true" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary1
        ))
        lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get
        whenReady(statePensionF) { statePension =>
          statePension.reducedRateElection shouldBe true
        }
      }

      "statePension" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary1
        ))
        lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get
        whenReady(statePensionF) { statePension =>
          statePension.reducedRateElectionCurrentWeeklyAmount shouldBe Some(32.61)
        }
      }

      "log a summary metric" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary1))

        when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          DesNIRecord(qualifyingYears = 9, List(DesNITaxYear(Some(2000), Some(false), Some(false), Some(true)), DesNITaxYear(Some(2001), Some(false), Some(false), Some(true))))
        ))
        lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get
        whenReady(statePensionF) { _ =>
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
            Matchers.eq(false)
          )
        }
      }
    }

    "the customer has male overseas auto credits (abroad exclusion)" should {

      val NEWsummary = DesSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "M",
        statePensionAgeDate = new LocalDate(2018, 1, 1),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1956, 7, 7),
        dateOfDeath = None,
        reducedRateElection = false,
        countryCode = 200,
        DesStatePensionAmounts()
      )


      "return StatePension object" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(NEWsummary))
        lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get

        whenReady(statePensionF) { statePension =>
          statePension shouldBe StatePension(new LocalDate("2016-04-05"), StatePensionAmounts(false, StatePensionAmount(None, None, 0.00), StatePensionAmount(Some(34), None, 151.20), StatePensionAmount(Some(0), Some(2), 155.65), StatePensionAmount(None, None, 0), StatePensionAmount(None, None, 0), OldRules(0, 0, 0), NewRules(0, 0)), 61, new LocalDate("2018-01-01"), "2049-50", 35, false, 155.65, false, None, false)
        }
      }

      "have a pension age of 61" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(NEWsummary))
        lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get

        whenReady(statePensionF) { statePension =>
          statePension.pensionAge shouldBe 61
        }
      }

      "have a pension date of 2018-1-1" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(NEWsummary))
        lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get

        whenReady(statePensionF) { statePension =>
          statePension.pensionDate shouldBe new LocalDate(2018, 1, 1)
        }
      }

      "log a summary metric" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(NEWsummary))
        lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get

        whenReady(statePensionF) { _ =>
          verify(mockMetrics, times(1)).summary(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
            Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
            Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
        }
      }

    }

    "the customer has amount dissonance" should {

      lazy val summary = DesSummary(
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        sex = "M",
        statePensionAgeDate = new LocalDate(2018, 1, 1),
        finalRelevantStartYear = 2049,
        pensionSharingOrderSERPS = false,
        dateOfBirth = new LocalDate(1956, 7, 7),
        amounts = DesStatePensionAmounts(
          pensionEntitlement = 155.65,
          startingAmount2016 = 155.65,
          amountB2016 = DesAmountB2016(
            mainComponent = 155.64
          )
        )
      )

      lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get

      "return amount dissonance" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get

        whenReady(exclusionF) { exclusion =>
          exclusion.exclusionReasons shouldBe List(Exclusion.AmountDissonance)
        }
      }

      "have a pension age of 61" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get

        whenReady(exclusionF) { exclusion =>
          exclusion.pensionAge shouldBe 61
        }
      }

      "have a pension date of 2018-1-1" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get

        whenReady(exclusionF) { exclusion =>
          exclusion.pensionDate shouldBe new LocalDate(2018, 1, 1)
        }
      }

      "not have the statePensionAgeUnderConsideration flag enabled" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get

        whenReady(exclusionF) { exclusion =>
          exclusion.statePensionAgeUnderConsideration shouldBe false
        }
      }

      "log an exclusion metric" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get

        whenReady(exclusionF) { _ =>
          verify(mockMetrics, times(1)).exclusion(
            Matchers.eq(Exclusion.AmountDissonance)
          )
        }
      }

      "not log a summary metric" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get

        verify(mockMetrics, never).summary(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
      }
    }

    "the customer has contributed national insurance in the isle of man" should {


      "return isle of man exclusion" in {
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(
            List(DesLiability(Some(5)))
          ))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          exclusion.exclusionReasons shouldBe List(Exclusion.IsleOfMan)
        }
      }

      "have a pension age of 61" in {
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(
            List(DesLiability(Some(5)))
          ))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionAge shouldBe 61
        }
      }

      "have a pension date of 2018-1-1" in {
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(
            List(DesLiability(Some(5)))
          ))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionDate shouldBe new LocalDate(2018, 1, 1)
        }
      }

      "not have the statePensionAgeUnderConsideration flag enabled" in {
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(
            List(DesLiability(Some(5)))
          ))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          exclusion.statePensionAgeUnderConsideration shouldBe false
        }
      }

      "log an exclusion metric" in {
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(
            List(DesLiability(Some(5)))
          ))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          verify(mockMetrics, times(1)).exclusion(
            Matchers.eq(Exclusion.IsleOfMan)
          )
        }
      }

      "not log a summary metric" in {
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(
            List(DesLiability(Some(5)))
          ))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        verify(mockMetrics, never).summary(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
      }
    }

    "the customer has a manual correspondence indicator" should {


      "return mci exclusion" in {
        when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(true))

        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          exclusion.exclusionReasons shouldBe List(Exclusion.ManualCorrespondenceIndicator)
        }
      }

      "have a pension age of 61" in {
        when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(true))

        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionAge shouldBe 61
        }
      }

      "have a pension date of 2018-1-1" in {
        when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(true))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionDate shouldBe new LocalDate(2018, 1, 1)
        }
      }

      "not have the statePensionAgeUnderConsideration flag enabled" in {
        when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any())).thenReturn(Future.successful(true))
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          List()
        ))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          exclusion.statePensionAgeUnderConsideration shouldBe false
        }
      }

      "log an exclusion metric" in {
        when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(true))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        whenReady(exclusionF) { exclusion =>
          verify(mockMetrics, times(1)).exclusion(
            Matchers.eq(Exclusion.ManualCorrespondenceIndicator)
          )
        }
      }

      "not log a summary metric" in {
        when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(true))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(summary))
        lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get
        verify(mockMetrics, never).summary(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
      }
    }
  }
}
