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

package uk.gov.hmrc.statepension.services

import org.joda.time.LocalDate
import play.api.Logging
import uk.gov.hmrc.statepension.domain.Exclusion
import uk.gov.hmrc.statepension.domain.Exclusion._
import uk.gov.hmrc.statepension.domain.nps.{Liability, LiabilityType}
import uk.gov.hmrc.statepension.util.FunctionHelper

case class ExclusionService(dateOfDeath: Option[LocalDate],
                            pensionDate: LocalDate,
                            now: LocalDate,
                            entitlement: BigDecimal,
                            startingAmount: BigDecimal,
                            calculatedStartingAmount: BigDecimal,
                            liabilities: List[Liability],
                            manualCorrespondenceOnly: Boolean) extends Logging {

  lazy val getExclusions: List[Exclusion] = exclusions(List())

  private val checkDead = (exclusionList: List[Exclusion]) =>
    dateOfDeath.fold(exclusionList)(_ => Dead :: exclusionList)

  private val checkManualCorrespondence = (exclusionList: List[Exclusion]) =>
    if (manualCorrespondenceOnly) ManualCorrespondenceIndicator :: exclusionList
    else exclusionList

  private val checkPostStatePensionAge = (exclusionList: List[Exclusion]) =>
    if (!now.isBefore(pensionDate.minusDays(1))) {
      PostStatePensionAge :: exclusionList
    } else {
      exclusionList
    }

  private val checkAmountDissonance = (exclusionList: List[Exclusion]) =>
    if (startingAmount != calculatedStartingAmount) {
      logger.warn(s"Dissonance Found!: Entitlement - $entitlement Starting - $startingAmount Components - $calculatedStartingAmount")
      AmountDissonance :: exclusionList
    } else {
      exclusionList
    }

  private val checkIsleOfMan = (exclusionList: List[Exclusion]) =>
    if (liabilities.exists(_.liabilityType.contains( LiabilityType.ISLE_OF_MAN))) IsleOfMan :: exclusionList
    else exclusionList

  private val exclusions = FunctionHelper.composeAll(List(
    checkDead,
    checkManualCorrespondence,
    checkPostStatePensionAge,
    checkAmountDissonance,
    checkIsleOfMan
  ))
}
