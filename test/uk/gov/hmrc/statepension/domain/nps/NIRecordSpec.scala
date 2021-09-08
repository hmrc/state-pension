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

package uk.gov.hmrc.statepension.domain.nps

import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.Json
import uk.gov.hmrc.statepension.StatePensionBaseSpec

class NIRecordSpec extends StatePensionBaseSpec {
  "DesNIRecord" should {
    "deserialise correctly" in {

      val jsonPayload =
        """
          |{
          |    "dateOfEntry": "2001-01-01",
          |    "nino": "AB123456A",
          |    "nonQualifyingYears": 3,
          |    "nonQualifyingYearsPayable": 2,
          |    "numberOfQualifyingYears": 31,
          |    "pre75CcCount": 0,
          |    "yearsToFry": 9,
          |    "employmentDetails": [
          |        {
          |            "employerName": "Fred Blogg's Enterprises",
          |            "startDate": "1981-02-02",
          |            "endDate": "1984-03-03"
          |        },
          |        {
          |            "employerName": "ACME Industries Limited",
          |            "startDate": "1984-04-04",
          |            "endDate": "1989-02-28"
          |        },
          |        {
          |            "employerName": "Stuck Forever Limited",
          |            "startDate": "1989-04-04"
          |        }
          |    ],
          |    "taxYears": [
          |        {
          |            "amountNeeded": 123.45,
          |            "classThreePayable": 99.99,
          |            "classThreePayableBy": "2018-06-05",
          |            "classThreePayableByPenalty": "2018-08-01",
          |            "classTwoOutstandingWeeks": 4,
          |            "classTwoPayable": 0.00,
          |            "classTwoPayableBy": "2018-06-05",
          |            "classTwoPayableByPenalty": "2018-08-01",
          |            "coClassOnePaid": 9999.99,
          |            "coPrimaryPaidEarnings": 78901.22,
          |            "niEarnings": 24567.89,
          |            "niEarningsEmployed": 12345.67,
          |            "niEarningsSelfEmployed": 99,
          |            "niEarningsVoluntary": 0,
          |            "otherCredits": [
          |                {
          |                    "ccType": 45,
          |                    "creditSourceType": 11,
          |                    "numberOfCredits": -99
          |                }
          |            ],
          |            "payable": true,
          |            "qualifying": true,
          |            "rattdTaxYear": "2016",
          |            "underInvestigationFlag": false,
          |            "primaryPaidEarnings": 987654.32
          |        },
          |        {
          |            "amountNeeded": 123.45,
          |            "classThreePayable": 99.99,
          |            "classThreePayableBy": "2018-06-05",
          |            "classThreePayableByPenalty": "2018-08-01",
          |            "classTwoOutstandingWeeks": 4,
          |            "classTwoPayable": 0.00,
          |            "classTwoPayableBy": "2018-06-05",
          |            "classTwoPayableByPenalty": "2018-08-01",
          |            "coClassOnePaid": 9999.99,
          |            "coPrimaryPaidEarnings": 78901.22,
          |            "niEarnings": 24567.89,
          |            "niEarningsEmployed": 12345.67,
          |            "niEarningsSelfEmployed": 99,
          |            "niEarningsVoluntary": 0,
          |            "otherCredits": [
          |                {
          |                    "ccType": 45,
          |                    "creditSourceType": 11,
          |                    "numberOfCredits": -99
          |                }
          |            ],
          |            "payable": true,
          |            "qualifying": true,
          |            "rattdTaxYear": "2015",
          |            "underInvestigationFlag": true,
          |            "primaryPaidEarnings": 987654.32
          |        }
          |    ]
          |}
        """.stripMargin

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

      Json.parse(jsonPayload).as[NIRecord] shouldBe testData

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
