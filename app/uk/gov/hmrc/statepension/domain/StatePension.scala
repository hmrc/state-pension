/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.statepension.domain

import java.time.LocalDate
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, JsPath, Json, Writes}
import uk.gov.hmrc.statepension.domain.PolicyDecisions.MINIMUM_QUALIFYING_YEARS

import scala.math.BigDecimal.RoundingMode

case class StatePensionAmount(yearsToWork: Option[Int],
                              gapsToFill: Option[Int],
                              weeklyAmount: BigDecimal) {
  val monthlyAmount: BigDecimal = (((weeklyAmount / 7) * 365.25) / 12).setScale(2, RoundingMode.HALF_UP)
  val annualAmount: BigDecimal = ((weeklyAmount / 7) * 365.25).setScale(2, RoundingMode.HALF_UP)
}

object StatePensionAmount {
  implicit val reads = Json.reads[StatePensionAmount]
  implicit val writes: Writes[StatePensionAmount] = (
    (JsPath \ "yearsToWork").writeNullable[Int] and
      (JsPath \ "gapsToFill").writeNullable[Int] and
      (JsPath \ "weeklyAmount").write[BigDecimal] and
      (JsPath \ "monthlyAmount").write[BigDecimal] and
      (JsPath \ "annualAmount").write[BigDecimal]
    )((sp: StatePensionAmount) => (sp.yearsToWork, sp.gapsToFill, sp.weeklyAmount, sp.monthlyAmount, sp.annualAmount))

  implicit val formats: Format[StatePensionAmount] = Format(reads, writes)
}

case class OldRules(basicStatePension:BigDecimal,
                    additionalStatePension:BigDecimal,
                    graduatedRetirementBenefit:BigDecimal
                   )

object OldRules {
  implicit val formats = Json.format[OldRules]
}

case class NewRules(grossStatePension:BigDecimal, rebateDerivedAmount:BigDecimal)

object NewRules {
  implicit val formats = Json.format[NewRules]
}

case class StatePensionAmounts(protectedPayment: Boolean,
                               current: StatePensionAmount,
                               forecast: StatePensionAmount,
                               maximum: StatePensionAmount,
                               cope: StatePensionAmount,
                               starting: StatePensionAmount,
                               oldRules: OldRules,
                               newRules: NewRules)

object StatePensionAmounts {
  implicit val formats = Json.format[StatePensionAmounts]
}

case class StatePension(earningsIncludedUpTo: LocalDate,
                        amounts: StatePensionAmounts,
                        pensionAge: Int,
                        pensionDate: LocalDate,
                        finalRelevantYear: String,
                        numberOfQualifyingYears: Int,
                        pensionSharingOrder: Boolean,
                        currentFullWeeklyPensionAmount: BigDecimal,
                        reducedRateElection: Boolean,
                        reducedRateElectionCurrentWeeklyAmount: Option[BigDecimal],
                        statePensionAgeUnderConsideration: Boolean) {
  lazy val contractedOut: Boolean = amounts.cope.weeklyAmount > 0

  lazy val forecastScenario: Scenario = {
    if (amounts.maximum.weeklyAmount == 0) {
      Scenario.CantGetPension
    } else if(amounts.maximum.weeklyAmount > amounts.forecast.weeklyAmount) {
      Scenario.FillGaps
    } else {
      if(amounts.forecast.weeklyAmount > amounts.current.weeklyAmount) {

        if (amounts.forecast.weeklyAmount >= currentFullWeeklyPensionAmount)
          Scenario.ContinueWorkingMax
        else Scenario.ContinueWorkingNonMax

      } else if(amounts.forecast.weeklyAmount == amounts.current.weeklyAmount) {
        Scenario.Reached
      } else {
        Scenario.ForecastOnly
      }
    }
  }

  lazy val mqpScenario: Option[MQPScenario] = {
    if (amounts.current.weeklyAmount > 0 && numberOfQualifyingYears >= MINIMUM_QUALIFYING_YEARS) {
      None
    } else {
      if (amounts.forecast.weeklyAmount > 0) {
        Some(MQPScenario.ContinueWorking)
      } else {
        if (amounts.maximum.weeklyAmount > 0) {
          Some(MQPScenario.CanGetWithGaps)
        } else {
          Some(MQPScenario.CantGet)
        }
      }
    }
  }
}

object StatePension {
  implicit val formats = Json.format[StatePension]
}
