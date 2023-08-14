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

package uk.gov.hmrc.statepension.domain.nps

import play.api.libs.json.Json
import utils.ProxyCacheTestData.niRecordJson
import utils.StatePensionBaseSpec

class NIRecordSpec extends StatePensionBaseSpec {
  "DesNIRecord" should {
    "deserialise correctly" in {

      val testData = NIRecord(
        qualifyingYears = 31,
        taxYears =  List(
          NITaxYear(
            startTaxYear = Some(2016),
            qualifying = Some(true),
            underInvestigation = Some(false),
            payableFlag = Some(true)
          ),
          NITaxYear(
            startTaxYear = Some(2015),
            qualifying = Some(true),
            underInvestigation = Some(true),
            payableFlag = Some(true)
          )
        )
      )

      Json.parse(niRecordJson).as[NIRecord] shouldBe testData

    }
  }


  "return a valid NiRecord with empty tax year records" in {
    val jsonPayload = """|{
                         |  "yearsToFry": 2,
                         |  "nonQualifyingYears": 11,
                         |  "dateOfEntry": null,
                         |  "employmentDetails": [],
                         |  "pre75CcCount": 250,
                         |  "numberOfQualifyingYears": 36,
                         |  "nonQualifyingYearsPayable": 6,
                         |  "taxYears": [
                         |    {
                         |    }
                         |  ],
                         |  "nino": "YN315615"
                         |}""".stripMargin

    val testData = NIRecord(
      qualifyingYears = 36,
      taxYears =  List.empty
    )

    Json.parse(jsonPayload).as[NIRecord] shouldBe testData
  }

  "return a valid NiRecord with empty qualifying years" in {
    val jsonPayload = """|{
                         |  "yearsToFry": 2,
                         |  "nonQualifyingYears": 11,
                         |  "dateOfEntry": null,
                         |  "employmentDetails": [],
                         |  "pre75CcCount": 250,
                         |  "nonQualifyingYearsPayable": 6,
                         |  "taxYears": [
                         |    {
                         |    }
                         |  ],
                         |  "nino": "YN315615"
                         |}""".stripMargin

    val testData = NIRecord(
      qualifyingYears = 0,
      taxYears =  List.empty
    )

    Json.parse(jsonPayload).as[NIRecord] shouldBe testData
  }


}
