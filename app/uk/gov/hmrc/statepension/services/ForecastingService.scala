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
import uk.gov.hmrc.statepension.domain.{Forecast, PersonalMaximum}
import uk.gov.hmrc.time.TaxYearResolver

import scala.annotation.tailrec
import scala.math.BigDecimal.RoundingMode

trait ForecastingService {

  def rateService: RateService

  final val MINIMUM_QUALIFYING_YEARS = 10

  def calculateStartingAmount(amountA2016: BigDecimal, amountB2016: BigDecimal): BigDecimal = {
    amountA2016.max(amountB2016)
    //TODO Revaluation
  }

  def calculateForecastAmount(earningsIncludedUpTo: LocalDate, finalRelevantStartYear: Int, currentAmount: BigDecimal, qualifyingYears: Int): Forecast = {
    require(earningsIncludedUpTo.getYear >= 2016, "2015-16 tax year has not been posted")

    val yearsLeft = yearsLeftToContribute(earningsIncludedUpTo, finalRelevantStartYear)

    if (currentAmount >= rateService.MAX_AMOUNT) {
      Forecast(currentAmount, yearsToWork = 0)
    }
    else if ((yearsLeft + qualifyingYears) < MINIMUM_QUALIFYING_YEARS) {
      Forecast(amount = 0, yearsToWork = 0)
    }
    else {
      val forecastAmount = (currentAmount + rateService.getSPAmount(yearsLeft)).setScale(2, RoundingMode.HALF_UP).min(rateService.MAX_AMOUNT)
      val difference = forecastAmount - currentAmount
      val yearsNeeded: Int = rateService.yearsNeededForAmount(difference)
      Forecast(forecastAmount, yearsNeeded.min(yearsLeft))
    }
  }

  def calculatePersonalMaximum(earningsIncludedUpTo: LocalDate, finalRelevantStartYear: Int,
                               qualifyingYears: Int, payableGaps: Int,
                               additionalPension: BigDecimal, rebateDerivedAmount: BigDecimal): PersonalMaximum = {
    val personalMaxCalculation = personalMaxCalc(
      earningsIncludedUpTo, finalRelevantStartYear, qualifyingYears, payableGaps, additionalPension, rebateDerivedAmount
    )
    val totalMaximum = personalMaxCalculation(payableGaps)
    if (payableGaps > 0) {
      personalMaxGenerator(totalMaximum.amount, payableGaps, personalMaxCalculation)
    }
    else {
      PersonalMaximum(totalMaximum.amount, totalMaximum.yearsToWork, gapsToFill = 0)
    }
  }

  private def personalMaxCalc(earningsIncludedUpTo: LocalDate, finalRelevantStartYear: Int, qualifyingYears: Int, payableGaps: Int,
                              additionalPension: BigDecimal, rebateDerivedAmount: BigDecimal) = (gapsToFill: Int) => {
    val startingAmount = calculateStartingAmount(
      amountA(qualifyingYears + gapsToFill, additionalPension),
      amountB(qualifyingYears + gapsToFill, rebateDerivedAmount)
    )
    calculateForecastAmount(earningsIncludedUpTo, finalRelevantStartYear, currentAmount = startingAmount, qualifyingYears + gapsToFill)
  }

  private def personalMaxGenerator(maximum: BigDecimal, payableGaps: Int, calculation: (Int) => (Forecast)): PersonalMaximum = {
    require(payableGaps > 0)

    @tailrec def go(years: Int): Int = {
      if (years < 0) 0
      else if (calculation(years).amount < maximum) years + 1
      else go(years - 1)
    }

    val minimumGaps = go(payableGaps)
    val minYearsToContributeForecast = calculation(minimumGaps)
    PersonalMaximum(maximum, minYearsToContributeForecast.yearsToWork, minimumGaps)
  }

  def yearsLeftToContribute(earningsIncludedUpTo: LocalDate, finalRelevantStartYear: Int): Int = {
    (finalRelevantStartYear - TaxYearResolver.taxYearFor(earningsIncludedUpTo)).max(0)
  }

  def amountA(qualifyingYearsPre2016: Int, additionalPension: BigDecimal): BigDecimal = {
    rateService.getBasicSPAmount(qualifyingYearsPre2016) + additionalPension
  }

  def amountB(qualfyingYears: Int, rebateDerivedAmount: BigDecimal): BigDecimal = {
    rateService.getSPAmount(qualfyingYears) - rebateDerivedAmount
  }

  def sanitiseCurrentAmount(current: BigDecimal, qualifyingYears: Int): BigDecimal = if (qualifyingYears < MINIMUM_QUALIFYING_YEARS) 0 else current

}

object ForecastingService extends ForecastingService {
  override lazy val rateService: RateService = RateService
}
