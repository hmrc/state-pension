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

package uk.gov.hmrc.statepension.connectors

import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.domain.nps._
import uk.gov.hmrc.statepension.helpers.StubMetrics
import uk.gov.hmrc.statepension.services.Metrics
import uk.gov.hmrc.http.{ HttpGet, HttpResponse }

class NpsConnectorSpec extends StatePensionUnitSpec with MockitoSugar {

  val nino: Nino = generateNino()
  val ninoWithSuffix: String = nino.toString().take(8)

  "getSummary" should {
    val connector = new NpsConnector {
      override val http = mock[HttpGet]

      override def npsBaseUrl: String = "test-url"

      override val serviceOriginatorId: (String, String) = ("a_key", "a_value")
      override val metrics: Metrics = StubMetrics
    }

    when(connector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
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
        |  "earnings_included_upto": "2016-04-05"
        |}
      """.stripMargin))))

    connector.getSummary(nino)

    "make an http request to hod-url/nps-rest-service/services/nps/pensions/ninoWithoutSuffix/sp_summary" in {
      verify(connector.http, times(1)).GET[HttpResponse](Matchers.eq(s"test-url/nps-rest-service/services/nps/pensions/$ninoWithSuffix/sp_summary"))(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "add the originator id to the header" in {
      val header = headerCarrier
      verify(connector.http, times(1)).GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.eq(header.copy(extraHeaders = Seq("a_key" -> "a_value"))), Matchers.any())
    }

    "parse the json and return a Future[NpsSummary]" in {
      val summary = await(connector.getSummary(nino))
      summary shouldBe NpsSummary(
        new LocalDate(2016, 4, 5),
        "M",
        statePensionAgeDate = new LocalDate(2019, 9, 6),
        finalRelevantStartYear = 2018,
        pensionSharingOrderSERPS = true,
        dateOfBirth = new LocalDate(1954, 3, 9),
        None,
        reducedRateElection = false,
        countryCode = 1,
        NpsStatePensionAmounts(
          pensionEntitlement = 161.18,
          startingAmount2016 = 161.18,
          protectedPayment2016 = 5.53,
          NpsAmountA2016(
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
          NpsAmountB2016(
            mainComponent = 155.65,
            rebateDerivedAmount = 0
          )
        )
      )
    }

    "return a failed future with a json validation exception when it cannot parse to an NpsSummary" in {
      when(connector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
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
          |      "ltb_post88_cod_cash_value": false,
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
          |  "address_postcode": "WS9 8LL"
          |}
        """.stripMargin))))

      ScalaFutures.whenReady(connector.getSummary(nino).failed) { ex =>
        ex shouldBe a[connector.JsonValidationException]
        ex.getMessage shouldBe "/earnings_included_upto - error.path.missing | /npsSpnam/npsAmnapr16/ltb_post88_cod_cash_value - error.expected.jsnumberorjsstring"
      }
    }
  }

  "getLiabilities" should {
    val connector = new NpsConnector {
      override val http = mock[HttpGet]

      override def npsBaseUrl: String = "test-url"

      override val serviceOriginatorId: (String, String) = ("a_key", "a_value")
      override val metrics: Metrics = StubMetrics
    }

    when(connector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
      s"""
         |{
         |  "npsErrlist": {
         |    "count": 0,
         |    "mgt_check": 0,
         |    "commit_status": 2,
         |    "npsErritem": [],
         |    "bfm_return_code": 0,
         |    "data_not_found": 0
         |  },
         |  "npsLcdo004d": [
         |    {
         |      "liability_type_end_date": "1992-11-21",
         |      "liability_occurrence_no": 1,
         |      "liability_type_start_date": "1983-11-06",
         |      "liability_type_end_date_reason": "END DATE HELD",
         |      "liability_type": 13,
         |      "nino": "$nino",
         |      "award_amount": null
         |    },
         |    {
         |      "liability_type_end_date": "2006-07-08",
         |      "liability_occurrence_no": 2,
         |      "liability_type_start_date": "1995-09-24",
         |      "liability_type_end_date_reason": "END DATE HELD",
         |      "liability_type": 13,
         |      "nino": "$nino",
         |      "award_amount": null
         |    },
         |    {
         |      "liability_type_end_date": "2006-07-15",
         |      "liability_occurrence_no": 3,
         |      "liability_type_start_date": "2006-07-09",
         |      "liability_type_end_date_reason": "END DATE HELD",
         |      "liability_type": 13,
         |      "nino": "$nino",
         |      "award_amount": null
         |    },
         |    {
         |      "liability_type_end_date": "2012-01-21",
         |      "liability_occurrence_no": 4,
         |      "liability_type_start_date": "2006-09-24",
         |      "liability_type_end_date_reason": "END DATE HELD",
         |      "liability_type": 13,
         |      "nino": "$nino",
         |      "award_amount": null
         |    }
         |  ]
         |}
      """.stripMargin))))

    connector.getLiabilities(nino)

    "make an http request to hod-url/nps-rest-service/services/nps/pensions/ninoWithoutSuffix/liabilities" in {
      verify(connector.http, times(1)).GET[HttpResponse](Matchers.eq(s"test-url/nps-rest-service/services/nps/pensions/$ninoWithSuffix/liabilities"))(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "add the originator id to the header" in {
      val header = headerCarrier
      verify(connector.http, times(1)).GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.eq(header.copy(extraHeaders = Seq("a_key" -> "a_value"))), Matchers.any())
    }

    "parse the json and return a Future[List[NpsLiability]" in {
      val summary = await(connector.getLiabilities(nino))
      summary shouldBe List(
        NpsLiability(13),
        NpsLiability(13),
        NpsLiability(13),
        NpsLiability(13)
      )
    }

    "return a failed future with a json validation exception when it cannot parse to an NpsLiabilities" in {
      val connector = new NpsConnector {
        override val http = mock[HttpGet]

        override def npsBaseUrl: String = "test-url"

        override val serviceOriginatorId: (String, String) = ("a_key", "a_value")
        override val metrics: Metrics = StubMetrics
      }

      when(connector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
        s"""
           |{
           |  "npsErrlist": {
           |    "count": 0,
           |    "mgt_check": 0,
           |    "commit_status": 2,
           |    "npsErritem": [],
           |    "bfm_return_code": 0,
           |    "data_not_found": 0
           |  },
           |  "npsLcdo004d": [
           |    {
           |      "liability_type_end_date": "1992-11-21",
           |      "liability_occurrence_no": 1,
           |      "liability_type_start_date": "1983-11-06",
           |      "liability_type_end_date_reason": "END DATE HELD",
           |      "liability_type": false,
           |      "nino": "$nino",
           |      "award_amount": null
           |    },
           |    {
           |      "liability_type_end_date": "2006-07-08",
           |      "liability_occurrence_no": 2,
           |      "liability_type_start_date": "1995-09-24",
           |      "liability_type_end_date_reason": "END DATE HELD",
           |      "liability_type": 13,
           |      "nino": "$nino",
           |      "award_amount": null
           |    },
           |    {
           |      "liability_type_end_date": "2006-07-15",
           |      "liability_occurrence_no": 3,
           |      "nino": "$nino",
           |      "award_amount": null
           |    },
           |    {
           |      "liability_type_end_date": "2012-01-21",
           |      "liability_occurrence_no": 4,
           |      "liability_type_start_date": "2006-09-24",
           |      "liability_type_end_date_reason": "END DATE HELD",
           |      "liability_type": 13,
           |      "nino": "$nino",
           |      "award_amount": null
           |    }
           |  ]
           |}
      """.stripMargin))))

      ScalaFutures.whenReady(connector.getLiabilities(nino).failed) { ex =>
        ex shouldBe a[connector.JsonValidationException]
        ex.getMessage shouldBe "/npsLcdo004d(0)/liability_type - error.expected.jsnumber | /npsLcdo004d(2)/liability_type - error.path.missing"
      }
    }
  }

  "getNIRecord" should {
    val connector = new NpsConnector {
      override val http = mock[HttpGet]

      override def npsBaseUrl: String = "test-url"

      override val serviceOriginatorId: (String, String) = ("a_key", "a_value")
      override val metrics: Metrics = StubMetrics
    }

    when(connector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
      s"""
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
         |    }
         |  ],
         |  "nino": "$nino"
         |}""".stripMargin))))

    connector.getNIRecord(nino)

    "make an http request to hod-url/nps-rest-service/services/nps/pensions/ninoWithoutSuffix/ni_record" in {
      verify(connector.http, times(1)).GET[HttpResponse](Matchers.eq(s"test-url/nps-rest-service/services/nps/pensions/$ninoWithSuffix/ni_record"))(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "add the originator id to the header" in {
      val header = headerCarrier
      verify(connector.http, times(1)).GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.eq(header.copy(extraHeaders = Seq("a_key" -> "a_value"))), Matchers.any())
    }

    "parse the json and return a Future[NpsNIRecord]" in {
      val summary = await(connector.getNIRecord(nino))
      summary shouldBe NpsNIRecord(
        qualifyingYears = 36,
        List(
          NpsNITaxYear(1975, qualifying = true, underInvestigation = false, payableFlag = false),
          NpsNITaxYear(1976, qualifying = true, underInvestigation = false, payableFlag = false),
          NpsNITaxYear(1977, qualifying = true, underInvestigation = false, payableFlag = false)
        ))
    }

    "return a failed future with a json validation exception when it cannot parse to an NpsNIRecord" in {
      val connector = new NpsConnector {

        override val http = mock[HttpGet]

        override def npsBaseUrl: String = "test-url"

        override val serviceOriginatorId: (String, String) = ("a_key", "a_value")

        override def metrics: Metrics = StubMetrics
      }

      when(connector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
        s"""
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
           |  "non_qualifying_years_payable": "5",
           |  "nino": "$nino"
           |}
      """.stripMargin))))

      ScalaFutures.whenReady(connector.getNIRecord(nino).failed) { ex =>
        ex shouldBe a[connector.JsonValidationException]
        ex.getMessage shouldBe "/npsLnitaxyr - error.path.missing"
      }
    }
  }
}
