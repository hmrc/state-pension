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
import utils.ProxyCacheTestData.summaryJson
import utils.StatePensionBaseSpec

import java.time.LocalDate

class SummarySpec extends StatePensionBaseSpec {

  "Summary" should {
    "deserialise correctly" in {

      val result = Summary(earningsIncludedUpTo = LocalDate.parse("2014-01-01"),
        statePensionAgeDate = LocalDate.parse("2014-08-25"),
        finalRelevantStartYear = 2014,
        pensionSharingOrderSERPS = true,
        dateOfBirth = LocalDate.parse("2014-08-25"),
        dateOfDeath = Some(LocalDate.parse("2014-08-25")),
        reducedRateElection = true,
        countryCode = 11,
        amounts = PensionAmounts(
          pensionEntitlement = 89,
          startingAmount2016 = 11,
          protectedPayment2016 = 12,
          amountA2016 = AmountA2016(
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
          amountB2016 = AmountB2016(
            mainComponent = 12,
            rebateDerivedAmount = 34
          )
        ),
        manualCorrespondenceIndicator = None
      )

      Json.parse(summaryJson).as[Summary] shouldBe result
    }
  }

}
