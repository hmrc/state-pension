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

package uk.gov.hmrc.statepension

import org.joda.time.LocalDate
import uk.gov.hmrc.statepension.domain.Forecast
import uk.gov.hmrc.statepension.services.ForecastingService

class ForecastingServiceSpec extends StatePensionUnitSpec {

  "calculateStartingAmount" when {
    "Amount A is higher than Amount B" should {
      "return Amount A" in {
        ForecastingService.calculateStartingAmount(100.00, 99.99) shouldBe 100
      }
    }

    "Amount B is higher than Amount A" should {
      "return Amount B" in {
        ForecastingService.calculateStartingAmount(99.99, 100.00) shouldBe 100
      }
    }

    "Amount B are equal Amount A" should {
      "return the value" in {
        ForecastingService.calculateStartingAmount(99, 99) shouldBe 99
      }
    }

  }

  "calculateForecastAmount" when {

    def forecastCalculation(earningsIncludedUpTo: LocalDate = new LocalDate(2016, 4, 5),
                            finalRelevantStartYear: Int = 2020,
                            currentAmount: BigDecimal = 125,
                            qualifyingYears: Int = 30) = ForecastingService.calculateForecastAmount(
      earningsIncludedUpTo,
      finalRelevantStartYear,
      currentAmount,
      qualifyingYears
    )

    "The earningsIncludedUpTo end tax year is less than 2016" should {
      "throw a RunTime Exception" in {
        val exception = intercept[IllegalArgumentException] {
          forecastCalculation(earningsIncludedUpTo = new LocalDate(2015, 4, 5))
        }
        exception.getMessage shouldBe "requirement failed: 2015-16 tax year has not been posted"
      }
    }

    "The currentAmount is already the maximum" should {
      "return the maximum" in {
        forecastCalculation(currentAmount = 155.65).amount shouldBe 155.65
      }

      "return no years to work" in {
        forecastCalculation(currentAmount = 155.65).yearsToWork shouldBe 0
      }

    }

    "The currentAmount is higher than the maximum" should {
      "return the value which is higher than the maximum" in {
        forecastCalculation(currentAmount = 155.66).amount shouldBe 155.66
      }

      "return no years to work" in {
        forecastCalculation(currentAmount = 155.66).yearsToWork shouldBe 0
      }
    }

    "The user cannot achieve more than 10 qualifying years" should {
      "return (0,0) for QYs = 1, Future Years = 2" in {
        forecastCalculation(qualifyingYears = 1, earningsIncludedUpTo = new LocalDate(2016, 4, 5), finalRelevantStartYear = 2017) shouldBe Forecast(0, 0)
      }
      "return (0,0) for QYs = 1, Future Years = 8" in {
        forecastCalculation(qualifyingYears = 1, earningsIncludedUpTo = new LocalDate(2016, 4, 5), finalRelevantStartYear = 2023) shouldBe Forecast(0, 0)
      }
      "return a forecast and years to work for QYs = 1, Future Years = 9" in {
        val forecast = forecastCalculation(qualifyingYears = 1, earningsIncludedUpTo = new LocalDate(2016, 4, 5), finalRelevantStartYear = 2024)
        forecast.amount > 0 shouldBe true
        forecast.yearsToWork > 0 shouldBe true
      }
    }

    "There is no more years to contribute" should {
      "return an un-modified current amount" in {
        forecastCalculation(new LocalDate(2016, 4, 5), 2015, currentAmount = 123, 30).amount shouldBe 123
      }
      "return no years to work" in {
        forecastCalculation(new LocalDate(2016, 4, 5), 2015, currentAmount = 123, 30).yearsToWork shouldBe 0
      }
    }

    "There is one year to contribute" should {
      "return a forecast with a difference of (max / amount) rounded (half-up)" in {
        forecastCalculation(new LocalDate(2016, 4, 5), 2016, currentAmount = 123, 30).amount shouldBe 127.45
      }
      "return one year to work" in {
        forecastCalculation(new LocalDate(2016, 4, 5), 2016, currentAmount = 123, 30).yearsToWork shouldBe 1
      }
    }

    "There is two years to contribute" should {
      "return a forecast with a difference of (max / amount) * 2 rounded (half-up)" in {
        forecastCalculation(new LocalDate(2016, 4, 5), 2017, currentAmount = 123, 30).amount shouldBe 131.89
      }
      "return two years to work" in {
        forecastCalculation(new LocalDate(2016, 4, 5), 2017, currentAmount = 123, 30).yearsToWork shouldBe 2
      }
    }

    "There is more years to contribute than required" should  {
      "cap the amount at the maximum" in {
        forecastCalculation(new LocalDate(2016, 4, 5), 2050, currentAmount = 123, 30).amount shouldBe 155.65
      }
      "years to work should be the number of years required and no more 155.65 - 123 / (155.65/35) = 7.34 rounded up to int = 8" in {
        forecastCalculation(new LocalDate(2016, 4, 5), 2050, currentAmount = 123, 30).yearsToWork shouldBe 8
      }
    }
  }

  "yearsToContribute" when {
    "The earningsIncludedUpTo taxYear is the same as the FRY" should {
      "return 0" in {
        ForecastingService.yearsLeftToContribute(new LocalDate(2017, 4, 5), 2016) shouldBe 0
      }
    }

    "The earningsIncludedUpTo taxYear is the after as the FRY" should {
      "return 0" in {
        ForecastingService.yearsLeftToContribute(new LocalDate(2017, 4, 6), 2016) shouldBe 0
      }
    }

    "The earningsIncludedUpTo taxYear is the before the FRY" should {
      "return 1 for one year before" in {
        ForecastingService.yearsLeftToContribute(new LocalDate(2016, 4, 5), 2016) shouldBe 1
      }

      "return 2 for two year before" in {
        ForecastingService.yearsLeftToContribute(new LocalDate(2016, 4, 5), 2017) shouldBe 2
      }

      "return 10 for two year before" in {
        ForecastingService.yearsLeftToContribute(new LocalDate(2016, 4, 5), 2025) shouldBe 10
      }
    }
  }

}
