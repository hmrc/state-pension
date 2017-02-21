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
import uk.gov.hmrc.time.TaxYearResolver

import scala.math.BigDecimal.RoundingMode

object ForecastingService {

  final val MINIMUM_QUALIFYING_YEARS = 10

  def calculateStartingAmount(amountA2016: BigDecimal, amountB2016: BigDecimal): BigDecimal = {
    amountA2016.max(amountB2016)
  }

  def calculateForecastAmount(earningsIncludedUpTo: LocalDate, finalRelevantStartYear: Int, currentAmount: BigDecimal, qualifyingYears: Int): BigDecimal = {
    require(earningsIncludedUpTo.getYear >= 2016, "2015-16 tax year has not been posted")

    val yearsLeft = yearsLeftToContribute(earningsIncludedUpTo, finalRelevantStartYear)

    if(currentAmount >= RateService.MAX_AMOUNT) currentAmount
    else if ((yearsLeft + qualifyingYears) < MINIMUM_QUALIFYING_YEARS) 0
    else (currentAmount + RateService.spAmountPerYear * yearsLeft).setScale(2, RoundingMode.HALF_UP).min(RateService.MAX_AMOUNT)
  }

  def yearsLeftToContribute(earningsIncludedUpTo: LocalDate, finalRelevantStartYear: Int): Int  = {
    (finalRelevantStartYear - TaxYearResolver.taxYearFor(earningsIncludedUpTo)).max(0)
  }

}
