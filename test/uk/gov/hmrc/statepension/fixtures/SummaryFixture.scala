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

import org.joda.time.LocalDate
import uk.gov.hmrc.statepension.domain.nps.{AmountA2016, AmountB2016, PensionAmounts, Summary}

object SummaryFixture {

  val exampleSummaryJson: String = """
  {
    "contractedOutFlag": 0,
    "sensitiveCaseFlag": 0,
    "spaDate": "2019-09-06",
    "finalRelevantYear": 2018,
    "accountNotMaintainedFlag": null,
    "penForecast": {
      "forecastAmount": 160.19,
      "nspMax": 155.65,
      "qualifyingYearsAtSpa": 40,
      "forecastAmount2016": 160.19
    },
    "pensionShareOrderCoeg": false,
    "dateOfDeath": null,
    "sex": "M",
    "statePensionAmount": {
      "nspEntitlement": 161.18,
      "apAmount": 2.36,
      "amountB2016": {
        "mainComponent": 155.65,
        "rebateDerivedAmount": 0.0
      },
      "amountA2016": {
        "ltbPost97ApCashValue": 6.03,
        "ltbCatACashValue": 119.3,
        "ltbPost88CodCashValue": null,
        "ltbPre97ApCashValue": 17.79,
        "ltbPre88CodCashValue": null,
        "grbCash": 2.66,
        "ltbPst88GmpCashValue": null,
        "pre88Gmp": null,
        "ltbPost02ApCashValue": 15.4
      },
      "protectedPayment2016": 5.53,
      "startingAmount": 161.18
    },
    "dateOfBirth": "1954-03-09",
    "nspQualifyingYears": 36,
    "countryCode": 1,
    "nspRequisiteYears": 35,
    "minimumQualifyingPeriod": 1,
    "addressPostcode": "WS9 8LL",
    "reducedRateElectionToConsider": false,
    "pensionShareOrderSerps": true,
    "nino": "QQ123456A",
    "earningsIncludedUpto": "2016-04-05"
  }
  """

  val exampleSummary: Summary = Summary(
    new LocalDate(2016, 4, 5),
    statePensionAgeDate = new LocalDate(2019, 9, 6),
    finalRelevantStartYear = 2018,
    pensionSharingOrderSERPS = true,
    dateOfBirth = new LocalDate(1954, 3, 9),
    None,
    reducedRateElection = false,
    countryCode = 1,
    PensionAmounts(
      pensionEntitlement = 161.18,
      startingAmount2016 = 161.18,
      protectedPayment2016 = 5.53,
      AmountA2016(
        basicStatePension = 119.30,
        pre97AP = 17.79,
        post97AP = 6.03,
        post02AP = 15.4,
        pre88GMP = 0,
        post88GMP = 0,
        pre88COD = 0,
        post88COD = 0,
        graduatedRetirementBenefit = 2.66
      ),
      AmountB2016(
        mainComponent = 155.65,
        rebateDerivedAmount = 0
      )
    ),
    None
  )
}
