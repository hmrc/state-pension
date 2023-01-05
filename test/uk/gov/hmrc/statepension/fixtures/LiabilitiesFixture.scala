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

package uk.gov.hmrc.statepension.fixtures

import uk.gov.hmrc.statepension.domain.nps.Liability

object LiabilitiesFixture {

  def exampleLiabilitiesJson(nino: String) = s"""
  {
    "liabilities": [
      {
        "liabilityTypeEndDate": "1992-11-21",
        "liabilityOccurrenceNo": 1,
        "liabilityTypeStartDate": "1983-11-06",
        "liabilityTypeEndDateReason": "END DATE HELD",
        "liabilityType": 13,
        "nino": "$nino",
        "awardAmount": null
      },
      {
        "liabilityTypeEndDate": "2006-07-08",
        "liabilityOccurrenceNo": 2,
        "liabilityTypeStartDate": "1995-09-24",
        "liabilityTypeEndDateReason": "END DATE HELD",
        "liabilityType": 13,
        "nino": "$nino",
        "awardAmount": null
      },
      {
        "liabilityTypeEndDate": "2006-07-15",
        "liabilityOccurrenceNo": 3,
        "liabilityTypeStartDate": "2006-07-09",
        "liabilityTypeEndDateReason": "END DATE HELD",
        "liabilityType": 13,
        "nino": "$nino",
        "awardAmount": null
      },
      {
        "liabilityTypeEndDate": "2012-01-21",
        "liabilityOccurrenceNo": 4,
        "liabilityTypeStartDate": "2006-09-24",
        "liabilityTypeEndDateReason": "END DATE HELD",
        "liabilityType": 13,
        "nino": "$nino",
        "awardAmount": null
      }
    ]
  }
  """

  val exampleLiabilities: List[Liability] = List(
    Liability(Some(13)),
    Liability(Some(13)),
    Liability(Some(13)),
    Liability(Some(13))
  )

}
