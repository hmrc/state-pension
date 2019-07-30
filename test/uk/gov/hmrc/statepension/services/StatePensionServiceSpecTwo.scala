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
import org.mockito.Matchers
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.builders.RateServiceBuilder
import uk.gov.hmrc.statepension.connectors.{DesConnector, StatePensionAuditConnector}
import uk.gov.hmrc.statepension.domain.{Exclusion, NewRules, OldRules, StatePension, StatePensionAmount, StatePensionAmounts, StatePensionExclusion}
import uk.gov.hmrc.statepension.domain.nps.{DesAmountB2016, DesLiability, DesNIRecord, DesNITaxYear, DesStatePensionAmounts, DesSummary}

import scala.concurrent.Future

class StatePensionServiceSpecTwo extends StatePensionUnitSpec
  with OneAppPerSuite
  with ScalaFutures
  with MockitoSugar {

  "StatePensionService with a HOD Connection" when {

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

    val summary = DesSummary(
      earningsIncludedUpTo = new LocalDate(2016, 4, 5),
      sex = "M",
      statePensionAgeDate = new LocalDate(2018, 1, 1),
      finalRelevantStartYear = 2049,
      pensionSharingOrderSERPS = false,
      dateOfBirth = new LocalDate(1956, 7, 7)
    )

    when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any())).thenReturn(Future.successful(false))

    "the customer has male overseas auto credits (abroad exclusion)" should {

      val summary = DesSummary(
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


      when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        List()
      ))
      when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        DesNIRecord(qualifyingYears = 35, List(DesNITaxYear(Some(2000), Some(false), Some(false), Some(true)), DesNITaxYear(Some(2001), Some(false), Some(false), Some(true))))
      ))

      lazy val statePensionF: Future[StatePension] = service.getStatement(generateNino()).right.get

      "return StatePension object" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))
        whenReady(statePensionF) { statePension =>
          statePension shouldBe StatePension(new LocalDate("2016-04-05"), StatePensionAmounts(false, StatePensionAmount(None, None, 0.00), StatePensionAmount(Some(34), None, 151.20), StatePensionAmount(Some(0), Some(2), 155.65), StatePensionAmount(None, None, 0), StatePensionAmount(None, None, 0), OldRules(0, 0, 0), NewRules(0, 0)), 61, new LocalDate("2018-01-01"), "2049-50", 35, false, 155.65, false, None, true, false)
        }
      }

      "have a pension age of 61" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))
        whenReady(statePensionF) { statePension =>
          statePension.pensionAge shouldBe 61
        }
      }

      "have a pension date of 2018-1-1" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))
        whenReady(statePensionF) { statePension =>
          statePension.pensionDate shouldBe new LocalDate(2018, 1, 1)
        }
      }

      "log a summary metric" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))
        verify(mockMetrics, times(1)).summary(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
      }

    }

    "the customer has amount dissonance" should {

      val summary = DesSummary(
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


      when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        List()
      ))
      when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        DesNIRecord(qualifyingYears = 35, List(DesNITaxYear(Some(2000), Some(false), Some(false), Some(true)), DesNITaxYear(Some(2001), Some(false), Some(false), Some(true))))
      ))


      lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get

      "return amount dissonance" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))

        whenReady(exclusionF) { exclusion =>
          exclusion.exclusionReasons shouldBe List(Exclusion.AmountDissonance)
        }
      }

      "have a pension age of 61" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionAge shouldBe 61
        }
      }

      "have a pension date of 2018-1-1" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionDate shouldBe new LocalDate(2018, 1, 1)
        }
      }

      "not have the statePensionAgeUnderConsideration flag enabled" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))
        whenReady(exclusionF) { exclusion =>
          exclusion.statePensionAgeUnderConsideration shouldBe false
        }
      }

      "log an exclusion metric" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))
        verify(mockMetrics, times(1)).exclusion(
          Matchers.eq(Exclusion.AmountDissonance)
        )
      }

      "not log a summary metric" in {
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))
        verify(mockMetrics, never).summary(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
      }
    }

    "the customer has contributed national insurance in the isle of man" should {

      when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        DesNIRecord(qualifyingYears = 35, List(DesNITaxYear(Some(2000), Some(false), Some(false), Some(true)), DesNITaxYear(Some(2001), Some(false), Some(false), Some(true))))
      ))

      lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get

      "return isle of man exclusion" in {
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          List(DesLiability(Some(5)))
        ))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))

        whenReady(exclusionF) { exclusion =>
          exclusion.exclusionReasons shouldBe List(Exclusion.IsleOfMan)
        }
      }

      "have a pension age of 61" in {
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          List(DesLiability(Some(5)))
        ))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionAge shouldBe 61
        }
      }

      "have a pension date of 2018-1-1" in {
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          List(DesLiability(Some(5)))
        ))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))
        whenReady(exclusionF) { exclusion =>
          exclusion.pensionDate shouldBe new LocalDate(2018, 1, 1)
        }
      }

      "not have the statePensionAgeUnderConsideration flag enabled" in {
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          List(DesLiability(Some(5)))
        ))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))
        whenReady(exclusionF) { exclusion =>
          exclusion.statePensionAgeUnderConsideration shouldBe false
        }
      }

      "log an exclusion metric" in {
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          List(DesLiability(Some(5)))
        ))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))
        verify(mockMetrics, times(1)).exclusion(
          Matchers.eq(Exclusion.IsleOfMan)
        )
      }

      "not log a summary metric" in {
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          List(DesLiability(Some(5)))
        ))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))
        verify(mockMetrics, never).summary(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
      }
    }

    "the customer has a manual correspondence indicator" should {

      when(mockDesConnector.getNIRecord(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
        DesNIRecord(qualifyingYears = 35, List(DesNITaxYear(Some(2000), Some(false), Some(false), Some(true)), DesNITaxYear(Some(2001), Some(false), Some(false), Some(true))))
      ))

      lazy val exclusionF: Future[StatePensionExclusion] = service.getStatement(generateNino()).left.get

      "return mci exclusion" in {
        when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any())).thenReturn(Future.successful(true))
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          List()
        ))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))
        whenReady(exclusionF) { exclusion =>
          exclusion.exclusionReasons shouldBe List(Exclusion.ManualCorrespondenceIndicator)
        }
      }

      "have a pension age of 61" in {
        when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any())).thenReturn(Future.successful(true))
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          List()
        ))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))

        whenReady(exclusionF) { exclusion =>
          exclusion.pensionAge shouldBe 61
        }
      }

      "have a pension date of 2018-1-1" in {
        when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any())).thenReturn(Future.successful(true))
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          List()
        ))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))

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

        whenReady(exclusionF) { exclusion =>
          exclusion.statePensionAgeUnderConsideration shouldBe false
        }
      }

      "log an exclusion metric" in {
        when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any())).thenReturn(Future.successful(true))
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          List()
        ))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))

        verify(mockMetrics, times(1)).exclusion(
          Matchers.eq(Exclusion.ManualCorrespondenceIndicator)
        )
      }

      "not log a summary metric" in {
        when(mockCitizenDetails.checkManualCorrespondenceIndicator(Matchers.any())(Matchers.any())).thenReturn(Future.successful(true))
        when(mockDesConnector.getLiabilities(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          List()
        ))
        when(mockDesConnector.getSummary(Matchers.any())(Matchers.any())).thenReturn(Future.successful(
          summary
        ))

        verify(mockMetrics, never).summary(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(),
          Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
      }
    }
  }
}
