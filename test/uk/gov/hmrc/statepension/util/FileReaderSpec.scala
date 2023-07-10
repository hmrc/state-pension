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

package uk.gov.hmrc.statepension.util

import uk.gov.hmrc.statepension.models.TaxRates
import uk.gov.hmrc.statepension.services.TaxYearResolver
import utils.UnitSpec

class FileReaderSpec extends UnitSpec {

  "getTaxRatesByYear" should {
    "return Exception" when {
      "filePath returns null" in {
        intercept[Exception]{
          FileReader.getTaxRatesByTaxYear(2020)
        }.getMessage shouldBe "Not Found: tax rates for requested year = 2020"
      }
    }

    "return TaxRates" when {
      "filePqth finds TaxRates json for valid year" in {
        FileReader.getTaxRatesByTaxYear(TaxYearResolver.currentTaxYear) shouldBe a[TaxRates]
      }
    }
  }
}
