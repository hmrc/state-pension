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

package uk.gov.hmrc.statepension.domain.nps

import play.api.Logger
import play.api.libs.json.{Reads, __}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

case class NpsNIRecord(taxYears: List[NpsNITaxYear]) {
  val payableGaps: Int = taxYears.count(_.payable)

  def purge(finalRelevantStartYear: Int): NpsNIRecord = {
    val filteredYears = taxYears.filter(_.startTaxYear <= finalRelevantStartYear)
    val purgedYears = taxYears.filter(_.startTaxYear > finalRelevantStartYear)
    if(purgedYears.nonEmpty) Logger.info(s"Purged years (FRY $finalRelevantStartYear): ${purgedYears.map(_.startTaxYear).mkString(",")}")

    this.copy(
      taxYears = filteredYears
    )
  }
}



object NpsNIRecord {
  implicit val reads: Reads[NpsNIRecord] = (__ \ "npsLnitaxyr").read[List[NpsNITaxYear]].map(NpsNIRecord.apply)
}