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

import org.joda.time.LocalDate
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class SummarySpec extends UnitSpec {

  "Summary" should {
    "deserialise correctly" in {
      val jsonPayload =
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

      val result = DesSummary(earningsIncludedUpTo = LocalDate.parse("2014-01-01"),
        sex = "M",
        statePensionAgeDate = LocalDate.parse("2014-08-25"),
        finalRelevantStartYear = 2014,
        pensionSharingOrderSERPS = true,
        dateOfBirth = LocalDate.parse("2014-08-25"),
        dateOfDeath = Some(LocalDate.parse("2014-08-25")),
        reducedRateElection = true,
        countryCode = 11,
        amounts = DesStatePensionAmounts(
          pensionEntitlement = 89,
          startingAmount2016 = 11,
          protectedPayment2016 = 12,
          amountA2016 = DesAmountA2016(
            basicStatePension = 123,
            pre97AP = 123,
            post97AP = 123,
            post02AP = 123,
            pre88GMP = 11,
            post88GMP = 122,
            pre88COD = 123,
            post88COD = 123,
            graduatedRetirementBenefit = 123
      ),
          amountB2016 = DesAmountB2016(
            mainComponent = 12,
            rebateDerivedAmount = 34
          )
        ))

      Json.parse(jsonPayload).as[DesSummary] shouldBe result
    }
  }

}
