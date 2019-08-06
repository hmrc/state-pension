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
import play.api.Configuration
import uk.gov.hmrc.statepension.config.{AppContext, RevaluationRates}

import scala.math.BigDecimal.RoundingMode

class RateService @Inject()(appContext: AppContext) {
  lazy val ratesConfig: Configuration = appContext.rates
  lazy val revaluationConfig: Option[Configuration] = appContext.revaluation

  val revaluationRates: RevaluationRates = RevaluationRates(revaluationConfig)

  private[services] lazy val ratesTable: Map[Int, BigDecimal] = {
      ratesConfig.keys.map(k => k.toInt -> ratesConfig.getString(k).fold[BigDecimal](0)(BigDecimal(_))).toMap
  }

  val MAX_YEARS: Int = ratesTable.keys.max
  val MAX_AMOUNT: BigDecimal = ratesTable(MAX_YEARS)

  def getSPAmount(totalQualifyingYears: Int): BigDecimal = {
    if (totalQualifyingYears > MAX_YEARS) {
      MAX_AMOUNT
    } else {
      ratesTable(totalQualifyingYears)
    }
  }

  def yearsNeededForAmount(amount: BigDecimal): Int = {
    if(amount < 0) 0
    else ratesTable.filter(_._2 >= amount).keys.min
  }

  final val MAX_BASIC_AMOUNT: BigDecimal = 119.30
  final val MAX_BASIC_YEARS: Int = 30

  val basicSPAmountPerYear: BigDecimal = MAX_BASIC_AMOUNT / MAX_BASIC_YEARS

  def getBasicSPAmount(qualifyingYears: Int): BigDecimal = {
    if (qualifyingYears > MAX_BASIC_YEARS) {
      MAX_BASIC_AMOUNT
    } else {
      (basicSPAmountPerYear * qualifyingYears).setScale(2, RoundingMode.HALF_UP)
    }
  }

  final val MAX_AMOUNT_2016: BigDecimal = 155.65
  final val MAX_YEARS_2016: BigDecimal = 35

  def getSPAmount2016(totalQualifyingYears: Int): BigDecimal = {
    if(totalQualifyingYears < 1) {
      0
    } else {
      ((MAX_AMOUNT_2016 / MAX_YEARS_2016) * totalQualifyingYears).setScale(2, RoundingMode.HALF_UP).min(MAX_AMOUNT_2016)
    }
  }
}
