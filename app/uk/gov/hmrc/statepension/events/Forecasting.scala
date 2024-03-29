/*
 * Copyright 2023 HM Revenue & Customs
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

import java.time.LocalDate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.statepension.domain.Exclusion
import uk.gov.hmrc.statepension.domain.nps.{AmountA2016, AmountB2016}

object Forecasting {
    def apply(nino: Nino, earningsIncludedUpTo: LocalDate, currentQualifyingYears: Int, amountA: AmountA2016, amountB: AmountB2016,
              finalRelevantYear: Int, exclusions: List[Exclusion])(implicit hc: HeaderCarrier): Forecasting =
        new Forecasting(nino, earningsIncludedUpTo, currentQualifyingYears, amountA, amountB, finalRelevantYear, exclusions)
}

class Forecasting(nino: Nino, earningsIncludedUpTo: LocalDate, currentQualifyingYears: Int, amountA: AmountA2016,
                  amountB: AmountB2016, finalRelevantYear: Int, exclusions: List[Exclusion])
                 (implicit hc: HeaderCarrier)
  extends BusinessEvent("Forecasting", nino, Map(
      "nino" -> nino.value,
      "earningsIncludedUpTo" -> earningsIncludedUpTo.toString,
      "currentQualifyingYears" -> currentQualifyingYears.toString,
      "amountAtotal" -> amountA.total.toString(),
      "basicPension" -> amountA.basicStatePension.toString(),
      "pre97AP" -> amountA.pre97AP.toString(),
      "post97AP" -> amountA.post97AP.toString(),
      "post02AP" -> amountA.post02AP.toString(),
      "pre88GMP" -> amountA.pre88GMP.toString(),
      "post88GMP" -> amountA.post88GMP.toString(),
      "pre88COD" -> amountA.pre88COD.toString(),
      "post88COD" -> amountA.post88COD.toString(),
      "grb" -> amountA.graduatedRetirementBenefit.toString(),
      "totalAP" -> amountA.totalAP.toString(),
      "amountBmain" -> amountB.mainComponent.toString(),
      "rda" -> amountB.rebateDerivedAmount.toString(),
      "fry" -> finalRelevantYear.toString,
      "exclusions" -> exclusions.map(_.toString).mkString(",")
  ))