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

package uk.gov.hmrc.statepension.connectors

import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.{HttpGet, HttpResponse}
import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.domain.nps.{NpsAmountA2016, NpsAmountB2016, NpsStatePensionAmounts, NpsSummary}

class NpsConnectorSpec extends StatePensionUnitSpec with MockitoSugar {

  val connector = new NpsConnector {
    override def getLiabilities = ???

    override def getNIRecord = ???

    override val http = mock[HttpGet]

    override def npsBaseUrl: String = "test-url"

    override val serviceOriginatorId: (String, String) = ("a_key", "a_value")
  }

  val nino: Nino = generateNino()
  val ninoWithSuffix: String = nino.toString().take(7)

  "getSummary" should {
    when(connector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
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
      verify(connector.http, times(1)).GET[HttpResponse](Matchers.eq(s"test-url/nps-rest-service/services/nps/pensions/$ninoWithSuffix/sp_summary"))(Matchers.any(), Matchers.any())
    }

    "add the originator id to the header" in {
      val header = headerCarrier
      verify(connector.http, times(1)).GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.eq(header.copy(extraHeaders = Seq("a_key" -> "a_value"))))
    }

    "parse the json and return a Future[NpsSummary]" in {
      val summary = await(connector.getSummary(nino))
      summary shouldBe NpsSummary(
        new LocalDate(2016, 4, 5),
        "M",
        qualifyingYears = 36,
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
          additionalPensionAccruedLastTaxYear = 2.36,
          NpsAmountA2016(
            basicPension = 119.30,
            pre97AP = 17.79,
            post97AP = 6.03,
            post02AP = 15.4,
            pre88GMP = 0,
            post88GMP = 0,
            pre88COD = 0,
            post88COD = 0,
            grb = 2.66
          ),
          NpsAmountB2016(
            mainComponent = 155.65,
            rebateDerivedAmount = 0
          )
        )
      )
    }

    "return a failed future with a json validation exception when it cannot parse to an NpsSummary" in {
      when(connector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
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
}
