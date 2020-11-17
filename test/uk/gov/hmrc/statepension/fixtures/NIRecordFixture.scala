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

package uk.gov.hmrc.statepension.fixtures

import uk.gov.hmrc.statepension.domain.nps.NIRecord

object NIRecordFixture {

  def exampleDesNiRecordJson(nino: String): String =
    s"""{
      "yearsToFry": 3,
      "nonQualifyingYears": 10,
      "dateOfEntry": "1969-08-01",
      "employmentDetails": [],
      "pre75CcCount": 250,
      "numberOfQualifyingYears": 36,
      "nonQualifyingYearsPayable": 5,
      "nino": "$nino"
    }"""

  val exampleDesNiRecord: NIRecord = NIRecord(qualifyingYears = 36, List.empty)

}
