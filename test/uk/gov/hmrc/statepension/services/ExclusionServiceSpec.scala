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
import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.domain.Exclusion
import uk.gov.hmrc.statepension.domain.nps.Liability

class ExclusionServiceSpec extends StatePensionUnitSpec {

  val exampleNow = new LocalDate(2017, 2, 16)
  val examplePensionDate = new LocalDate(2022, 2, 2)

  def exclusionServiceBuilder(dateOfDeath: Option[LocalDate] = None,
                              pensionDate: LocalDate = examplePensionDate,
                              now: LocalDate = exampleNow,
                              entitlement: BigDecimal = 0,
                              startingAmount: BigDecimal = 0,
                              calculatedStartingAmount: BigDecimal = 0,
                              liabilities: List[Liability] = List(),
                              manualCorrespondenceOnly: Boolean = false) =
    ExclusionService(dateOfDeath, pensionDate, now, entitlement, startingAmount, calculatedStartingAmount, liabilities, manualCorrespondenceOnly)

  "getExclusions" when {
    "there is no exclusions" should {
      "return an empty list" in {
        exclusionServiceBuilder(dateOfDeath = None).getExclusions shouldBe Nil
      }
    }

    "there is a date of death" should {
      "return a List(Dead)" in {
        exclusionServiceBuilder(dateOfDeath = Some(new LocalDate(2000, 9, 13))).getExclusions shouldBe List(Exclusion.Dead)
      }
    }

    "checking for post state pension age" should {
      "return a List(PostStatePensionAge)" when {
        "the state pension age is the same as the current date" in {
          exclusionServiceBuilder(pensionDate = new LocalDate(2000, 1, 1), now = new LocalDate(2000, 1, 1)).getExclusions shouldBe List(Exclusion.PostStatePensionAge)
        }
        "the state pension age is one day after the the current date" in {
          exclusionServiceBuilder(pensionDate = new LocalDate(2000, 1, 2), now = new LocalDate(2000, 1, 1)).getExclusions shouldBe List(Exclusion.PostStatePensionAge)
        }
        "the state pension age is one day before the the current date" in {
          exclusionServiceBuilder(pensionDate = new LocalDate(2000, 1, 1), now = new LocalDate(2000, 1, 2)).getExclusions shouldBe List(Exclusion.PostStatePensionAge)
        }
      }

      "return an empty list" when {
        "the state pension age is two days after the current date" in {
          exclusionServiceBuilder(pensionDate = new LocalDate(2000, 1, 3), now = new LocalDate(2000, 1, 1)).getExclusions shouldBe List()
        }
      }
    }

   "the amount dissonance criteria is met" when  {
      "the startingAmount and calculatedStartingAmount are the same" should {
        "return no exclusions" in {
          exclusionServiceBuilder(startingAmount = 155.65, calculatedStartingAmount = 155.65).getExclusions shouldBe Nil
        }
      }
      "the startingAmount and calculatedStartingAmount are the same but entitlement differs" should {
        "return no exclusions" in {
          exclusionServiceBuilder(entitlement = 155, startingAmount = 155.65, calculatedStartingAmount = 155.65).getExclusions shouldBe Nil
        }
      }
      "the startingAmount and calculatedStartingAmount are different" should {
        "return List(AmountDissonance)" in {
          exclusionServiceBuilder(startingAmount = 155.65, calculatedStartingAmount = 101).getExclusions shouldBe List(Exclusion.AmountDissonance)
        }
      }
      "the startingAmount and calculatedStartingAmount are different but entitlement matches" should {
        "return List(AmountDissonance)" in {
          exclusionServiceBuilder(entitlement = 101, startingAmount = 155.65, calculatedStartingAmount = 101).getExclusions shouldBe List(Exclusion.AmountDissonance)
        }
      }
    }

    "the isle of man criteria is met" when {
      "there is no liabilities" should  {
        "return no exclusions" in {
          exclusionServiceBuilder(liabilities = List()).getExclusions shouldBe Nil
        }
      }
      "there is some liabilities" should {
        "return List(IsleOfMan) if the list includes liability type 5" in {
          exclusionServiceBuilder(liabilities = List(Liability(Some(5)), Liability(Some(16)))).getExclusions shouldBe List(Exclusion.IsleOfMan)
        }
        "return no exclusions if the list does not include liability type 5" in {
          exclusionServiceBuilder(liabilities = List(Liability(Some(15)), Liability(Some(16)))).getExclusions shouldBe Nil
        }
      }
    }

    "there is manual correspondence only" should {
      "return List(ManualCorrespondenceIndicator)" in {
        exclusionServiceBuilder(manualCorrespondenceOnly = true).getExclusions shouldBe List(Exclusion.ManualCorrespondenceIndicator)
      }
    }

    "there is not manual correspondence only" should {
      "return no exclusions" in {
        exclusionServiceBuilder(manualCorrespondenceOnly = false).getExclusions shouldBe Nil
      }
    }

    "all the exclusion criteria are met" should {
      "return a sorted list of Dead, PostSPA, MWRRE" in {
        exclusionServiceBuilder(
          dateOfDeath = Some(new LocalDate(1999, 12, 31)),
          pensionDate = new LocalDate(2000, 1, 1),
          now = new LocalDate(2000, 1, 1),
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
}
