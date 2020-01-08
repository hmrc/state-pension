/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, __}

case class DesNIRecord(qualifyingYears: Int = 0, taxYears: List[DesNITaxYear]) {
  val payableGapsPre2016: Int = taxYears.filter(_.startTaxYear.exists(_ < 2016)).count(_.payable)
  val payableGapsPost2016: Int = taxYears.filter(_.startTaxYear.exists(_ >= 2016)).count(_.payable)
  val qualifyingYearsPost2016: Int = taxYears.filter(_.startTaxYear.exists(_ >= 2016)).count(_.qualifying.get)
  val qualifyingYearsPre2016: Int = qualifyingYears - qualifyingYearsPost2016

  def purge(finalRelevantStartYear: Int): DesNIRecord = {
    val filteredYears = taxYears.filter(_.startTaxYear.get <= finalRelevantStartYear)
    val purgedYears = taxYears.filter(_.startTaxYear.get > finalRelevantStartYear)
    if (purgedYears.nonEmpty) Logger.warn(s"Purged years (FRY $finalRelevantStartYear): ${purgedYears.map(_.startTaxYear).mkString(",")}")

    this.copy(
      qualifyingYears = qualifyingYears - purgedYears.count(_.qualifying.get),
      taxYears = filteredYears
    )
  }
}

object DesNIRecord {
  val readNullableInt: JsPath => Reads[Int] =
    jsPath => jsPath.readNullable[Int].map(_.getOrElse(0))

  val readNullableList:JsPath => Reads[List[DesNITaxYear]] =
    jsPath => jsPath.readNullable[List[DesNITaxYear]].map(_.getOrElse(List.empty)
      .filter(x => x.payableFlag.isDefined || x.qualifying.isDefined || x.startTaxYear.isDefined || x.underInvestigation.isDefined ))

  implicit val reads: Reads[DesNIRecord] = (
      readNullableInt(__ \ "numberOfQualifyingYears") and
      readNullableList(__ \ "taxYears")
    ) (DesNIRecord.apply _)
}
