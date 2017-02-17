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

import org.joda.time.LocalDate
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec


class NpsSummarySpec extends UnitSpec {
  val summaryJson = Json.parse(
    """
      |{
      |  "contracted_out_flag": 0,
      |  "sensitive_flag": 0,
      |  "spa_date": "2019-09-06",
      |  "final_relevant_year": 2018,
      |  "account_not_maintained_flag": null,
      |  "npsPenfor": {
      |    "forecast_amount": 160.19,
      |    "nsp_max": 155.65,
      |    "qualifying_years_at_spa": 40,
      |    "forecast_amount_2016": 160.19
      |  },
      |  "pension_share_order_coeg": 0,
      |  "date_of_death": null,
      |  "sex": "M",
      |  "npsSpnam": {
      |    "nsp_entitlement": 161.18,
      |    "ap_amount": 2.36,
      |    "npsAmnbpr16": {
      |      "main_component": 155.65,
      |      "rebate_derived_amount": 0.0
      |    },
      |    "npsAmnapr16": {
      |      "ltb_post97_ap_cash_value": 6.03,
      |      "ltb_cat_a_cash_value": 119.3,
      |      "ltb_post88_cod_cash_value": null,
      |      "ltb_pre97_ap_cash_value": 17.79,
      |      "ltb_pre88_cod_cash_value": null,
      |      "grb_cash": 2.66,
      |      "ltb_pst88_gmp_cash_value": null,
      |      "pre88_gmp": null,
      |      "ltb_post02_ap_cash_value": 15.4
      |    },
      |    "protected_payment_2016": 5.53,
      |    "starting_amount": 161.18
      |  },
      |  "npsErrlist": {
      |    "count": 0,
      |    "mgt_check": 0,
      |    "commit_status": 2,
      |    "npsErritem": [],
      |    "bfm_return_code": 0,
      |    "data_not_found": 0
      |  },
      |  "date_of_birth": "1954-03-09",
      |  "nsp_qualifying_years": 36,
      |  "country_code": 1,
      |  "nsp_requisite_years": 35,
      |  "minimum_qualifying_period": 1,
      |  "address_postcode": "WS9 8LL",
      |  "rre_to_consider": 0,
      |  "pension_share_order_serps": 1,
      |  "nino": "QQ123456A",
      |  "earnings_included_upto": "2015-04-05"
      |}
    """.stripMargin)

  "NpsSummary" should {
    "parse earnings included up to correctly" in {
      summaryJson.as[NpsSummary].earningsIncludedUpTo shouldBe new LocalDate(2015, 4, 5)
    }
    "parse sex correctly" in {
      summaryJson.as[NpsSummary].sex shouldBe "M"
    }
    "parse qualifying_years correctly" in {
      summaryJson.as[NpsSummary].qualifyingYears shouldBe 36
    }

    "parse state pension age date correctly" when {
      "it exists as 2019-09-06" in {
        summaryJson.as[NpsSummary].statePensionAgeDate shouldBe new LocalDate(2019, 9, 6)
      }
    }

    "parse final relevant start year correctly" when {
      "it exists as 2019-09-06" in {
        summaryJson.as[NpsSummary].finalRelevantStartYear shouldBe 2018
      }
    }

    "parse pension sharing order correctly" when {
      "it exists as true" in {
        summaryJson.as[NpsSummary].pensionSharingOrderSERPS shouldBe true
      }
    }

    "parse date of birth correctly" when {
      "it exists as 1954-03-09" in {
        summaryJson.as[NpsSummary].dateOfBirth shouldBe new LocalDate(1954, 3, 9)
      }
    }

    "parse date of death correctly" when {

      "it is null" in {
        summaryJson.as[NpsSummary].dateOfDeath shouldBe None
      }

      "it exists as 2001-9-11" in {
        Json.parse(
          """
            |{
            |  "contracted_out_flag": 0,
            |  "sensitive_flag": 0,
            |  "spa_date": "2019-09-06",
            |  "final_relevant_year": 2018,
            |  "account_not_maintained_flag": null,
            |  "npsPenfor": {
            |    "forecast_amount": 160.19,
            |    "nsp_max": 155.65,
            |    "qualifying_years_at_spa": 40,
            |    "forecast_amount_2016": 160.19
            |  },
            |  "pension_share_order_coeg": 0,
            |  "date_of_death": "2000-09-13",
            |  "sex": "M",
            |  "npsSpnam": {
            |    "nsp_entitlement": 161.18,
            |    "ap_amount": 2.36,
            |    "npsAmnbpr16": {
            |      "main_component": 155.65,
            |      "rebate_derived_amount": 0.0
            |    },
            |    "npsAmnapr16": {
            |      "ltb_post97_ap_cash_value": 6.03,
            |      "ltb_cat_a_cash_value": 119.3,
            |      "ltb_post88_cod_cash_value": null,
            |      "ltb_pre97_ap_cash_value": 17.79,
            |      "ltb_pre88_cod_cash_value": null,
            |      "grb_cash": 2.66,
            |      "ltb_pst88_gmp_cash_value": null,
            |      "pre88_gmp": null,
            |      "ltb_post02_ap_cash_value": 15.4
            |    },
            |    "protected_payment_2016": 5.53,
            |    "starting_amount": 161.18
            |  },
            |  "npsErrlist": {
            |    "count": 0,
            |    "mgt_check": 0,
            |    "commit_status": 2,
            |    "npsErritem": [],
            |    "bfm_return_code": 0,
            |    "data_not_found": 0
            |  },
            |  "date_of_birth": "1954-03-09",
            |  "nsp_qualifying_years": 36,
            |  "country_code": 1,
            |  "nsp_requisite_years": 35,
            |  "minimum_qualifying_period": 1,
            |  "address_postcode": "WS9 8LL",
            |  "rre_to_consider": 0,
            |  "pension_share_order_serps": 1,
            |  "nino": "QQ123456A",
            |  "earnings_included_upto": "2015-04-05"
            |}
          """.stripMargin).as[NpsSummary].dateOfDeath shouldBe Some(new LocalDate(2000, 9, 13))
      }

    }


    "parse reduced rate election correctly" when {
      "it is 0 and therefore false" in {
        summaryJson.as[NpsSummary].reducedRateElection shouldBe false
      }
      "it is higher and therefore true" in {
        Json.parse(
          """
            |{
            |  "contracted_out_flag": 0,
            |  "sensitive_flag": 0,
            |  "spa_date": "2019-09-06",
            |  "final_relevant_year": 2018,
            |  "account_not_maintained_flag": null,
            |  "npsPenfor": {
            |    "forecast_amount": 160.19,
            |    "nsp_max": 155.65,
            |    "qualifying_years_at_spa": 40,
            |    "forecast_amount_2016": 160.19
            |  },
            |  "pension_share_order_coeg": 0,
            |  "date_of_death": null,
            |  "sex": "M",
            |  "npsSpnam": {
            |    "nsp_entitlement": 161.18,
            |    "ap_amount": 2.36,
            |    "npsAmnbpr16": {
            |      "main_component": 155.65,
            |      "rebate_derived_amount": 0.0
            |    },
            |    "npsAmnapr16": {
            |      "ltb_post97_ap_cash_value": 6.03,
            |      "ltb_cat_a_cash_value": 119.3,
            |      "ltb_post88_cod_cash_value": null,
            |      "ltb_pre97_ap_cash_value": 17.79,
            |      "ltb_pre88_cod_cash_value": null,
            |      "grb_cash": 2.66,
            |      "ltb_pst88_gmp_cash_value": null,
            |      "pre88_gmp": null,
            |      "ltb_post02_ap_cash_value": 15.4
            |    },
            |    "protected_payment_2016": 5.53,
            |    "starting_amount": 161.18
            |  },
            |  "npsErrlist": {
            |    "count": 0,
            |    "mgt_check": 0,
            |    "commit_status": 2,
            |    "npsErritem": [],
            |    "bfm_return_code": 0,
            |    "data_not_found": 0
            |  },
            |  "date_of_birth": "1954-03-09",
            |  "nsp_qualifying_years": 36,
            |  "country_code": 1,
            |  "nsp_requisite_years": 35,
            |  "minimum_qualifying_period": 1,
            |  "address_postcode": "WS9 8LL",
            |  "rre_to_consider": 1,
            |  "pension_share_order_serps": 1,
            |  "nino": "QQ123456A",
            |  "earnings_included_upto": "2015-04-05"
            |}
          """.stripMargin).as[NpsSummary].reducedRateElection shouldBe true
      }
    }

    "parse the country code correctly" when {
      "it exists as 1" in {
        summaryJson.as[NpsSummary].countryCode shouldBe 1
      }
    }

    "parse the amounts correctly" in {

      summaryJson.as[NpsSummary].amounts shouldBe NpsStatePensionAmounts(
        pensionEntitlement = 161.18,
        startingAmount2016 = 161.18,
        protectedPayment2016 = 5.53,
        additionalPensionAccruedLastTaxYear = 2.36,
        NpsAmountA2016(
          basicPension = 119.3,
          pre97AP = 17.79,
          post97AP = 6.03,
          post02AP = 15.4,
          pre88GMP = 0,
          post88GMP = 0,
          pre88COD = 0,
          post88COD = 0,
          grb =  2.66
        ),
        NpsAmountB2016(
          mainComponent = 155.65,
          rebateDerivedAmount = 0
        )
      )
    }

  }

  "NpsAmounts" should {

      val nullAmountJson = Json.parse("""{
                                        |    "nsp_entitlement": null,
                                        |    "ap_amount": null,
                                        |    "npsAmnbpr16": {
                                        |      "main_component": null,
                                        |      "rebate_derived_amount": null
                                        |    },
                                        |    "npsAmnapr16": {
                                        |      "ltb_post97_ap_cash_value": null,
                                        |      "ltb_cat_a_cash_value": 0,
                                        |      "ltb_post88_cod_cash_value": null,
                                        |      "ltb_pre97_ap_cash_value": null,
                                        |      "ltb_pre88_cod_cash_value": null,
                                        |      "grb_cash": 0,
                                        |      "ltb_pst88_gmp_cash_value": null,
                                        |      "pre88_gmp": null,
                                        |      "ltb_post02_ap_cash_value": null
                                        |    },
                                        |    "protected_payment_2016": null,
                                        |    "starting_amount": null
                                        |}""".stripMargin)

    val amountJson = Json.parse("""{
                                      |    "nsp_entitlement": 100.10,
                                      |    "ap_amount": 11.11,
                                      |    "npsAmnbpr16": {
                                      |      "main_component": 12.12,
                                      |      "rebate_derived_amount": 13.13
                                      |    },
                                      |    "npsAmnapr16": {
                                      |      "ltb_post97_ap_cash_value": 20.20,
                                      |      "ltb_cat_a_cash_value": 21.21,
                                      |      "ltb_post88_cod_cash_value": 22.22,
                                      |      "ltb_pre97_ap_cash_value": 23.23,
                                      |      "ltb_pre88_cod_cash_value": 24.24,
                                      |      "grb_cash": 25.25,
                                      |      "ltb_pst88_gmp_cash_value": 26.26,
                                      |      "pre88_gmp": 27.27,
                                      |      "ltb_post02_ap_cash_value": 28.28
                                      |    },
                                      |    "protected_payment_2016": 29.29,
                                      |    "starting_amount": 300.00
                                      |}""".stripMargin)

      "parse 2016 starting amount correctly" when {
        "it exists as 300.00" in {
          amountJson.as[NpsStatePensionAmounts].startingAmount2016 shouldBe 300.00
        }

        "it is null" in {
          nullAmountJson.as[NpsStatePensionAmounts].startingAmount2016 shouldBe 0
        }
      }

      "parse nsp entitlement correctly" when {
        "it exists as 100.10" in {
          amountJson.as[NpsStatePensionAmounts].pensionEntitlement shouldBe 100.10
        }

        "it is null" in {
          nullAmountJson.as[NpsStatePensionAmounts].pensionEntitlement shouldBe 0
        }
      }

      "parse 2016 protected payment correctly" when {
        "it exists as 29.29" in {
          amountJson.as[NpsStatePensionAmounts].protectedPayment2016 shouldBe 29.29
        }

        "it is null" in {
          nullAmountJson.as[NpsStatePensionAmounts].protectedPayment2016 shouldBe 0
        }
      }

      "parse ap amount correctly" when {
        "it exists as 11.11" in {
          amountJson.as[NpsStatePensionAmounts].additionalPensionAccruedLastTaxYear shouldBe 11.11
        }

        "it is null" in {
          nullAmountJson.as[NpsStatePensionAmounts].additionalPensionAccruedLastTaxYear shouldBe 0
        }
      }

      "be parsing Amount A correctly and therefore" should {


        "parse basic pension correctly" when {
          "it exists as 21.21" in {
            amountJson.as[NpsStatePensionAmounts].amountA2016.basicPension shouldBe 21.21
          }

          "it is null" in {
            nullAmountJson.as[NpsStatePensionAmounts].amountA2016.basicPension shouldBe 0
          }
        }

        "parse pre 97 additional pension correctly" when {
          "it exists as 23.23" in {
            amountJson.as[NpsStatePensionAmounts].amountA2016.pre97AP shouldBe 23.23
          }

          "it is null" in {
            nullAmountJson.as[NpsStatePensionAmounts].amountA2016.pre97AP shouldBe 0
          }
        }

        "parse post 97 additional pension correctly" when {
          "it exists as 20.20" in {
            amountJson.as[NpsStatePensionAmounts].amountA2016.post97AP shouldBe 20.20
          }

          "it is null" in {
            nullAmountJson.as[NpsStatePensionAmounts].amountA2016.post97AP shouldBe 0
          }
        }

        "parse post 02 additional pension correctly" when {
          "it exists as 28.28" in {
            amountJson.as[NpsStatePensionAmounts].amountA2016.post02AP shouldBe 28.28
          }

          "it is null" in {
            nullAmountJson.as[NpsStatePensionAmounts].amountA2016.post02AP shouldBe 0
          }
        }

        "parse pre 88 GMP  correctly" when {
          "it exists as 27.27" in {
            amountJson.as[NpsStatePensionAmounts].amountA2016.pre88GMP shouldBe 27.27
          }

          "it is null" in {
            nullAmountJson.as[NpsStatePensionAmounts].amountA2016.pre88GMP shouldBe 0
          }
        }

        "parse post 88 GMP  correctly" when {
          "it exists as 26.26" in {
            amountJson.as[NpsStatePensionAmounts].amountA2016.post88GMP shouldBe 26.26
          }

          "it is null" in {
            nullAmountJson.as[NpsStatePensionAmounts].amountA2016.post88GMP shouldBe 0
          }
        }

        "parse pre 88 COD correctly" when {
          "it exists as  24.24" in {
            amountJson.as[NpsStatePensionAmounts].amountA2016.pre88COD shouldBe  24.24
          }

          "it is null" in {
            nullAmountJson.as[NpsStatePensionAmounts].amountA2016.pre88COD shouldBe 0
          }
        }

        "parse post 88 COD correctly" when {
          "it exists as  22.22" in {
            amountJson.as[NpsStatePensionAmounts].amountA2016.post88COD shouldBe  22.22
          }

          "it is null" in {
            nullAmountJson.as[NpsStatePensionAmounts].amountA2016.post88COD shouldBe 0
          }
        }
        "parse graduated retirement benefit" when {
          "it exists as  25.25" in {
            amountJson.as[NpsStatePensionAmounts].amountA2016.grb shouldBe  25.25
          }

          "it is null" in {
            nullAmountJson.as[NpsStatePensionAmounts].amountA2016.grb shouldBe 0
          }
        }

      }

      "be parsing Amount B correctly and therefore" should {
        "parse main component correctly" when {
          "it exists as 12.12" in {
            amountJson.as[NpsStatePensionAmounts].amountB2016.mainComponent shouldBe 12.12
          }

          "it is null" in {
            nullAmountJson.as[NpsStatePensionAmounts].amountB2016.mainComponent shouldBe 0
          }
        }

        "parse rebate derived amount correctly" when {
          "it exists as 13.13" in {
            amountJson.as[NpsStatePensionAmounts].amountB2016.rebateDerivedAmount shouldBe 13.13
          }

          "it is null" in {
            nullAmountJson.as[NpsStatePensionAmounts].amountB2016.rebateDerivedAmount shouldBe 0
          }
        }
      }


  }

  "NpsAmountA2016" should {
    "Total AP Calculation" should {
      "return £4321 for pre97 AP 1, post97 AP 20, post02 AP 300, grb 4000" in {
        NpsAmountA2016(115.95, 1, 20, 300, 0, 0, 0, 0, 4000).totalAP shouldBe 4321
      }

      "return £4320 for pre97 AP 1, post97 AP 20, post02 AP 300, grb 4000, pre88 gmp 1" in {
        NpsAmountA2016(115.95, 1, 20, 300, 1, 0, 0, 0, 4000).totalAP shouldBe 4320
      }

      "return £4320 for pre97 AP 1, post97 AP 20, post02 AP 300, grb 4000, pre88 gmp 2" in {
        NpsAmountA2016(115.95, 1, 20, 300, 2, 0, 0, 0, 4000).totalAP shouldBe 4320
      }

      "return £4320 for pre97 AP 1, post97 AP 20, post02 AP 300, grb 4000, post88 gmp 1" in {
        NpsAmountA2016(115.95, 1, 20, 300, 0, 1, 0, 0, 4000).totalAP shouldBe 4320
      }

      "return £4320 for pre97 AP 1, post97 AP 20, post02 AP 300, grb 4000, pre88 cod 1" in {
        NpsAmountA2016(115.95, 1, 20, 300, 0, 0, 1, 0, 4000).totalAP shouldBe 4320
      }

      "return £4320 for pre97 AP 1, post97 AP 20, post02 AP 300, grb 4000, post88 cod 1" in {
        NpsAmountA2016(115.95, 1, 20, 300, 0, 0, 0, 1, 4000).totalAP shouldBe 4320
      }

      "return £4320 for pre97 AP 1, post97 AP 20, post02 AP 300, grb 4000, post88 cod 1, pre88 cod 1" in {
        NpsAmountA2016(115.95, 1, 20, 300, 0, 0, 1, 1, 4000).totalAP shouldBe 4320
      }

      "return £4319 for pre97 AP 1, post97 AP 19, post02 AP 300, grb 4000, post88 cod 1, pre88 cod 1" in {
        NpsAmountA2016(115.95, 1, 19, 300, 0, 0, 1, 1, 4000).totalAP shouldBe 4319
      }

      "return £4319 for pre97 AP 1, post97 AP 19, post02 AP 300, grb 4000, post88 cod 2, pre88 cod 1" in {
        NpsAmountA2016(115.95, 1, 19, 300, 0, 0, 2, 1, 4000).totalAP shouldBe 4319
      }
    }
  }

  "finalRelevantYear" when {

    def summaryWithFinalRelevantStartYear(finalRelevantStartYear: Int) = NpsSummary(
      earningsIncludedUpTo = new LocalDate(2016, 4, 5),
      sex = "F",
      qualifyingYears = 20,
      statePensionAgeDate = new LocalDate(2020, 6, 9),
      finalRelevantStartYear = finalRelevantStartYear,
      pensionSharingOrderSERPS = false,
      dateOfBirth = new LocalDate(1960, 4, 5)
    )


    "finalRelevantStartYear is 2015" should {
      "return 2015-16" in {
        summaryWithFinalRelevantStartYear(2015).finalRelevantYear shouldBe "2015-16"
      }
    }

    "finalRelevantStartYear is 1999" should {
      "return 1999-99" in {
        summaryWithFinalRelevantStartYear(1999).finalRelevantYear shouldBe "1999-00"
      }
    }

    "finalRelevantStartYear is 2009" should {
      "return 2009-10" in {
        summaryWithFinalRelevantStartYear(2009).finalRelevantYear shouldBe "2009-10"
      }
    }

    "finalRelevantStartYear is 1975" should {
      "return 1975-76" in {
        summaryWithFinalRelevantStartYear(1975).finalRelevantYear shouldBe "1975-76"
      }
    }
  }

  "statePensionAge" when {

    def summaryWithDOBandSPA(dateOfBirth: LocalDate, statePensionAgeDate: LocalDate) = NpsSummary(
      earningsIncludedUpTo = new LocalDate(2016, 4, 5),
      sex = "F",
      qualifyingYears = 20,
      statePensionAgeDate = statePensionAgeDate,
      finalRelevantStartYear = 2020,
      pensionSharingOrderSERPS = false,
      dateOfBirth
    )

    "when the date of birth is 1950-1-1 and the state pension age date is 2017-1-1" should {
      "return 67" in {
        summaryWithDOBandSPA(new LocalDate(1950, 1, 1), new LocalDate(2017, 1, 1)).statePensionAge shouldBe 67
      }
    }

    "when the date of birth is 1950-1-1 and the state pension age date is 2017-1-2" should {
      "return 67" in {
        summaryWithDOBandSPA(new LocalDate(1950, 1, 1), new LocalDate(2017, 1, 2)).statePensionAge shouldBe 67
      }
    }

    "when the date of birth is 1950-1-1 and the state pension age date is 2017-5-2" should {
      "return 67" in {
        summaryWithDOBandSPA(new LocalDate(1950, 1, 1), new LocalDate(2017, 5, 2)).statePensionAge shouldBe 67
      }
    }

    "when the date of birth is 2017-1-2 and the state pension age date is 2017-1-1"  should {
      "return 66" in {
        summaryWithDOBandSPA(new LocalDate(1950, 1, 2), new LocalDate(2017, 1, 1)).statePensionAge shouldBe 66
      }
    }
  }
}
