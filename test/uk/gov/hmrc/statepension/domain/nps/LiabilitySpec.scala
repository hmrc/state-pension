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

class LiabilitySpec extends StatePensionBaseSpec {

  "Liability" should {
    "deserialise correctly" in {
      val jsonPayload =
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

      val testData = Liabilities(
        List(
          Liability(liabilityType = Some(45)),
          Liability(liabilityType = Some(45))
        )
      )

      Json.parse(jsonPayload).as[Liabilities] shouldBe testData
    }

    "give an emptyList for empty json object" in {
      val jsonPayload =
        """
          |{
          |    "liabilities": [
          |        {
          |            "awardAmount": 123.49,
          |            "liabilityOccurrenceNo": 89,
          |            "liabilityTypeEndDate": "2014-08-25",
          |            "liabilityTypeEndDateReason": "END DATE HELD",
          |            "liabilityTypeStartDate": "2014-08-25",
          |            "nino":"SK196234"
          |        }
          |    ]
          |}
        """.stripMargin

      val testData = Liabilities(
        List()
      )

      Json.parse(jsonPayload).as[Liabilities] shouldBe testData
    }
  }

}
