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

import com.google.inject.Inject
import org.joda.time.LocalDate
import services.TaxYearResolver
import uk.gov.hmrc.statepension.domain.{Forecast, PersonalMaximum}
import uk.gov.hmrc.statepension.domain.PolicyDecisions.MINIMUM_QUALIFYING_YEARS

import scala.math.BigDecimal.RoundingMode

class ForecastingService @Inject()(val rateService: RateService) {

  def calculateStartingAmount(amountA2016: BigDecimal, amountB2016: BigDecimal): BigDecimal = {
    amountA2016.max(amountB2016)
  }

  def calculateRevaluedStartingAmount(amountA2016: BigDecimal, amountB2016: BigDecimal): BigDecimal = {
    val startingAmount2016 = calculateStartingAmount(amountA2016, amountB2016)
    if (startingAmount2016 < rateService.MAX_AMOUNT_2016) (startingAmount2016 * rateService.revaluationRates.startingAmount).setScale(2, RoundingMode.HALF_UP)
    else if (startingAmount2016 == rateService.MAX_AMOUNT_2016) rateService.MAX_AMOUNT
    else {
      val protectedPayment2016 = startingAmount2016 - rateService.MAX_AMOUNT_2016
      (protectedPayment2016 * rateService.revaluationRates.protectedPayment).setScale(2, RoundingMode.HALF_UP) + rateService.MAX_AMOUNT
    }
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
                               qualifyingYearsPre2016: Int, qualifyingYearsPost2016: Int, payableGapsPre2016: Int, payableGapsPost2016: Int,
                               additionalPension: BigDecimal, rebateDerivedAmount: BigDecimal): PersonalMaximum = {
    val personalMaxCalculation = personalMaxCalc(
      earningsIncludedUpTo, finalRelevantStartYear, qualifyingYearsPre2016, qualifyingYearsPost2016, additionalPension, rebateDerivedAmount
    )
    val totalMaximum = personalMaxCalculation(payableGapsPre2016, payableGapsPost2016)
    PersonalMaximum(totalMaximum.amount, totalMaximum.yearsToWork, gapsToFill = payableGapsPre2016 + payableGapsPost2016)
  }

  private def personalMaxCalc(earningsIncludedUpTo: LocalDate, finalRelevantStartYear: Int, qualifyingYearsPre2016: Int, qualifyingYearsPost2016: Int,
                              additionalPension: BigDecimal, rebateDerivedAmount: BigDecimal) = (gapsToFillPre2016: Int, gapsToFillPost2016: Int) => {
    val startingAmount = calculateRevaluedStartingAmount(
      amountA(qualifyingYearsPre2016 + gapsToFillPre2016, additionalPension),
      amountB(qualifyingYearsPre2016 + gapsToFillPre2016, rebateDerivedAmount)
    )
    val currentAmount =
      if (startingAmount >= rateService.MAX_AMOUNT) startingAmount
      else (startingAmount + rateService.getSPAmount(qualifyingYearsPost2016 + gapsToFillPost2016)).min(rateService.MAX_AMOUNT)
    calculateForecastAmount(earningsIncludedUpTo, finalRelevantStartYear, currentAmount = currentAmount, qualifyingYearsPre2016 + qualifyingYearsPost2016 + gapsToFillPre2016 + gapsToFillPost2016)
  }

  def yearsLeftToContribute(earningsIncludedUpTo: LocalDate, finalRelevantStartYear: Int): Int = {
    (finalRelevantStartYear - TaxYearResolver.taxYearFor(earningsIncludedUpTo)).max(0)
  }

  def amountA(qualifyingYearsPre2016: Int, additionalPension: BigDecimal): BigDecimal = {
    rateService.getBasicSPAmount(qualifyingYearsPre2016) + additionalPension
  }

  def amountB(qualfyingYears: Int, rebateDerivedAmount: BigDecimal): BigDecimal = {
    rateService.getSPAmount2016(qualfyingYears) - rebateDerivedAmount
  }

  def sanitiseCurrentAmount(current: BigDecimal, qualifyingYears: Int): BigDecimal = if (qualifyingYears < MINIMUM_QUALIFYING_YEARS) 0 else current

}
