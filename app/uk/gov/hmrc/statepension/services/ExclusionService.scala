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
import uk.gov.hmrc.statepension.domain.Exclusion
import uk.gov.hmrc.statepension.domain.Exclusion.Exclusion
import uk.gov.hmrc.statepension.util.FunctionHelper

class ExclusionService(dateOfDeath: Option[LocalDate], pensionDate: LocalDate, now: LocalDate, reducedRateElection: Boolean) {

  lazy val getExclusions: List[Exclusion] = exclusions(List())

  val checkDead: (List[Exclusion]) => List[Exclusion] = (exclusionList: List[Exclusion]) =>
    dateOfDeath.fold(exclusionList)(_ => Exclusion.Dead :: exclusionList)

  val checkPostStatePensionAge: (List[Exclusion]) => List[Exclusion] = (exclusionList: List[Exclusion]) =>
    if (!now.isBefore(pensionDate.minusDays(1))) {
      Exclusion.PostStatePensionAge :: exclusionList
    } else {
      exclusionList
    }

  val checkMarriedWomensReducedRateElection: (List[Exclusion]) => List[Exclusion] = (exclusionList: List[Exclusion]) =>
    if (reducedRateElection) Exclusion.MarriedWomenReducedRateElection :: exclusionList else exclusionList

  private val exclusions = FunctionHelper.composeAll(List(checkDead, checkPostStatePensionAge, checkMarriedWomensReducedRateElection))
}
