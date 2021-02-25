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

package uk.gov.hmrc.statepension.events

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.statepension.domain.Exclusion

import scala.language.postfixOps

object StatePensionExclusion{
  def apply(nino: Nino, exclusionReasons: List[Exclusion], pensionAge: Int,
            pensionDate: LocalDate, statePensionAgeUnderConsideration: Boolean)(implicit hc: HeaderCarrier): StatePensionExclusion =
    new StatePensionExclusion(nino, exclusionReasons, pensionAge, pensionDate, statePensionAgeUnderConsideration)
}

class StatePensionExclusion(nino: Nino, exclusionReasons: List[Exclusion], pensionAge: Int, pensionDate: LocalDate,
                            statePensionAgeUnderConsideration: Boolean) (implicit hc: HeaderCarrier)
  extends BusinessEvent("StatePensionExclusion", nino,
    Map(
      "reasons" -> exclusionReasons.map(_.toString).mkString(","),
      "pensionAge" -> pensionAge.toString,
      "pensionDate" -> pensionDate.toString,
      "statePensionAgeUnderConsideration" -> statePensionAgeUnderConsideration.toString
    )
  )
