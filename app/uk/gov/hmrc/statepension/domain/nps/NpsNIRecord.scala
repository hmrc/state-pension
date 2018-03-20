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

import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, __}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

case class NpsNIRecord(qualifyingYears: Int, taxYears: List[NpsNITaxYear]) {
  val payableGapsPre2016: Int = taxYears.filter(_.startTaxYear < 2016).count(_.payable)
  val payableGapsPost2016: Int = taxYears.filter(_.startTaxYear >= 2016).count(_.payable)
  val qualifyingYearsPost2016: Int = taxYears.filter(_.startTaxYear >= 2016).count(_.qualifying)
  val qualifyingYearsPre2016: Int = qualifyingYears - qualifyingYearsPost2016

  def purge(finalRelevantStartYear: Int): NpsNIRecord = {
    val filteredYears = taxYears.filter(_.startTaxYear <= finalRelevantStartYear)
    val purgedYears = taxYears.filter(_.startTaxYear > finalRelevantStartYear)
    if (purgedYears.nonEmpty) Logger.warn(s"Purged years (FRY $finalRelevantStartYear): ${purgedYears.map(_.startTaxYear).mkString(",")}")

    this.copy(
      qualifyingYears = qualifyingYears - purgedYears.count(_.qualifying),
      taxYears = filteredYears
    )
  }
}

object NpsNIRecord {
  implicit val reads: Reads[NpsNIRecord] = (
      (__ \ "number_of_qualifying_years").read[Int] and
      (__ \ "npsLnitaxyr").read[List[NpsNITaxYear]]
    ) (NpsNIRecord.apply _)
}
