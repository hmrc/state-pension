/*
 * Copyright 2017 HM Revenue & Customs
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
import uk.gov.hmrc.statepension.StatePensionUnitSpec


class NpsNIRecordSpec extends StatePensionUnitSpec {

  val recordJson = Json.parse(
    """
      |{
      |  "years_to_fry": 3,
      |  "non_qualifying_years": 10,
      |  "date_of_entry": "1969-08-01",
      |  "npsLniemply": [],
      |  "pre_75_cc_count": 250,
      |  "number_of_qualifying_years": 36,
      |  "npsErrlist": {
      |    "count": 0,
      |    "mgt_check": 0,
      |    "commit_status": 2,
      |    "npsErritem": [],
      |    "bfm_return_code": 0,
      |    "data_not_found": 0
      |  },
      |  "non_qualifying_years_payable": 5,
      |  "npsLnitaxyr": [
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
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
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1976,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "932.1700",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "53.5000",
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 0,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 4
      |        },
      |        {
      |          "credit_source_type": 24,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 30
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1977,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "1433.0400",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "82.1300",
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 24,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 28
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1978,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "1069.2300",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "69.3500",
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 24,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 41
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1979,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "383.0800",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "24.9000",
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 24,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 42
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1980,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "1691.8500",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "114.1900",
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 24,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 35
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1981,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": null,
      |      "class_three_payable": null,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 24,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 52
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1982,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": null,
      |      "class_three_payable": null,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 24,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 52
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
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
      |      "rattd_tax_year": 1983,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "197.7800",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "17.7600",
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 24,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 44
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1984,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "2658.8900",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "239.2100",
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 24,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 19
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
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
      |      "rattd_tax_year": 1985,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": null,
      |      "class_three_payable": null,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 24,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 34
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
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
      |      "rattd_tax_year": 1986,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "720.8000",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "36.0400",
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 24,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 3
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1987,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": null,
      |      "class_three_payable": null,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 25,
      |          "cc_type": 24,
      |          "no_of_credits_and_conts": 52
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1988,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": null,
      |      "class_three_payable": null,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 25,
      |          "cc_type": 24,
      |          "no_of_credits_and_conts": 52
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1989,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "13624.0000",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "1149.9800",
      |      "npsLothcred": [],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1990,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "11180.0000",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "840.8400",
      |      "npsLothcred": [],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1991,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "14144.0000",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "1085.7600",
      |      "npsLothcred": [],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1992,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "19448.0000",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "1555.8400",
      |      "npsLothcred": [],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1993,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "13312.0000",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "996.3200",
      |      "npsLothcred": [],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1994,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "23452.0000",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "2094.0400",
      |      "npsLothcred": [],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
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
      |      "rattd_tax_year": 1995,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": null,
      |      "class_three_payable": null,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
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
      |      "rattd_tax_year": 1996,
      |      "ni_earnings": null,
      |      "amount_needed": "     ",
      |      "primary_paid_earnings": null,
      |      "class_three_payable": null,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1997,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "23452.0000",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "2094.0400",
      |      "npsLothcred": [],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1998,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "3912.0000",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "311.4400",
      |      "npsLothcred": [],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 1999,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "10311.0000",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "860.9900",
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 29,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 1
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 2000,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "13779.0000",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "1111.9500",
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 27,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 2
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": "2008-04-05",
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 0,
      |      "under_investigation_flag": 1,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 1,
      |      "rattd_tax_year": 2001,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "230.0000",
      |      "class_three_payable": 0.0,
      |      "ni_earnings_employed": "14.3000",
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 22,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 7
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 2002,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": null,
      |      "class_three_payable": null,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 22,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 52
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 2003,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": null,
      |      "class_three_payable": null,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 22,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 53
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 2004,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "30000.0000",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "2779.4800",
      |      "npsLothcred": [],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 2005,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": null,
      |      "class_three_payable": null,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 22,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 52
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 2006,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": null,
      |      "class_three_payable": null,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 22,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 52
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 2007,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": null,
      |      "class_three_payable": null,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 22,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 52
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": "2023-04-05",
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 0,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 1,
      |      "rattd_tax_year": 2008,
      |      "ni_earnings": null,
      |      "amount_needed": "675.7500",
      |      "primary_paid_earnings": null,
      |      "class_three_payable": 675.75,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 27,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 1
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": "2019-04-05",
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 2009,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": null,
      |      "class_three_payable": null,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 25,
      |          "cc_type": 24,
      |          "no_of_credits_and_conts": 52
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": "2023-04-05",
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 0,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 1,
      |      "rattd_tax_year": 2010,
      |      "ni_earnings": null,
      |      "amount_needed": "578.4000",
      |      "primary_paid_earnings": null,
      |      "class_three_payable": 578.4,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 27,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 4
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": "2019-04-05",
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": "2023-04-05",
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 0,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 1,
      |      "rattd_tax_year": 2011,
      |      "ni_earnings": null,
      |      "amount_needed": "655.2000",
      |      "primary_paid_earnings": null,
      |      "class_three_payable": 655.2,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": "2019-04-05",
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": "2023-04-05",
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 0,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 1,
      |      "rattd_tax_year": 2012,
      |      "ni_earnings": null,
      |      "amount_needed": "530.0000",
      |      "primary_paid_earnings": null,
      |      "class_three_payable": 530.0,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 27,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 12
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": "2019-04-05",
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 2013,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "28000.0000",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "2430.2400",
      |      "npsLothcred": [],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 2014,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": "28000.0000",
      |      "class_three_payable": null,
      |      "ni_earnings_employed": "2430.2400",
      |      "npsLothcred": [],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 2015,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": null,
      |      "class_three_payable": null,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 25,
      |          "cc_type": 24,
      |          "no_of_credits_and_conts": 52
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": "2023-04-05",
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 0,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 1,
      |      "rattd_tax_year": 2016,
      |      "ni_earnings": null,
      |      "amount_needed": "530.0000",
      |      "primary_paid_earnings": null,
      |      "class_three_payable": 530.0,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 27,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 12
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": "2019-04-05",
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": null,
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 1,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 0,
      |      "rattd_tax_year": 2017,
      |      "ni_earnings": null,
      |      "amount_needed": null,
      |      "primary_paid_earnings": null,
      |      "class_three_payable": null,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 25,
      |          "cc_type": 24,
      |          "no_of_credits_and_conts": 52
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": null,
      |      "ni_earnings_voluntary": null
      |    },
      |    {
      |      "class_three_payable_by_penalty": "2023-04-05",
      |      "class_two_outstanding_weeks": null,
      |      "class_two_payable": null,
      |      "qualifying": 0,
      |      "under_investigation_flag": 0,
      |      "class_two_payable_by": null,
      |      "co_class_one_paid": null,
      |      "class_two_payable_by_penalty": null,
      |      "co_primary_paid_earnings": null,
      |      "payable": 1,
      |      "rattd_tax_year": 2018,
      |      "ni_earnings": null,
      |      "amount_needed": "530.0000",
      |      "primary_paid_earnings": null,
      |      "class_three_payable": 530.0,
      |      "ni_earnings_employed": null,
      |      "npsLothcred": [
      |        {
      |          "credit_source_type": 27,
      |          "cc_type": 23,
      |          "no_of_credits_and_conts": 12
      |        }
      |      ],
      |      "ni_earnings_self_employed": null,
      |      "class_three_payable_by": "2019-04-05",
      |      "ni_earnings_voluntary": null
      |    }
      |  ],
      |  "nino": "QQ123456A"
      |}
    """.stripMargin).as[NpsNIRecord]

  "NpsNIRecord" should {

    "parse the tax years correctly" in {
      recordJson.taxYears shouldBe List(
        NpsNITaxYear(1975, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1976, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1977, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1978, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1979, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1980, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1981, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1982, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1983, qualifying = false, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1984, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1985, qualifying = false, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1986, qualifying = false, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1987, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1988, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1989, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1990, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1991, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1992, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1993, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1994, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1995, qualifying = false, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1996, qualifying = false, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1997, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1998, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(1999, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(2000, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(2001, qualifying = false, underInvestigation = true, payableFlag = true),
        NpsNITaxYear(2002, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(2003, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(2004, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(2005, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(2006, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(2007, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(2008, qualifying = false, underInvestigation = false, payableFlag = true),
        NpsNITaxYear(2009, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(2010, qualifying = false, underInvestigation = false, payableFlag = true),
        NpsNITaxYear(2011, qualifying = false, underInvestigation = false, payableFlag = true),
        NpsNITaxYear(2012, qualifying = false, underInvestigation = false, payableFlag = true),
        NpsNITaxYear(2013, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(2014, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(2015, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(2016, qualifying = false, underInvestigation = false, payableFlag = true),
        NpsNITaxYear(2017, qualifying = true, underInvestigation = false, payableFlag = false),
        NpsNITaxYear(2018, qualifying = false, underInvestigation = false, payableFlag = true)
      )
    }

    "parse qualifying years correctly (from json)" in {
      recordJson.qualifyingYears shouldBe 36
    }

    "parse payable gaps correctly (count, not read the non_qualifying_years_payable field)" in {
      recordJson.payableGapsPre2016 shouldBe 4
      recordJson.payableGapsPost2016 shouldBe 2
    }

    "parse qualifying years post 2016 correctly (count them)" in {
      recordJson.qualifyingYearsPost2016 shouldBe 1
    }

    "parse qualifying years pre 2016 correct (take post away from total)" in {
      recordJson.qualifyingYearsPre2016 shouldBe 35
    }

    "purge" should {
      "return an nirecord with no tax years after 2014 when the FRY 2014" in {
        val niRecord = NpsNIRecord(qualifyingYears = 6, List(
          NpsNITaxYear(2010, qualifying = true, underInvestigation = false, payableFlag = false),
          NpsNITaxYear(2011, qualifying = true, underInvestigation = false, payableFlag = false),
          NpsNITaxYear(2012, qualifying = true, underInvestigation = false, payableFlag = false),
          NpsNITaxYear(2013, qualifying = true, underInvestigation = false, payableFlag = false),
          NpsNITaxYear(2014, qualifying = true, underInvestigation = false, payableFlag = false),
          NpsNITaxYear(2015, qualifying = false, underInvestigation = false, payableFlag = true),
          NpsNITaxYear(2016, qualifying = true, underInvestigation = false, payableFlag = false)
        ))

        val purged = niRecord.purge(finalRelevantStartYear = 2014)

        purged.qualifyingYears shouldBe 5
        purged.payableGapsPre2016 shouldBe 0
        purged.qualifyingYearsPost2016 shouldBe 0
        purged.taxYears shouldBe List(
          NpsNITaxYear(2010, qualifying = true, underInvestigation = false, payableFlag = false),
          NpsNITaxYear(2011, qualifying = true, underInvestigation = false, payableFlag = false),
          NpsNITaxYear(2012, qualifying = true, underInvestigation = false, payableFlag = false),
          NpsNITaxYear(2013, qualifying = true, underInvestigation = false, payableFlag = false),
          NpsNITaxYear(2014, qualifying = true, underInvestigation = false, payableFlag = false)
        )
      }

      "return an nirecord with no tax years after 2015 when the FRY 2015" in {
        val niRecord = NpsNIRecord(qualifyingYears = 3, List(
          NpsNITaxYear(2010, qualifying = true, underInvestigation = false, payableFlag = false),
          NpsNITaxYear(2011, qualifying = false, underInvestigation = false, payableFlag = false),
          NpsNITaxYear(2012, qualifying = false, underInvestigation = false, payableFlag = true),
          NpsNITaxYear(2013, qualifying = true, underInvestigation = false, payableFlag = false),
          NpsNITaxYear(2014, qualifying = true, underInvestigation = false, payableFlag = false),
          NpsNITaxYear(2015, qualifying = false, underInvestigation = false, payableFlag = true),
          NpsNITaxYear(2016, qualifying = false, underInvestigation = false, payableFlag = true)
        ))

        val purged = niRecord.purge(finalRelevantStartYear = 2015)

        purged.qualifyingYears shouldBe 3
        purged.payableGapsPre2016 shouldBe 2
        purged.qualifyingYearsPost2016 shouldBe 0
        purged.taxYears shouldBe List(
          NpsNITaxYear(2010, qualifying = true, underInvestigation = false, payableFlag = false),
          NpsNITaxYear(2011, qualifying = false, underInvestigation = false, payableFlag = false),
          NpsNITaxYear(2012, qualifying = false, underInvestigation = false, payableFlag = true),
          NpsNITaxYear(2013, qualifying = true, underInvestigation = false, payableFlag = false),
          NpsNITaxYear(2014, qualifying = true, underInvestigation = false, payableFlag = false),
          NpsNITaxYear(2015, qualifying = false, underInvestigation = false, payableFlag = true)
        )
      }
    }

  }
}
