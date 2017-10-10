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

package uk.gov.hmrc.statepension.events

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.statepension.domain.{StatePensionAmount, StatePensionAmounts}

object StatePension{
  def apply(nino: Nino, earningsIncludedUpTo: LocalDate, amounts: StatePensionAmounts, pensionAge: Int,
            pensionDate: LocalDate, finalRelevantYear: String, numberOfQualifyingYears: Int, pensionSharingOrder: Boolean,
            currentFullWeeklyPensionAmount: BigDecimal,reducedRateElection: Boolean, additionalStatePension: BigDecimal,
            graduatedRetirementBenefit:BigDecimal)(implicit hc: HeaderCarrier): StatePension =
    new StatePension(nino, earningsIncludedUpTo, amounts, pensionAge, pensionDate, finalRelevantYear, numberOfQualifyingYears,
      pensionSharingOrder, currentFullWeeklyPensionAmount, reducedRateElection, additionalStatePension, graduatedRetirementBenefit)
}

class StatePension(nino: Nino, earningsIncludedUpTo: LocalDate, amounts: StatePensionAmounts, pensionAge: Int,
                        pensionDate: LocalDate, finalRelevantYear: String, numberOfQualifyingYears: Int,
                        pensionSharingOrder: Boolean, currentFullWeeklyPensionAmount: BigDecimal,
                        reducedRateElection: Boolean, additionalStatePension: BigDecimal,
                        graduatedRetirementBenefit:BigDecimal) (implicit hc: HeaderCarrier)
  extends BusinessEvent("StatePension", nino,
    Map(
      "earningsIncludedUpTo" -> earningsIncludedUpTo.toString,
      "currentAmount.week" -> amounts.current.weeklyAmount.toString,
      "forecastAmount.week" -> amounts.forecast.weeklyAmount.toString,
      "forecastAmount.yearsToWork" -> amounts.forecast.yearsToWork.map(_.toString).getOrElse(""),
      "maximumAmount.week" -> amounts.maximum.weeklyAmount.toString,
      "maximumAmount.yearsToWork" -> amounts.maximum.yearsToWork.map(_.toString).getOrElse(""),
      "maximumAmount.gapsToFill" -> amounts.maximum.gapsToFill.map(_.toString).getOrElse(""),
      "copeAmount.week" -> amounts.cope.weeklyAmount.toString(),
      "pensionAge" -> pensionAge.toString,
      "pensionDate" -> pensionDate.toString,
      "finalRelevantYear" -> finalRelevantYear,
      "numberOfQualifyingYears" -> numberOfQualifyingYears.toString,
      "pensionSharingOrder" -> pensionSharingOrder.toString,
      "currentFullWeeklyPensionAmount" -> currentFullWeeklyPensionAmount.toString(),
      "reducedRateElection" -> reducedRateElection.toString(),
      "additionalStatePension" -> additionalStatePension.toString(),
      "graduatedRetirementBenefit" -> graduatedRetirementBenefit.toString()
    )

  )
