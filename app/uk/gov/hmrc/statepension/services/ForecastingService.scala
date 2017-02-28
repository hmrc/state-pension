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
import uk.gov.hmrc.statepension.domain.Forecast
import uk.gov.hmrc.time.TaxYearResolver

import scala.math.BigDecimal.RoundingMode

object ForecastingService {

  final val MINIMUM_QUALIFYING_YEARS = 10

  def calculateStartingAmount(amountA2016: BigDecimal, amountB2016: BigDecimal): BigDecimal = {
    amountA2016.max(amountB2016)
  }

  def calculateForecastAmount(earningsIncludedUpTo: LocalDate, finalRelevantStartYear: Int, currentAmount: BigDecimal, qualifyingYears: Int): Forecast = {
    require(earningsIncludedUpTo.getYear >= 2016, "2015-16 tax year has not been posted")

    val yearsLeft = yearsLeftToContribute(earningsIncludedUpTo, finalRelevantStartYear)

    if(currentAmount >= RateService.MAX_AMOUNT) {
      Forecast(currentAmount, yearsToWork = 0)
    }
    else if ((yearsLeft + qualifyingYears) < MINIMUM_QUALIFYING_YEARS) {
      Forecast(amount = 0, yearsToWork = 0)
    }
    else {
      val forecastAmount = (currentAmount + RateService.spAmountPerYear * yearsLeft).setScale(2, RoundingMode.HALF_UP).min(RateService.MAX_AMOUNT)
      val difference = forecastAmount - currentAmount
      val yearsNeeded: Int = (difference / RateService.spAmountPerYear).setScale(0, RoundingMode.CEILING).toInt
      Forecast(forecastAmount, yearsNeeded.min(yearsLeft))
    }
  }

  def calculatePersonalMaximum(earningsIncludedUpTo: LocalDate, finalRelevantStartYear: Int,
                               qualifyingYears: Int, payableGaps: Int,
                               additionalPension: BigDecimal, rebateDerivedAmount: BigDecimal): BigDecimal = {
    val potentialYears = qualifyingYears + payableGaps
    val startingAmount = calculateStartingAmount(amountA(potentialYears, additionalPension), amountB(potentialYears, rebateDerivedAmount))
    calculateForecastAmount(earningsIncludedUpTo, finalRelevantStartYear, currentAmount = startingAmount, qualifyingYears).amount
  }

  def yearsLeftToContribute(earningsIncludedUpTo: LocalDate, finalRelevantStartYear: Int): Int  = {
    (finalRelevantStartYear - TaxYearResolver.taxYearFor(earningsIncludedUpTo)).max(0)
  }

  def amountA(qualifyingYearsPre2016: Int, additionalPension: BigDecimal): BigDecimal = {
    RateService.getBasicSPAmount(qualifyingYearsPre2016) + additionalPension
  }

  def amountB(qualfyingYears: Int, rebateDerivedAmount: BigDecimal): BigDecimal = {
    RateService.getSPAmount(qualfyingYears) - rebateDerivedAmount
  }

}
