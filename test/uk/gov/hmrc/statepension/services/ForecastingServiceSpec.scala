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
import uk.gov.hmrc.statepension.StatePensionBaseSpec
import uk.gov.hmrc.statepension.builders.RateServiceBuilder
import uk.gov.hmrc.statepension.domain.Forecast

class ForecastingServiceSpec extends StatePensionBaseSpec {

  val testForecastingService = new ForecastingService(rateService = RateServiceBuilder.default)

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

    "there is revalution rates" should {
      "not take them into account" in {
        val service = new ForecastingService(rateService = RateServiceBuilder.twentySeventeenToTwentyEighteen)

        service.calculateStartingAmount(99.99, 100.00) shouldBe 100
      }
    }
  }

  "calculateRevaluedStartingAmount" when {
    "there is no rates" when {
      "Amount A is higher than Amount B" should {
        "return Amount A" in {
          testForecastingService.calculateRevaluedStartingAmount(100.00, 99.99) shouldBe 100
        }
      }

      "Amount B is higher than Amount A" should {
        "return Amount B" in {
          testForecastingService.calculateRevaluedStartingAmount(99.99, 100.00) shouldBe 100
        }
      }

      "Amount B are equal Amount A" should {
        "return the value" in {
          testForecastingService.calculateRevaluedStartingAmount(99, 99) shouldBe 99
        }
      }
    }

    "the starting amount revaluation rate is 2.5056% and pp revaluation rate is 1%" should {
      val service = new ForecastingService(rateService = RateServiceBuilder.twentySeventeenToTwentyEighteen)

      "protected payment" should {
        "return 159.56 for 155.66" in {
          service.calculateRevaluedStartingAmount(155.66, 155.65) shouldBe 159.56
        }

        "return 204.34 for 200 (half-up rounding)" in {
          service.calculateRevaluedStartingAmount(200, 155.65) shouldBe 204.34
        }
      }

      "full rate" should {
        "return 159.55 for 155.65" in {
          service.calculateRevaluedStartingAmount(155.65, 155.65) shouldBe 159.55
        }
      }

      "starting amount" should {
        "return 102.51 for 100" in {
          service.calculateRevaluedStartingAmount(100, 100) shouldBe 102.51
        }
        "return 126.08 for 123 (half-up rounding)" in {
          service.calculateRevaluedStartingAmount(123, 100) shouldBe 126.08
        }
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
        val service = new ForecastingService(rateService = RateServiceBuilder.twentySeventeenToTwentyEighteen)

        service.calculateForecastAmount(new LocalDate(2017, 4, 5), 2020, 130, 28) shouldBe Forecast(148.23, 4)
      }
    }

    "the rates have changed 2018-19" should {
      "use the rates from the configuration" in {
        val service = new ForecastingService(rateService = RateServiceBuilder.twentyEighteenToTwentyNineteen)

        service.calculateForecastAmount(new LocalDate(2018, 4, 5), 2021, 130, 28) shouldBe Forecast(148.78, 4)
      }
    }

    "the rates have changed 2019-20" should {
      "use the rates from the configuration" in {
        val service = new ForecastingService(rateService = RateServiceBuilder.twentyNineteenToTwentyTwenty)

        service.calculateForecastAmount(new LocalDate(2019, 4, 5), 2022, 130, 28) shouldBe Forecast(149.27, 4)
      }
    }
  }

  "calculatePersonalMaximum" when {
    def maximumCalculation(earningsIncludedUpTo: LocalDate = new LocalDate(2016, 4, 5),
                           finalRelevantStartYear: Int = 2020,
                           qualifyingYearsPre2016: Int = 30,
                           qualifyingYearsPost2016: Int = 0,
                           payableGapsPre2016: Int = 0,
                           payableGapsPost2016: Int = 0,
                           additionalPension: BigDecimal = 0,
                           rebateDerivedAmount: BigDecimal = 0,
                           forecastingService: ForecastingService = testForecastingService
                          ) = forecastingService.calculatePersonalMaximum(
      earningsIncludedUpTo,
      finalRelevantStartYear,
      qualifyingYearsPre2016,
      qualifyingYearsPost2016,
      payableGapsPre2016,
      payableGapsPost2016,
      additionalPension,
      rebateDerivedAmount
    )

    "2016-17 is not posted" when {
      "there is no gaps it" should {
        "perform the same as the forecast calculation" should {
          "There is no more years to contribute" should {
            val max = maximumCalculation(new LocalDate(2016, 4, 5), 2015, qualifyingYearsPre2016 = 30, payableGapsPre2016 = 0, additionalPension = 3.7, rebateDerivedAmount = 100)
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
            val max = maximumCalculation(new LocalDate(2016, 4, 5), 2017, qualifyingYearsPre2016 = 30, payableGapsPre2016 = 0, additionalPension = 3.7, rebateDerivedAmount = 100)
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
            val max = maximumCalculation(new LocalDate(2016, 4, 5), 2050, qualifyingYearsPre2016 = 30, payableGapsPre2016 = 0, additionalPension = 3.7, rebateDerivedAmount = 100)
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
          val max = maximumCalculation(new LocalDate(2016, 4, 5), 2015, qualifyingYearsPre2016 = 29, payableGapsPre2016 = 1, rebateDerivedAmount = 100)
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
          val max = maximumCalculation(new LocalDate(2016, 4, 5), 2015, qualifyingYearsPre2016 = 30, payableGapsPre2016 = 1, rebateDerivedAmount = 100)
          "return the current amount" in {
            max.amount shouldBe 119.30
          }
          "return zero years to work as there is none" in {
            max.yearsToWork shouldBe 0
          }
          "return 1 gap to fill as none are required but it's the number of payable gaps (tactical behaviour)" in {
            max.gapsToFill shouldBe 1
          }
        }
        "there is 28 qualifying years" should {
          val max = maximumCalculation(new LocalDate(2016, 4, 5), 2015, qualifyingYearsPre2016 = 28, payableGapsPre2016 = 1, rebateDerivedAmount = 100)
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
          val max = maximumCalculation(new LocalDate(2016, 4, 5), 2016, qualifyingYearsPre2016 = 29, payableGapsPre2016 = 1, rebateDerivedAmount = 100)
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
          val max = maximumCalculation(new LocalDate(2016, 4, 5), 2016, qualifyingYearsPre2016 = 30, payableGapsPre2016 = 1, rebateDerivedAmount = 100)
          "return the current amount + 1 one year of state pension (155.65/35)" in {
            max.amount shouldBe 123.75
          }
          "return one year to work" in {
            max.yearsToWork shouldBe 1
          }
          "return 1 gaps to fill as they will not matter but it's the number of payable gaps (tactical behaviour)" in {
            max.gapsToFill shouldBe 1
          }
        }
        "there is 28 qualifying years" should {
          val max = maximumCalculation(new LocalDate(2016, 4, 5), 2016, qualifyingYearsPre2016 = 28, payableGapsPre2016 = 1, rebateDerivedAmount = 100)
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
          val max = maximumCalculation(new LocalDate(2016, 4, 5), 2016, qualifyingYearsPre2016 = 30, payableGapsPre2016 = 1)
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
          val max = maximumCalculation(new LocalDate(2016, 4, 5), 2020, 23, payableGapsPre2016 = 10, additionalPension = 42)
          "return a maximum of 219.30" in {
            max.amount shouldBe 161.3
          }
          "return 0 years to work" in {
            max.yearsToWork shouldBe 0
          }
          "return 10 gaps to fill should be 7 but it's the number of payable gaps (tactical behaviour)" in {
            max.gapsToFill shouldBe 10
          }
        }
        "there is 23 QYs, 6 gaps, 5 years to contribute, 40 AP (working is more cost effective than paying but it's the tactical solution so paying is favoured over working)" should {
          val max = maximumCalculation(new LocalDate(2016, 4, 5), 2020, 23, payableGapsPre2016 = 6, additionalPension = 40)
          "return a maximum of 219.30" in {
            max.amount shouldBe 155.65
          }
          "return 5 years to work but tactical behaviour so return 1" in {
            max.yearsToWork shouldBe 1
          }
          "return 1 gaps to fill but tactical behaviour so fill 6 (total payable gaps)" in {
            max.gapsToFill shouldBe 6
          }
        }
      }

      "there is only 34 years and and 1 fillable gaps" should {
        val max = maximumCalculation(new LocalDate(2016, 4, 5), 2020, qualifyingYearsPre2016 = 34, payableGapsPre2016 = 1)
        "return the full rate of 155.65" in {
          max.amount shouldBe 155.65
        }
        "return 1 year to work but tactical behaviour so 0" in {
          max.yearsToWork shouldBe 0
        }
        "return no gaps to fill but tactical so 1" in {
          max.gapsToFill shouldBe 1
        }
      }
    }

    "2016-17 and more are posted (with the 2017-18 rates)" when {
      val service = new ForecastingService(rateService = RateServiceBuilder.twentySeventeenToTwentyEighteen)

      "there are no gaps" should {
        "add post 16 years onto the starting amount" should {
          val max = maximumCalculation(new LocalDate(2018, 4, 5), finalRelevantStartYear = 2017, qualifyingYearsPre2016 = 30, qualifyingYearsPost2016 = 2, forecastingService = service)
          "return 145.87 for the current/forecast amount" in {
            max.amount shouldBe 145.87
          }
          "return 0 years to work" in {
            max.yearsToWork shouldBe 0
          }
          "return 0 gaps to fill" in {
            max.gapsToFill shouldBe 0
          }
        }
        "forecast regularly from the earningsIncludedUpTo date (2016 + 2/35ths + 2/35ths rather than 2016 + 4/35ths)" should {
          val max = maximumCalculation(new LocalDate(2018, 4, 5), finalRelevantStartYear = 2019, qualifyingYearsPre2016 = 30, qualifyingYearsPost2016 = 2, forecastingService = service)
          "return 154.99 for the forecast amount" in {
            max.amount shouldBe 154.99
          }
          "return 2 years to work" in {
            max.yearsToWork shouldBe 2
          }
          "return 0 gaps to fill" in {
            max.gapsToFill shouldBe 0
          }
        }

        "still include the RDA when calculating the starting amount" should {
          val max = maximumCalculation(new LocalDate(2018, 4, 5), finalRelevantStartYear = 2019, qualifyingYearsPre2016 = 30, qualifyingYearsPost2016 = 2, rebateDerivedAmount = 12, forecastingService = service)
          "return 142.69 for the forecast amount" in {
            max.amount shouldBe 142.69
          }
          "return 2 years to work" in {
            max.yearsToWork shouldBe 2
          }
          "return 0 gaps to fill" in {
            max.gapsToFill shouldBe 0
          }
        }

        "the forecast still caps at 159.55" should {
          val max = maximumCalculation(new LocalDate(2018, 4, 5), finalRelevantStartYear = 2025, qualifyingYearsPre2016 = 30, qualifyingYearsPost2016 = 2, forecastingService = service)
          "return 159.55 for the forecast amount" in {
            max.amount shouldBe 159.55
          }
          "return 3 years to work" in {
            max.yearsToWork shouldBe 3
          }
          "return 0 gaps to fill" in {
            max.gapsToFill shouldBe 0
          }
        }

        "protected payments are only uprated and post 16 years don't matter" should {
          val max = maximumCalculation(new LocalDate(2018, 4, 5), finalRelevantStartYear = 2019, qualifyingYearsPre2016 = 30, qualifyingYearsPost2016 = 2, additionalPension = 50, forecastingService = service)
          "return 173.34 for the forecast amount" in {
            max.amount shouldBe 173.34
          }
          "return 0 years to work" in {
            max.yearsToWork shouldBe 0
          }
          "return 0 gaps to fill" in {
            max.gapsToFill shouldBe 0
          }
        }

        "when adding post 16 years (for the current amount) they should be capped at 159.55" should {
          val max = maximumCalculation(new LocalDate(2018, 4, 5), finalRelevantStartYear = 2019, qualifyingYearsPre2016 = 34, qualifyingYearsPost2016 = 2, forecastingService = service)
          "return 159.55 for the forecast amount" in {
            max.amount shouldBe 159.55
          }
          "return 0 years to work" in {
            max.yearsToWork shouldBe 0
          }
          "return 0 gaps to fill" in {
            max.gapsToFill shouldBe 0
          }
        }
      }
      "there are only pre16 gaps" should {
        "be able to still affect amount A" should {
          val max = maximumCalculation(new LocalDate(2017, 4, 5), finalRelevantStartYear = 2016, qualifyingYearsPre2016 = 25, qualifyingYearsPost2016 = 1, additionalPension = 30, payableGapsPre2016 = 5, rebateDerivedAmount = 100, forecastingService = service)
          "return 157.60 for the current/forecast amount" in {
            max.amount shouldBe 157.6
          }
          "return 0 years to work" in {
            max.yearsToWork shouldBe 0
          }
          "return 5 gaps to fill" in {
            max.gapsToFill shouldBe 5
          }
        }
        "be able to still affect amount B" should {
          val max = maximumCalculation(new LocalDate(2017, 4, 5), finalRelevantStartYear = 2016, qualifyingYearsPre2016 = 29, qualifyingYearsPost2016 = 1, payableGapsPre2016 = 5, forecastingService = service)
          "return 159.55 for the current/forecast amount" in {
            max.amount shouldBe 159.55
          }
          "return 0 years to work" in {
            max.yearsToWork shouldBe 0
          }
          "return 5 gaps to fill" in {
            max.gapsToFill shouldBe 5
          }
        }
        "be able to push people into protected payments" should {
          val max = maximumCalculation(new LocalDate(2017, 4, 5), finalRelevantStartYear = 2016, qualifyingYearsPre2016 = 25, qualifyingYearsPost2016 = 1, additionalPension = 50, payableGapsPre2016 = 5, rebateDerivedAmount = 100, forecastingService = service)
          "return 173.34 for the current/forecast amount" in {
            max.amount shouldBe 173.34
          }
          "return 0 years to work" in {
            max.yearsToWork shouldBe 0
          }
          "return 5 gaps to fill" in {
            max.gapsToFill shouldBe 5
          }
        }
        "people with 35 QYs and RDA cannot improve their amount" should {
          val max = maximumCalculation(new LocalDate(2016, 4, 5), finalRelevantStartYear = 2015, qualifyingYearsPre2016 = 35, qualifyingYearsPost2016 = 0, additionalPension = 0, payableGapsPre2016 = 5, rebateDerivedAmount = 20, forecastingService = service)
          "return 139.05 for the current/forecast amount" in {
            max.amount shouldBe 139.05
          }
          "return 0 years to work" in {
            max.yearsToWork shouldBe 0
          }
          "return 0 gaps to fill but tactical solution so return 5 payable gaps" in {
            max.gapsToFill shouldBe 5
          }
        }
      }
      "there are only post16 gaps" should {
        "do nothing for a protected payment customers" should {
          val max = maximumCalculation(new LocalDate(2018, 4, 5), finalRelevantStartYear = 2017, qualifyingYearsPre2016 = 28, qualifyingYearsPost2016 = 0, additionalPension = 100, payableGapsPre2016 = 0, payableGapsPost2016 = 2, rebateDerivedAmount = 100, forecastingService = service)
          "return 215.81 for the current/forecast amount" in {
            max.amount shouldBe 215.81
          }
          "return 0 years to work" in {
            max.yearsToWork shouldBe 0
          }
          "return 0 gaps to fill but it's the tactical solution and total payable gaps = 2" in {
            max.gapsToFill shouldBe 2
          }
        }
        "can not push people into protected payments (capped at max)" should {
          val max = maximumCalculation(new LocalDate(2018, 4, 5), finalRelevantStartYear = 2017, qualifyingYearsPre2016 = 25, qualifyingYearsPost2016 = 0, additionalPension = 50, payableGapsPost2016 = 2, rebateDerivedAmount = 100, forecastingService = service)
          "return 159.55 for the current/forecast amount" in {
            max.amount shouldBe 159.55
          }
          "return 0 years to work" in {
            max.yearsToWork shouldBe 0
          }
          "return 2 gaps to fill" in {
            max.gapsToFill shouldBe 2
          }
        }
        "calculation takes into account post16 qualifying years and gaps when calculating current amount (should be 2/35ths)" should {
          val max = maximumCalculation(new LocalDate(2018, 4, 5), finalRelevantStartYear = 2017, qualifyingYearsPre2016 = 24, qualifyingYearsPost2016 = 1, additionalPension = 50, payableGapsPost2016 = 1, rebateDerivedAmount = 100, forecastingService = service)
          "return 158.2 for the current/forecast amount" in {
            max.amount shouldBe 158.20
          }
          "return 0 years to work" in {
            max.yearsToWork shouldBe 0
          }
          "return 1 gaps to fill" in {
            max.gapsToFill shouldBe 1
          }
        }

        "calculation takes into account post16 qualifying years and gaps when calculating current amount and then treats the forecast separate (should be 2/35ths + 3/35ths)" should {
          val max = maximumCalculation(new LocalDate(2018, 4, 5), finalRelevantStartYear = 2020, qualifyingYearsPre2016 = 20, qualifyingYearsPost2016 = 1, additionalPension = 50, payableGapsPost2016 = 1, rebateDerivedAmount = 100, forecastingService = service)
          "return 158.2 for the current/forecast amount" in {
            max.amount shouldBe 155.58
          }
          "return 3 years to work" in {
            max.yearsToWork shouldBe 3
          }
          "return 1 gaps to fill" in {
            max.gapsToFill shouldBe 1
          }
        }
      }

      "there is a mixture of pre and post 16 gaps" should {
        "take both into account" should {
          val max = maximumCalculation(new LocalDate(2018, 4, 5), finalRelevantStartYear = 2020, qualifyingYearsPre2016 = 20, qualifyingYearsPost2016 = 1, additionalPension = 50, payableGapsPre2016 = 1, payableGapsPost2016 = 1, rebateDerivedAmount = 100, forecastingService = service)
          "return 159.55 for the current/forecast amount" in {
            max.amount shouldBe 159.55
          }
          "return 3 years to work" in {
            max.yearsToWork shouldBe 3
          }
          "return 2 gaps to fill" in {
            max.gapsToFill shouldBe 2
          }
        }
        "people with 35 QYs and RDA can only improve their amount with post 16 gaps" should {
          val max = maximumCalculation(new LocalDate(2018, 4, 5), finalRelevantStartYear = 2017, qualifyingYearsPre2016 = 35, qualifyingYearsPost2016 = 0, additionalPension = 0, payableGapsPre2016 = 5, payableGapsPost2016 = 2, rebateDerivedAmount = 20, forecastingService = service)
          "return 148.17 for the current/forecast amount" in {
            max.amount shouldBe 148.17
          }
          "return 0 years to work" in {
            max.yearsToWork shouldBe 0
          }
          "return 2 gaps to fill but tactical solution so return 7 payable gaps" in {
            max.gapsToFill shouldBe 7
          }
        }
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
        val service = new ForecastingService(rateService = RateServiceBuilder.apply(Map(0 -> 0, 1 -> 100, 2 -> 200)))

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
