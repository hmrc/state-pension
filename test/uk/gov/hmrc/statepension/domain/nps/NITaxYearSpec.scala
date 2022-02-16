/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.statepension.StatePensionBaseSpec

class NITaxYearSpec extends StatePensionBaseSpec {

  val trueYear: NITaxYear = Json.parse(
  """
      |{
      |      "qualifying": true,
      |      "underInvestigationFlag": true,
      |      "payable": true,
      |      "rattdTaxYear": "1975"
      |}
    """.stripMargin).as[NITaxYear]

  val falseYear: NITaxYear = Json.parse(
    """
      |{
      |      "qualifying": false,
      |      "underInvestigationFlag": false,
      |      "payable": false,
      |      "rattdTaxYear": "1976"
      |}
    """.stripMargin).as[NITaxYear]

  "NITaxYear" when {
    "being read from JSON" should {
      "parse the start tax year correctly" in {
        trueYear.startTaxYear shouldBe Some(1975)
        falseYear.startTaxYear shouldBe Some(1976)
      }

      "parse the payable flag correctly" in {
        trueYear.payableFlag shouldBe Some(true)
        falseYear.payableFlag shouldBe Some(false)
      }

      "parse under investigation correctly" in {
        trueYear.underInvestigation shouldBe Some(true)
        falseYear.underInvestigation shouldBe Some(false)
      }

      "parse qualifying correctly" in {
        trueYear.qualifying shouldBe Some(true)
        falseYear.qualifying shouldBe Some(false)
      }
    }

    "payable" when {
      "payableFlag is 0" should {
        "return false" in {
          NITaxYear(Some(2015), qualifying = Some(false), underInvestigation = Some(false), payableFlag = Some(false)).payable shouldBe false
        }
      }
      "payableFlag is true" when {
        "the year is qualifying" should {
          "return false" in {
            NITaxYear(Some(2015), qualifying = Some(true), underInvestigation = Some(false), payableFlag = Some(true)).payable shouldBe false
          }
        }
        "the year is not qualifying" when {
          "the year is under investigation" should {
            "return false" in {
              NITaxYear(Some(2015), qualifying = Some(false), underInvestigation = Some(true), payableFlag = Some(true)).payable shouldBe false
            }
          }
          "the year is not under investigation" should {
            "return true" in {
              NITaxYear(Some(2015), qualifying = Some(false), underInvestigation = Some(false), payableFlag = Some(true)).payable shouldBe true
            }
          }
        }
      }
    }
  }
}