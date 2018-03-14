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

import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class NpsNITaxYearSpec extends UnitSpec {

  val trueYear: NpsNITaxYear = Json.parse(
    """
      |{
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 1,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 1,
      |      "rattd_tax_year": 1975,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "1285.4500",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "70.6700",
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 0,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 20
      |        },
      |        {
      |          "credit_source_type": 24,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 6
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    }
    """.stripMargin).as[NpsNITaxYear]

  val falseYear: NpsNITaxYear = Json.parse(
    """
      |{
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 0,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1976,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "1285.4500",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "70.6700",
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 0,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 20
      |        },
      |        {
      |          "credit_source_type": 24,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 6
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    }
    """.stripMargin).as[NpsNITaxYear]

  "NpsNITaxYear" when {
    "being read from JSON" should {
      "parse the start tax year correctly" in {
        trueYear.startTaxYear shouldBe 1975
        falseYear.startTaxYear shouldBe 1976
      }

      "parse the payable flag correctly" in {
        trueYear.payableFlag shouldBe true
        falseYear.payableFlag shouldBe false
      }

      "parse under investigation correctly" in {
        trueYear.underInvestigation shouldBe true
        falseYear.underInvestigation shouldBe false
      }

      "parse qualifying correctly" in {
        trueYear.qualifying shouldBe true
        falseYear.qualifying shouldBe false
      }
    }

    "payable" when {
      "payableFlag is 0" should {
        "return false" in {
          NpsNITaxYear(2015, qualifying = false, underInvestigation = false, payableFlag = false).payable shouldBe false
        }
      }
      "payableFlag is true" when {
        "the year is qualifying" should {
          "return false" in {
            NpsNITaxYear(2015, qualifying = true, underInvestigation = false, payableFlag = true).payable shouldBe false
          }
        }
        "the year is not qualifying" when {
          "the year is under investigation" should {
            "return false" in {
              NpsNITaxYear(2015, qualifying = false, underInvestigation = true, payableFlag = true).payable shouldBe false
            }
          }
          "the year is not under investigation" should {
            "return true" in {
              NpsNITaxYear(2015, qualifying = false, underInvestigation = false, payableFlag = true).payable shouldBe true
            }
          }
        }
      }
    }
  }

}
