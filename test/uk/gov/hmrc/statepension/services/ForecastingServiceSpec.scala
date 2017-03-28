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

package uk.gov.hmrc.statepension.services

import org.joda.time.LocalDate
import play.api.Configuration
import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.builders.RateServiceBuilder
import uk.gov.hmrc.statepension.domain.Forecast

class ForecastingServiceSpec extends StatePensionUnitSpec {

  val testForecastingService = new ForecastingService {
    override lazy val rateService: RateService = RateServiceBuilder.default
  }

  "calculateStartingAmount" when {
    "Amount A is higher than Amount B" should {
      "return Amount A" in {
        testForecastingService.calculateStartingAmount(100.00, 99.99) shouldBe 100
      }
    }

    "Amount B is higher than Amount A" should {
      "return Amount B" in {
        testForecastingService.calculateStartingAmount(99.99, 100.00) shouldBe 100
      }
    }

    "Amount B are equal Amount A" should {
      "return the value" in {
        testForecastingService.calculateStartingAmount(99, 99) shouldBe 99
      }
    }

  }

  "calculateForecastAmount" when {

    def forecastCalculation(earningsIncludedUpTo: LocalDate = new LocalDate(2016, 4, 5),
                            finalRelevantStartYear: Int = 2020,
                            currentAmount: BigDecimal = 125,
                            qualifyingYears: Int = 30) = testForecastingService.calculateForecastAmount(
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

    "There is more years to contribute than required" should {
      "cap the amount at the maximum" in {
        forecastCalculation(new LocalDate(2016, 4, 5), 2050, currentAmount = 123, 30).amount shouldBe 155.65
      }
      "years to work should be the number of years required and no more 155.65 - 123 / (155.65/35) = 7.34 rounded up to int = 8" in {
        forecastCalculation(new LocalDate(2016, 4, 5), 2050, currentAmount = 123, 30).yearsToWork shouldBe 8
      }
    }

    "the rates have changed 2017-18" should {
      "use the rates from the configuration" in {
        val service = new ForecastingService {
          override def rateService: RateService = RateServiceBuilder.twentySeventeenToTwentyEighteen
        }
        service.calculateForecastAmount(new LocalDate(2017, 4, 5), 2020, 130, 28) shouldBe Forecast(148.23, 4)
      }
    }

  }

  "calculatePersonalMaximum" when {
    def maximumCalculation(earningsIncludedUpTo: LocalDate = new LocalDate(2016, 4, 5),
                           finalRelevantStartYear: Int = 2020,
                           qualifyingYears: Int = 30,
                           payableGaps: Int = 0,
                           additionalPension: BigDecimal = 0,
                           rebateDerivedAmount: BigDecimal = 0
                          ) = testForecastingService.calculatePersonalMaximum(
      earningsIncludedUpTo,
      finalRelevantStartYear,
      qualifyingYears,
      payableGaps,
      additionalPension,
      rebateDerivedAmount
    )

    "when there is no gaps it" should {
      "perform the same as the forecast calculation" should {
        "There is no more years to contribute" should {
          val max = maximumCalculation(new LocalDate(2016, 4, 5), 2015, qualifyingYears = 30, payableGaps = 0, additionalPension = 3.7, rebateDerivedAmount = 100)
          "return the current amount" in {
            max.amount shouldBe 123
          }
          "return 0 years to work" in {
            max.yearsToWork shouldBe 0
          }

          "return 0 gaps to fill" in {
            max.gapsToFill shouldBe 0
          }
        }

        "There is two years to contribute" should {
          val max = maximumCalculation(new LocalDate(2016, 4, 5), 2017, qualifyingYears = 30, payableGaps = 0, additionalPension = 3.7, rebateDerivedAmount = 100)
          "return a forecast with a difference of (max / amount) * 2 rounded (half-up)" in {
            max.amount shouldBe 131.89
          }
          "return 2 years to work" in {
            max.yearsToWork shouldBe 2
          }
          "return 0 gaps to fill" in {
            max.gapsToFill shouldBe 0
          }
        }

        "There is more years to contribute than required" should {
          val max = maximumCalculation(new LocalDate(2016, 4, 5), 2050, qualifyingYears = 30, payableGaps = 0, additionalPension = 3.7, rebateDerivedAmount = 100)
          "cap the amount at the maximum" in {
            max.amount shouldBe 155.65
          }
          "years to work should be the number of years required and no more 155.65 - 123 / (155.65/35) = 7.34 rounded up to int = 8" in {
            max.yearsToWork shouldBe 8
          }
          "return 0 gaps to fill" in {
            max.gapsToFill shouldBe 0
          }
        }
      }
    }

    "there is one payable gap and zero years to contribute" when {
      "there is less than 30 qualifying years" should {
        val max = maximumCalculation(new LocalDate(2016, 4, 5), 2015, 29, payableGaps = 1, rebateDerivedAmount = 100)
        "return amount + 1 year of basic pension (119.3 /30)" in {
          max.amount shouldBe 119.30
        }
        "return zero years to work as there is none" in {
          max.yearsToWork shouldBe 0
        }
        "return one gap to fill" in {
          max.gapsToFill shouldBe 1
        }
      }
      "there is 30 qualifying years" should {
        val max = maximumCalculation(new LocalDate(2016, 4, 5), 2015, 30, payableGaps = 1, rebateDerivedAmount = 100)
        "return the current amount" in {
          max.amount shouldBe 119.30
        }
        "return zero years to work as there is none" in {
          max.yearsToWork shouldBe 0
        }
        "return zero gap to fill as none are required" in {
          max.gapsToFill shouldBe 0
        }
      }
      "there is 28 qualifying years" should {
        val max = maximumCalculation(new LocalDate(2016, 4, 5), 2015, 28, payableGaps = 1, rebateDerivedAmount = 100)
        "return the amount + 1 year of basic pension (119.3 /30) " in {
          max.amount shouldBe 115.32
        }
        "return zero years to work as there is none" in {
          max.yearsToWork shouldBe 0
        }
        "return one gap to fill" in {
          max.gapsToFill shouldBe 1
        }
      }
    }

    "there is one payable gap and one year to contribute " when {
      "there is 29 qualifying years" should {
        val max = maximumCalculation(new LocalDate(2016, 4, 5), 2016, 29, payableGaps = 1, rebateDerivedAmount = 100)
        "return amount + 1 year of basic pension (119.3 /30) + 1 one year of state pension (155.65/35)" in {
          max.amount shouldBe 123.75
        }
        "return one year to work" in {
          max.yearsToWork shouldBe 1
        }
        "return one gap to fill" in {
          max.gapsToFill shouldBe 1
        }
      }
      "there is 30 qualifying years" should {
        val max = maximumCalculation(new LocalDate(2016, 4, 5), 2016, 30, payableGaps = 1, rebateDerivedAmount = 100)
        "return the current amount + 1 one year of state pension (155.65/35)" in {
          max.amount shouldBe 123.75
        }
        "return one year to work" in {
          max.yearsToWork shouldBe 1
        }
        "return zero gaps to fill as they will not matter" in {
          max.gapsToFill shouldBe 0
        }
      }
      "there is 28 qualifying years" should {
        val max = maximumCalculation(new LocalDate(2016, 4, 5), 2016, 28, payableGaps = 1, rebateDerivedAmount = 100)
        "return the amount + 1 year of basic pension (119.3 /30) + 1 one year of state pension (155.65/35) " in {
          max.amount shouldBe 119.77
        }
        "return 1 year to work" in {
          max.yearsToWork shouldBe 1
        }
        "return one gap to fill" in {
          max.gapsToFill shouldBe 1
        }
      }

      "there is 30 qualifying years and no RDA" should {
        val max = maximumCalculation(new LocalDate(2016, 4, 5), 2016, 30, payableGaps = 1)
        "return 32 qualifying years of new state pension" in {
          max.amount shouldBe 142.31
        }
        "return 1 year to work" in {
          max.yearsToWork shouldBe 1
        }
        "return 1 gap to fill" in {
          max.gapsToFill shouldBe 1
        }
      }
    }

    "there are multiple gaps and many years to contribute " when {
      "there is 23 QYs, 10 gaps, 5 years to contribute, 42 AP (only filling gaps matters because Amount A)" should {
        val max = maximumCalculation(new LocalDate(2016, 4, 5), 2020, 23, payableGaps = 10, additionalPension = 42)
        "return a maximum of 219.30" in {
          max.amount shouldBe 161.3
        }
        "return 0 years to work" in {
          max.yearsToWork shouldBe 0
        }
        "return 7 gaps to fill" in {
          max.gapsToFill shouldBe 7
        }
      }
      "there is 23 QYs, 6 gaps, 5 years to contribute, 40 AP (working is more cost effective than paying)" should {
        val max = maximumCalculation(new LocalDate(2016, 4, 5), 2020, 23, payableGaps = 6, additionalPension = 40)
        "return a maximum of 219.30" in {
          max.amount shouldBe 155.65
        }
        "return 5 years to work" in {
          max.yearsToWork shouldBe 5
        }
        "return 1 gaps to fill" in {
          max.gapsToFill shouldBe 1
        }
      }
    }

    "there is only 34 years and and 1 fillable gaps" should {
      val max = maximumCalculation(new LocalDate(2016, 4, 5), 2020, 34, 1, 0, 0)
      "return the full rate of 155.65" in {
        max.amount shouldBe 155.65
      }
      "return 1 year to work" in {
        max.yearsToWork shouldBe 1
      }
      "return no gaps to fill" in {
        max.gapsToFill shouldBe 0
      }
    }
  }

  "yearsToContribute" when {
    "The earningsIncludedUpTo taxYear is the same as the FRY" should {
      "return 0" in {
        testForecastingService.yearsLeftToContribute(new LocalDate(2017, 4, 5), 2016) shouldBe 0
      }
    }

    "The earningsIncludedUpTo taxYear is the after as the FRY" should {
      "return 0" in {
        testForecastingService.yearsLeftToContribute(new LocalDate(2017, 4, 6), 2016) shouldBe 0
      }
    }

    "The earningsIncludedUpTo taxYear is the before the FRY" should {
      "return 1 for one year before" in {
        testForecastingService.yearsLeftToContribute(new LocalDate(2016, 4, 5), 2016) shouldBe 1
      }

      "return 2 for two year before" in {
        testForecastingService.yearsLeftToContribute(new LocalDate(2016, 4, 5), 2017) shouldBe 2
      }

      "return 10 for two year before" in {
        testForecastingService.yearsLeftToContribute(new LocalDate(2016, 4, 5), 2025) shouldBe 10
      }
    }
  }

  "amountA" when {
    "ap is 0" should {
      "return 3.98 for 1 qualifying year" in {
        testForecastingService.amountA(1, 0) shouldBe 3.98
      }
      "return 7.95 for 2 qualifying years" in {
        testForecastingService.amountA(2, 0) shouldBe 7.95
      }
      "return 39.77 for 10 qualifying years" in {
        testForecastingService.amountA(10, 0) shouldBe 39.77
      }
      "return 119.30 for 30 qualifying years" in {
        testForecastingService.amountA(30, 0) shouldBe 119.30
      }
      "return 119.30 for 31 qualifying years" in {
        testForecastingService.amountA(31, 0) shouldBe 119.30
      }
    }

    "ap has a value" should {
      "return 140 for 30 qualifyingYears and £20.70 AP" in {
        testForecastingService.amountA(30, 20.70) shouldBe 140
      }
      "return 4 for 1 qualifyingYears and £0.02 AP" in {
        testForecastingService.amountA(1, 0.02) shouldBe 4
      }
    }
  }

  "amountB" when {
    "rda is 0" should {
      "return 155.65 for 35 years" in {
        testForecastingService.amountB(35, 0) shouldBe 155.65
      }

      "return 155.65 for 36 years" in {
        testForecastingService.amountB(36, 0) shouldBe 155.65
      }
    }

    "rda has a value" should {
      "return 105.65 for QY= 35, RDA = 50" in {
        testForecastingService.amountB(35, 50) shouldBe 105.65
      }
    }

    "rates have revalued" should {
      "still return the 2016 values" in {
        val service = new ForecastingService {
          override def rateService: RateService = RateServiceBuilder.apply(Map(0 -> 0, 1 -> 100, 2 -> 200))
        }
        service.amountB(35, 0) shouldBe 155.65
      }
    }
  }

  "sanitiseCurrentAmount" when {
    "there is < 10 years" should {
      "return 0" in {
        testForecastingService.sanitiseCurrentAmount(current = 123, qualifyingYears = 9) shouldBe 0
      }
    }
    "there is 10 years" should {
      "return the current amount" in {
        testForecastingService.sanitiseCurrentAmount(current = 123, qualifyingYears = 10) shouldBe 123
      }
    }
    "there is 20 years" should {
      "return the current amount" in {
        testForecastingService.sanitiseCurrentAmount(current = 123, qualifyingYears = 20) shouldBe 123
      }
    }
  }

}
