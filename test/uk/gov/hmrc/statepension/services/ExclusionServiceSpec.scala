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

import org.mockito.Mockito.when
import uk.gov.hmrc.statepension.config.{AppConfig, StatePensionExclusionOffset}

import java.time.{LocalDate, Duration}
import uk.gov.hmrc.statepension.domain.Exclusion
import uk.gov.hmrc.statepension.domain.nps.Liability
import utils.StatePensionBaseSpec

class ExclusionServiceSpec extends StatePensionBaseSpec {

  val mockAppConfig: AppConfig = mock[AppConfig]

  case class OffsetReturn(
                           days: Int = 1,
                           weeks: Int = 0,
                           months: Int = 0,
                           years: Int = 0
                         ) {
    def dateDelta(start: LocalDate): Long =
        Duration.between(start.atStartOfDay(),
          start
            .plusYears(years)
            .plusMonths(months)
            .plusDays(days)
            .atStartOfDay()
        ).toDays

  }

  "getExclusions" when {
    "there is no exclusions" should {
      "return an empty list" in new Setup {
        exclusionServiceBuilder(dateOfDeath = None).getExclusions shouldBe Nil
      }
    }

    "there is a date of death" should {
      "return a List(Dead)" in new Setup {
        exclusionServiceBuilder(dateOfDeath = Some(LocalDate.of(2000, 9, 13))).getExclusions shouldBe List(Exclusion.Dead)
      }
    }

    List(
      OffsetReturn(days = 1),
      OffsetReturn(days = 30),
      OffsetReturn(days = 8, months = 1)
    ).foreach { offset =>
      s"checking for post state pension age when the delta between current date and acceptable Pension Date is " +
        s"${offset.months} month(s) and ${offset.days} day(s)" should {
        val now: LocalDate = LocalDate.now()

        "return a List(PostStatePensionAge)" when {
          "the state pension age is the same as the current date" in new Setup(offset) {
            exclusionServiceBuilder(pensionDate = now, now = now).getExclusions shouldBe List(Exclusion.PostStatePensionAge)
          }
          s"the state pension age is ${offset.days} day(s) after the the current date" in new Setup(offset) {
            exclusionServiceBuilder(
              pensionDate = now
                .plusMonths(offset.months)
                .plusDays(offset.days), now = now)
              .getExclusions shouldBe List(Exclusion.PostStatePensionAge)
          }
          "the state pension age is one day before the the current date" in new Setup(offset) {
            exclusionServiceBuilder(pensionDate = now.minusDays(1), now = now).getExclusions shouldBe List(Exclusion.PostStatePensionAge)
          }
        }

        "return an empty list" when {
          s"the state pension age is ${offset.dateDelta(now) + 1} days after the current date" in new Setup(offset) {
            exclusionServiceBuilder(
              pensionDate = now.plusDays(offset.dateDelta(now) + 1),
              now = now
            )
              .getExclusions shouldBe List()
          }
        }
      }
    }

    "the amount dissonance criteria is met" when  {
      "the startingAmount and calculatedStartingAmount are the same" should {
        "return no exclusions" in new Setup {
          exclusionServiceBuilder(startingAmount = 155.65, calculatedStartingAmount = 155.65).getExclusions shouldBe Nil
        }
      }
      "the startingAmount and calculatedStartingAmount are the same but entitlement differs" should {
        "return no exclusions" in new Setup {
          exclusionServiceBuilder(entitlement = 155, startingAmount = 155.65, calculatedStartingAmount = 155.65).getExclusions shouldBe Nil
        }
      }
      "the startingAmount and calculatedStartingAmount are different" should {
        "return List(AmountDissonance)" in new Setup {
          exclusionServiceBuilder(startingAmount = 155.65, calculatedStartingAmount = 101).getExclusions shouldBe List(Exclusion.AmountDissonance)
        }
      }
      "the startingAmount and calculatedStartingAmount are different but entitlement matches" should {
        "return List(AmountDissonance)" in new Setup {
          exclusionServiceBuilder(entitlement = 101, startingAmount = 155.65, calculatedStartingAmount = 101).getExclusions shouldBe List(Exclusion.AmountDissonance)
        }
      }
    }

    "the isle of man criteria is met" when {
      "there is no liabilities" should  {
        "return no exclusions" in new Setup {
          exclusionServiceBuilder(liabilities = List()).getExclusions shouldBe Nil
        }
      }
      "there is some liabilities" should {
        "return List(IsleOfMan) if the list includes liability type 5" in new Setup {
          exclusionServiceBuilder(liabilities = List(Liability(Some(5)), Liability(Some(16)))).getExclusions shouldBe List(Exclusion.IsleOfMan)
        }
        "return no exclusions if the list does not include liability type 5" in new Setup {
          exclusionServiceBuilder(liabilities = List(Liability(Some(15)), Liability(Some(16)))).getExclusions shouldBe Nil
        }
      }
    }

    "there is manual correspondence only" should {
      "return List(ManualCorrespondenceIndicator)" in new Setup {
        exclusionServiceBuilder(manualCorrespondenceOnly = true).getExclusions shouldBe List(Exclusion.ManualCorrespondenceIndicator)
      }
    }

    "there is not manual correspondence only" should {
      "return no exclusions" in new Setup {
        exclusionServiceBuilder(manualCorrespondenceOnly = false).getExclusions shouldBe Nil
      }
    }

    "all the exclusion criteria are met" should {
      "return a sorted list of Dead, PostSPA, MWRRE, CopeProcessing" in new Setup {
        exclusionServiceBuilder(
          dateOfDeath = Some(LocalDate.of(1999, 12, 31)),
          pensionDate = LocalDate.of(2000, 1, 1),
          now = LocalDate.of(2000, 1, 1),
          entitlement = 100,
          startingAmount = 100,
          calculatedStartingAmount = 101,
          liabilities = List(Liability(Some(5)), Liability(Some(5)), Liability(Some(1))),
          manualCorrespondenceOnly = true
        ).getExclusions shouldBe List(
          Exclusion.Dead,
          Exclusion.ManualCorrespondenceIndicator,
          Exclusion.PostStatePensionAge,
          Exclusion.AmountDissonance,
          Exclusion.IsleOfMan
        )
      }
    }
  }

  class Setup(offset: OffsetReturn = OffsetReturn(1, 0, 0, 0)) {
    val exampleNow: LocalDate = LocalDate.of(2017, 2, 16)
    val examplePensionDate: LocalDate = LocalDate.of(2022, 2, 2)

    when(mockAppConfig.statePensionExclusionOffset)
      .thenReturn(StatePensionExclusionOffset(years = offset.years, months = offset.months, weeks = offset.weeks, days = offset.days))

    def exclusionServiceBuilder(dateOfDeath: Option[LocalDate] = None,
                                pensionDate: LocalDate = examplePensionDate,
                                now: LocalDate = exampleNow,
                                entitlement: BigDecimal = 0,
                                startingAmount: BigDecimal = 0,
                                calculatedStartingAmount: BigDecimal = 0,
                                liabilities: List[Liability] = List(),
                                manualCorrespondenceOnly: Boolean = false,
                                appConfig: AppConfig = mockAppConfig): ExclusionService =
      ExclusionService(dateOfDeath, pensionDate, now, entitlement, startingAmount, calculatedStartingAmount, liabilities, manualCorrespondenceOnly, appConfig)

  }
}
