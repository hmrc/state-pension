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

package utils

import play.api.libs.json.Json
import uk.gov.hmrc.statepension.domain.nps.{Liabilities, NIRecord, ProxyCacheData, Summary}

object ProxyCacheTestData {
  val niRecordJson: String =
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

  val summaryJson: String =
    """
      |{
      |    "nino": "SK196234",
      |    "accountNotMaintainedFlag": false,
      |    "addressPostcode": "TF3 4ER",
      |    "contractedOutFlag": 2,
      |    "countryCode": 11,
      |    "dateOfBirth": "2014-08-25",
      |    "dateOfDeath": "2014-08-25",
      |    "earningsIncludedUpto": "2014-01-01",
      |    "finalRelevantYear": 2014,
      |    "minimumQualifyingPeriod": true,
      |    "nspQualifyingYears": 1,
      |    "nspRequisiteYears": 1,
      |    "pensionShareOrderCoeg": true,
      |    "pensionShareOrderSerps": true,
      |    "reducedRateElectionToConsider": true,
      |    "sensitiveCaseFlag": 1,
      |    "sex": "M",
      |    "spaDate": "2014-08-25",
      |    "pensionForecast": {
      |        "forecastAmount": 1234,
      |        "forecastAmount2016": 123,
      |        "nspMax": 567,
      |        "qualifyingYearsAtSpa": 12
      |    },
      |    "statePensionAmount": {
      |        "apAmount": 123,
      |        "amountA2016": {
      |            "grbCash": 123,
      |            "ltbCatACashValue": 123,
      |            "ltbPost02ApCashValue": 123,
      |            "ltbPost88CodCashValue": 123,
      |            "ltbPost97ApCashValue": 123,
      |            "ltbPre88CodCashValue": 123,
      |            "ltbPre97ApCashValue": 123,
      |            "ltbPst88GmpCashValue": 122,
      |            "pre88Gmp": 11
      |        },
      |        "amountB2016": {
      |            "mainComponent": 12,
      |            "rebateDerivedAmount": 34
      |        },
      |        "nspEntitlement": 89,
      |        "protectedPayment2016": 12,
      |        "startingAmount": 11
      |    }
      |}
          """.stripMargin

  val liabilitiesJson: String =
    """
      |{
      |    "liabilities": [
      |        {
      |            "awardAmount": 123.49,
      |            "liabilityOccurrenceNo": 89,
      |            "liabilityType": 45,
      |            "liabilityTypeEndDate": "2014-08-25",
      |            "liabilityTypeEndDateReason": "END DATE HELD",
      |            "liabilityTypeStartDate": "2014-08-25",
      |            "nino":"SK196234"
      |        },
      |        {
      |            "awardAmount": 456.54,
      |            "liabilityOccurrenceNo": 90,
      |            "liabilityType": 45,
      |            "liabilityTypeEndDate": "2018-08-25",
      |            "liabilityTypeEndDateReason": "END DATE HELD",
      |            "liabilityTypeStartDate": "2017-08-26",
      |            "nino":"SK196234"
      |        }
      |
      |    ]
      |}
  """.stripMargin

  val summary: Summary =
    Json.parse(summaryJson).as[Summary]

  val niRecord: NIRecord =
    Json.parse(niRecordJson).as[NIRecord]

  val liabilities: Liabilities =
    Json.parse(liabilitiesJson).as[Liabilities]

  val proxyCacheData: ProxyCacheData =
    ProxyCacheData(
      summary = summary,
      niRecord = niRecord,
      liabilities = liabilities
    )

  val proxyCacheDataJson: String =
    Json.obj(
      "summary" -> Json.parse(summaryJson),
      "niRecord" -> Json.parse(niRecordJson),
      "liabilities" -> Json.parse(liabilitiesJson)
    ).toString()
}
