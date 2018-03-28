/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.statepension.domain.nps

import org.joda.time.{LocalDate, Period}
import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._

import scala.math.BigDecimal.RoundingMode

case class DesSummary(
                       earningsIncludedUpTo: LocalDate,
                       sex: String,
                       statePensionAgeDate: LocalDate,
                       finalRelevantStartYear: Int,
                       pensionSharingOrderSERPS: Boolean,
                       dateOfBirth: LocalDate,
                       dateOfDeath: Option[LocalDate] = None,
                       reducedRateElection: Boolean = false,
                       countryCode: Int = 0,
                       amounts: DesStatePensionAmounts = DesStatePensionAmounts()
                     ) {
  val finalRelevantYear: String = s"$finalRelevantStartYear-${(finalRelevantStartYear + 1).toString.takeRight(2)}"
  val statePensionAge: Int = new Period(dateOfBirth, statePensionAgeDate).getYears
}

object DesSummary {

  val readBooleanFromInt: JsPath => Reads[Boolean] =
    jsPath => jsPath.readNullable[Int].map(_.getOrElse(0) != 0)

  val readNullableInt: JsPath => Reads[Int] =
    jsPath => jsPath.readNullable[Int].map(_.getOrElse(0))

  val readBooleanWithDefault: JsPath => Reads[Boolean] =
    jsPath => jsPath.readNullable[Boolean].map(_.getOrElse(false))

  implicit val reads: Reads[DesSummary] = (
    (JsPath \ "earningsIncludedUpto").read[LocalDate] and
      (JsPath \ "sex").read[String] and
      (JsPath \ "spaDate").read[LocalDate] and
      (JsPath \ "finalRelevantYear").read[Int] and
      readBooleanWithDefault(JsPath \ "pensionShareOrderSerps") and
      (JsPath \ "dateOfBirth").read[LocalDate] and
      (JsPath \ "dateOfDeath").readNullable[LocalDate] and
      readBooleanWithDefault(JsPath \ "reducedRateElectionToConsider") and
      readNullableInt(JsPath \ "countryCode") and
      (JsPath \ "statePensionAmount").read[DesStatePensionAmounts]
    ) (DesSummary.apply _)

}

case class DesStatePensionAmounts(
                                   pensionEntitlement: BigDecimal = 0,
                                   startingAmount2016: BigDecimal = 0,
                                   protectedPayment2016: BigDecimal = 0,
                                   amountA2016: DesAmountA2016 = DesAmountA2016(),
                                   amountB2016: DesAmountB2016 = DesAmountB2016()
                                 ) {
  lazy val pensionEntitlementRounded: BigDecimal = pensionEntitlement.setScale(2, RoundingMode.HALF_UP)
}

object DesStatePensionAmounts {

  val readBigDecimal: JsPath => Reads[BigDecimal] =
    jsPath => jsPath.readNullable[BigDecimal].map(_.getOrElse(0))

  implicit val reads: Reads[DesStatePensionAmounts] = (
    readBigDecimal(JsPath \ "nspEntitlement") and
      readBigDecimal(JsPath \ "startingAmount") and
      readBigDecimal(JsPath \ "protectedPayment2016") and
      (JsPath \ "amountA2016").read[DesAmountA2016] and
      (JsPath \ "amountB2016").read[DesAmountB2016]
    ) (DesStatePensionAmounts.apply _)
}

case class DesAmountA2016(
                           basicStatePension: BigDecimal = 0,
                           pre97AP: BigDecimal = 0,
                           post97AP: BigDecimal = 0,
                           post02AP: BigDecimal = 0,
                           pre88GMP: BigDecimal = 0,
                           post88GMP: BigDecimal = 0,
                           pre88COD: BigDecimal = 0,
                           post88COD: BigDecimal = 0,
                           graduatedRetirementBenefit: BigDecimal = 0
                         ) {
  val additionalStatePension: BigDecimal = (pre97AP - (pre88GMP + post88GMP + pre88COD + post88COD)).max(0) + post97AP + post02AP

  val totalAP: BigDecimal = additionalStatePension + graduatedRetirementBenefit
  val total: BigDecimal = totalAP + basicStatePension

}

object DesAmountA2016 {

  val readBigDecimal: JsPath => Reads[BigDecimal] =
    jsPath => jsPath.readNullable[BigDecimal].map(_.getOrElse(0))

  implicit val reads: Reads[DesAmountA2016] = (
    readBigDecimal(JsPath \ "ltbCatACashValue") and
      readBigDecimal(JsPath \ "ltbPre97ApCashValue") and
      readBigDecimal(JsPath \ "ltbPost97ApCashValue") and
      readBigDecimal(JsPath \ "ltbPost02ApCashValue") and
      readBigDecimal(JsPath \ "pre88Gmp") and
      readBigDecimal(JsPath \ "ltbPst88GmpCashValue") and
      readBigDecimal(JsPath \ "ltbPre88CodCashValue") and
      readBigDecimal(JsPath \ "ltbPost88CodCashValue") and
      readBigDecimal(JsPath \ "grbCash")
    ) (DesAmountA2016.apply _)

}

case class DesAmountB2016(mainComponent: BigDecimal = 0, rebateDerivedAmount: BigDecimal = 0)

object DesAmountB2016 {
  val readBigDecimal: JsPath => Reads[BigDecimal] =
    jsPath => jsPath.readNullable[BigDecimal].map(_.getOrElse(0))

  implicit val reads: Reads[DesAmountB2016] = (
    readBigDecimal(JsPath \ "mainComponent") and
      readBigDecimal(JsPath \ "rebateDerivedAmount")
    ) (DesAmountB2016.apply _)
}
