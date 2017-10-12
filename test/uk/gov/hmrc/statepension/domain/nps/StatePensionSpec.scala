/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.statepension.domain.nps

import org.joda.time.LocalDate
import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.domain._

class StatePensionSpec extends StatePensionUnitSpec {

  "StatePensionAmount" should {
    "Weekly / Monthly / Annual Calculation" should {
      "return 151.25, 657.67, 7892.01" in {
        StatePensionAmount(None, None, 151.25).monthlyAmount shouldBe 657.67
        StatePensionAmount(None, None, 151.25).annualAmount shouldBe 7892.01
      }

      "return 43.21, 187.89, 2254.64" in {
        StatePensionAmount(None, None, 43.21).monthlyAmount shouldBe 187.89
        StatePensionAmount(None, None, 43.21).annualAmount shouldBe 2254.64
      }

      "return 95.07, 413.38, 4960.62" in {
        StatePensionAmount(None, None, 95.07).monthlyAmount shouldBe 413.38
        StatePensionAmount(None, None, 95.07).annualAmount shouldBe 4960.62
      }

      "yearsToWork and gapsToFill have no bearing on calculation" in {
        StatePensionAmount(Some(2), None, 95.07).monthlyAmount shouldBe 413.38
        StatePensionAmount(None, Some(2), 95.07).annualAmount shouldBe 4960.62
        StatePensionAmount(None, Some(2), 95.07).monthlyAmount shouldBe 413.38
        StatePensionAmount(Some(2), None, 95.07).annualAmount shouldBe 4960.62
        StatePensionAmount(Some(2), Some(2), 95.07).monthlyAmount shouldBe 413.38
        StatePensionAmount(Some(2), Some(2), 95.07).annualAmount shouldBe 4960.62
      }
    }
  }

  private def createStatePension(cope: BigDecimal = 0,
                         finalRelevantYear: String = "2018-19",
                         earningsIncludedUpTo: Int = 2015,
                         currentAmount: BigDecimal = 0,
                         forecastAmount: BigDecimal = 0,
                         maximumAmount: BigDecimal = 0,
                         fullStatePensionAmount: BigDecimal = 155.65,
                         qualifyingYears: Int = 30,
                         reducedRateElection:Boolean = false,
                         startingAmount: BigDecimal = 160.18,
                         oldRules: OldRules = OldRules(basicStatePension = 119.30,
                                                       additionalStatePension= 30.00,
                                                       graduatedRetirementBenefit =10.88),
                         newRules: NewRules = NewRules(grossStatePension = 155.65,
                                                       rebateDerivedAmount= 0.00)
                        ) = {
    StatePension(
      new LocalDate(earningsIncludedUpTo + 1, 4, 5),
      amounts = StatePensionAmounts(
        false,
        StatePensionAmount(None, None, currentAmount),
        StatePensionAmount(None, None, forecastAmount),
        StatePensionAmount(None, None, maximumAmount),
        cope = StatePensionAmount(None, None, cope),
        starting = StatePensionAmount(None, None, startingAmount),
        oldRules,
        newRules
      ),
      65,
      new LocalDate(2019, 5, 1),
      finalRelevantYear,
      qualifyingYears,
      false,
      fullStatePensionAmount,
      reducedRateElection
    )
  }

  "starting" should {
    "return starting amount" in {
        createStatePension().amounts.starting shouldBe StatePensionAmount(None,None,160.18)
        createStatePension().amounts.starting.weeklyAmount shouldBe 160.18
        createStatePension().amounts.starting.monthlyAmount shouldBe 696.50
        createStatePension().amounts.starting.annualAmount shouldBe 8357.96
    }
  }

  "oldRules" should {
    "return OldRules where basicStatePension is 119.30 graduatedRetirementBenefit is 10.88 and additionalStatePension is 30.00" in {
        createStatePension().amounts.oldRules shouldBe
            OldRules(basicStatePension = 119.30,additionalStatePension=30.00,graduatedRetirementBenefit =10.88)
    }
    "return OldRules where basicStatePension is 119.30, additionalStatePension is 20.00 and graduatedRetirementBenefit is 10.00" in {
       createStatePension(oldRules = OldRules(basicStatePension = 119.30,additionalStatePension=20.00, graduatedRetirementBenefit=10.00))
           .amounts.oldRules shouldBe OldRules(basicStatePension = 119.30,additionalStatePension=20.00,graduatedRetirementBenefit=10.00)
    }
  }

  "newRules" should {
    "return NewRules where grossStatePension is 119.30 and rebateDerivedAmount is 0.00" in {
        createStatePension().amounts.newRules shouldBe
            NewRules(grossStatePension = 155.65, rebateDerivedAmount=0.00)
    }
    "return NewRules where grossStatePension is 119.30 and rebateDerivedAmount is 20.00" in {
       createStatePension(newRules = NewRules(grossStatePension = 119.30, rebateDerivedAmount=20.00))
           .amounts.newRules shouldBe NewRules(grossStatePension = 119.30, rebateDerivedAmount=20.00)
    }
  }

  "contractedOut" should {
    "return true when the user has a COPE amount more than 0" in {
      createStatePension(cope = 0.87).contractedOut shouldBe true
    }
    "return false when the user has a COPE amount of 0" in {
      createStatePension(cope = 0).contractedOut shouldBe false
    }
  }

  "MWRRE customer" should {
    "return false to Non-RRE Customers" in {
      createStatePension(reducedRateElection = false).reducedRateElection shouldBe false
    }

    "return true to RRE Customers" in {
      createStatePension(reducedRateElection = true).reducedRateElection shouldBe true
    }
  }

  "mqpScenario" should {

    "should be an MQP Scenario if they have less than 10 years" in {
      createStatePension(qualifyingYears = 4, currentAmount = 50, forecastAmount = 0, maximumAmount = 0).mqpScenario.isDefined shouldBe true
    }

    "be None if they have a Current Amount of more than 0" in {
      createStatePension(currentAmount = 122.34).mqpScenario shouldBe None
    }
    "be ContinueWorking if they have a Current Amount of 0, ForecastAmount more than 0" in {
      createStatePension(currentAmount = 0, forecastAmount = 89.34).mqpScenario shouldBe Some(MQPScenario.ContinueWorking)
    }
    "be CanGetWithGaps if they have a Current Amount of 0, Forecast Amount of 0 and a Maximum more than 0" in {
      createStatePension(currentAmount = 0, forecastAmount = 0, maximumAmount = 250.99).mqpScenario shouldBe Some(MQPScenario.CanGetWithGaps)
    }
    "be CantGet if all the amounts are 0" in {
      createStatePension(currentAmount = 0, forecastAmount = 0, maximumAmount = 0).mqpScenario shouldBe Some(MQPScenario.CantGet)
    }
  }

  "forecastScenario" should {
    "be ForecastOnly when Forecast Amount is less than the Current Amount" in {
      createStatePension(currentAmount = 20, forecastAmount = 10, maximumAmount = 10).forecastScenario shouldBe Scenario.ForecastOnly
    }
    "be Reached when current, forecast and maximum are all the same" in {
      createStatePension(currentAmount = 10, forecastAmount = 10, maximumAmount = 10).forecastScenario shouldBe Scenario.Reached
    }
    "be FillGaps when current and forecast are the same and maximum is greater" in {
      createStatePension(currentAmount = 10, forecastAmount = 10, maximumAmount = 20).forecastScenario shouldBe Scenario.FillGaps
    }
    "be FillGaps when current and forecast are different and maximum is greater" in {
      createStatePension(currentAmount = 10, forecastAmount = 20, maximumAmount = 30).forecastScenario shouldBe Scenario.FillGaps
      createStatePension(currentAmount = 20, forecastAmount = 10, maximumAmount = 30).forecastScenario shouldBe Scenario.FillGaps
    }

    "be ContinueWorkingMax when forecast and maximum are the same and the value is the full amount" in {
      createStatePension(currentAmount = 100, forecastAmount = 155.65, maximumAmount = 155.65, fullStatePensionAmount = 155.65).forecastScenario shouldBe Scenario.ContinueWorkingMax
      createStatePension(currentAmount = 10, forecastAmount = 20, maximumAmount = 20, fullStatePensionAmount = 20).forecastScenario shouldBe Scenario.ContinueWorkingMax
    }
    "be ContinueWorkingMax when forecast and maximum are the same and the value is more than full amount" in {
      createStatePension(currentAmount = 100, forecastAmount = 170.00, maximumAmount = 170.00, fullStatePensionAmount = 155.65).forecastScenario shouldBe Scenario.ContinueWorkingMax
      createStatePension(currentAmount = 100, forecastAmount = 155.66, maximumAmount = 155.66, fullStatePensionAmount = 155.65).forecastScenario shouldBe Scenario.ContinueWorkingMax
      createStatePension(currentAmount = 10, forecastAmount = 20.01, maximumAmount = 20.01, fullStatePensionAmount = 20).forecastScenario shouldBe Scenario.ContinueWorkingMax
    }
    "be ContinueWorkingNonMax when forecast and maximum are the same and the value is less than full amount" in {
      createStatePension(currentAmount = 100, forecastAmount = 155.64, maximumAmount = 155.64, fullStatePensionAmount = 155.65).forecastScenario shouldBe Scenario.ContinueWorkingNonMax
      createStatePension(currentAmount = 10, forecastAmount = 20.01, maximumAmount = 20.01, fullStatePensionAmount = 21).forecastScenario shouldBe Scenario.ContinueWorkingNonMax
    }
    "be CantGetPension when maximum is 0" in {
      createStatePension(maximumAmount = 0).forecastScenario shouldBe Scenario.CantGetPension
    }
  }
}
