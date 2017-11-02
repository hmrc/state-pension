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
import play.Logger
import uk.gov.hmrc.statepension.domain.Exclusion
import uk.gov.hmrc.statepension.domain.Exclusion.Exclusion
import uk.gov.hmrc.statepension.domain.nps.{LiabilityType, NpsLiability}
import uk.gov.hmrc.statepension.util.FunctionHelper

class ExclusionService(dateOfDeath: Option[LocalDate],
                       pensionDate: LocalDate,
                       now: LocalDate,
                       isAbroad: Boolean,
                       sex: String,
                       entitlement: BigDecimal,
                       startingAmount: BigDecimal,
                       calculatedStartingAmount: BigDecimal,
                       liabilities: List[NpsLiability],
                       manualCorrespondenceOnly: Boolean) {

  lazy val getExclusions: List[Exclusion] = exclusions(List())

  private val checkDead = (exclusionList: List[Exclusion]) =>
    dateOfDeath.fold(exclusionList)(_ => Exclusion.Dead :: exclusionList)

  private val checkManualCorrespondence = (exclusionList: List[Exclusion]) =>
    if (manualCorrespondenceOnly) Exclusion.ManualCorrespondenceIndicator :: exclusionList
    else exclusionList

  private val checkPostStatePensionAge = (exclusionList: List[Exclusion]) =>
    if (!now.isBefore(pensionDate.minusDays(1))) {
      Exclusion.PostStatePensionAge :: exclusionList
    } else {
      exclusionList
    }

  private val checkAmountDissonance = (exclusionList: List[Exclusion]) =>
    if (startingAmount != calculatedStartingAmount) {
      Logger.warn(s"Dissonance Found!: Entitlement - $entitlement Starting - $startingAmount Components - $calculatedStartingAmount")
      Exclusion.AmountDissonance :: exclusionList
    } else {
      exclusionList
    }

  private val checkIsleOfMan = (exclusionList: List[Exclusion]) =>
    if (liabilities.exists(_.liabilityType == LiabilityType.ISLE_OF_MAN)) Exclusion.IsleOfMan :: exclusionList
    else exclusionList

  // scalastyle:off magic.number
  final val AUTO_CREDITS_EXCLUSION_DATE = new LocalDate(2018, 10, 6)
  // scalastyle:on magic.number

  private val checkOverseasMaleAutoCredits = (exclusionList: List[Exclusion]) => {
    if (sex.equalsIgnoreCase("M") && isAbroad && pensionDate.isBefore(AUTO_CREDITS_EXCLUSION_DATE)) {
      Exclusion.Abroad :: exclusionList
    } else {
      exclusionList
    }
  }

  private val exclusions = FunctionHelper.composeAll(List(
    checkDead,
    checkManualCorrespondence,
    checkPostStatePensionAge,
    checkAmountDissonance,
    checkIsleOfMan,
    checkOverseasMaleAutoCredits
  ))
}
