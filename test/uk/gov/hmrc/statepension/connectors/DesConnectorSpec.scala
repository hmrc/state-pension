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
import uk.gov.hmrc.http.{HttpGet, HttpResponse}
import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.domain.nps._
import uk.gov.hmrc.statepension.helpers.StubMetrics
import uk.gov.hmrc.statepension.services.Metrics

class DesConnectorSpec extends StatePensionUnitSpec with MockitoSugar {

  val nino: Nino = generateNino()
  val ninoWithSuffix: String = nino.toString().take(8)

  "getSummary" should {
    val connector = new DesConnector {
      override val http = mock[HttpGet]

      override def desBaseUrl: String = "test-url"

      override val serviceOriginatorId: (String, String) = ("a_key", "a_value")
      override val metrics: Metrics = StubMetrics

      override def token: String = "token"
      override def environment: (String, String) = ("environment", "unit test")
    }

    when(connector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
      """
        |{
        |  "contractedOutFlag": 0,
        |  "sensitiveCaseFlag": 0,
        |  "spaDate": "2019-09-06",
        |  "finalRelevantYear": 2018,
        |  "accountNotMaintainedFlag": null,
        |  "penForecast": {
        |    "forecastAmount": 160.19,
        |    "nspMax": 155.65,
        |    "qualifyingYearsAtSpa": 40,
        |    "forecastAmount2016": 160.19
        |  },
        |  "pensionShareOrderCoeg": false,
        |  "dateOfDeath": null,
        |  "sex": "M",
        |  "statePensionAmount": {
        |    "nspEntitlement": 161.18,
        |    "apAmount": 2.36,
        |    "amountB2016": {
        |      "mainComponent": 155.65,
        |      "rebateDerivedAmount": 0.0
        |    },
        |    "amountA2016": {
        |      "ltbPost97ApCashValue": 6.03,
        |      "ltbCatACashValue": 119.3,
        |      "ltbPost88CodCashValue": null,
        |      "ltbPre97ApCashValue": 17.79,
        |      "ltbPre88CodCashValue": null,
        |      "grbCash": 2.66,
        |      "ltbPst88GmpCashValue": null,
        |      "pre88Gmp": null,
        |      "ltbPost02ApCashValue": 15.4
        |    },
        |    "protectedPayment2016": 5.53,
        |    "startingAmount": 161.18
        |  },
        |  "dateOfBirth": "1954-03-09",
        |  "nspQualifyingYears": 36,
        |  "countryCode": 1,
        |  "nspRequisiteYears": 35,
        |  "minimumQualifyingPeriod": 1,
        |  "addressPostcode": "WS9 8LL",
        |  "reducedRateElectionToConsider": false,
        |  "pensionShareOrderSerps": true,
        |  "nino": "QQ123456A",
        |  "earningsIncludedUpto": "2016-04-05"
        |}
      """.stripMargin))))

    connector.getSummary(nino)

    "make an http request to hod-url/nps-rest-service/services/nps/pensions/ninoWithoutSuffix/sp_summary" in {
      verify(connector.http, times(1)).GET[HttpResponse](Matchers.eq(s"test-url/individuals/$ninoWithSuffix/pensions/summary"))(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "add the originator id to the header" ignore {
      val header = headerCarrier
      verify(connector.http, times(1)).GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.eq(header.copy(extraHeaders = Seq("a_key" -> "a_value"))), Matchers.any())
    }

    "parse the json and return a Future[DesSummary]" in {
      val summary = await(connector.getSummary(nino))
      summary shouldBe DesSummary(
        new LocalDate(2016, 4, 5),
        "M",
        statePensionAgeDate = new LocalDate(2019, 9, 6),
        finalRelevantStartYear = 2018,
        pensionSharingOrderSERPS = true,
        dateOfBirth = new LocalDate(1954, 3, 9),
        None,
        reducedRateElection = false,
        countryCode = 1,
        DesStatePensionAmounts(
          pensionEntitlement = 161.18,
          startingAmount2016 = 161.18,
          protectedPayment2016 = 5.53,
          DesAmountA2016(
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
          DesAmountB2016(
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
          |  "contractedOutFlag": 0,
          |  "sensitiveCaseFlag": 0,
          |  "spaDate": "2019-09-06",
          |  "finalRelevantYear": 2018,
          |  "accountNotMaintainedFlag": null,
          |  "penForecast": {
          |    "forecastAmount": 160.19,
          |    "nspMax": 155.65,
          |    "qualifyingYearsAtSpa": 40,
          |    "forecastAmount2016": 160.19
          |  },
          |  "pensionShareOrderCoeg": false,
          |  "dateOfDeath": null,
          |  "sex": "M",
          |  "statePensionAmount": {
          |    "nspEntitlement": 161.18,
          |    "apAmount": 2.36,
          |    "amountB2016": {
          |      "mainComponent": 155.65,
          |      "rebateDerivedAmount": 0.0
          |    },
          |    "amountA2016": {
          |      "ltbPost97ApCashValue": 6.03,
          |      "ltbCatACashValue": 119.3,
          |      "ltbPost88CodCashValue": false,
          |      "ltbPre97ApCashValue": 17.79,
          |      "ltbPre88CodCashValue": null,
          |      "grbCash": 2.66,
          |      "ltbPst88GmpCashValue": null,
          |      "pre88Gmp": null,
          |      "ltbPost02ApCashValue": 15.4
          |    },
          |    "protectedPayment2016": 5.53,
          |    "startingAmount": 161.18
          |  },
          |  "dateOfBirth": "1954-03-09",
          |  "nspQualifyingYears": 36,
          |  "countryCode": 1,
          |  "nspRequisiteYears": 35,
          |  "minimumQualifyingPeriod": 1,
          |  "addressPostcode": "WS9 8LL"
          |}
        """.stripMargin))))

      ScalaFutures.whenReady(connector.getSummary(nino).failed) { ex =>
        ex shouldBe a[connector.JsonValidationException]
        ex.getMessage shouldBe "/earningsIncludedUpto - error.path.missing | /statePensionAmount/amountA2016/ltbPost88CodCashValue - error.expected.jsnumberorjsstring"
      }
    }
  }

  "getLiabilities" should {
    val connector = new DesConnector {
      override val http = mock[HttpGet]

      override def desBaseUrl: String = "test-url"
      override def token: String = "token"
      override def environment: (String, String) = ("environment", "unit test")
      override val serviceOriginatorId: (String, String) = ("a_key", "a_value")
      override val metrics: Metrics = StubMetrics
    }

    when(connector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
      s"""
         |{
         |  "liabilities": [
         |    {
         |      "liabilityTypeEndDate": "1992-11-21",
         |      "liabilityOccurrenceNo": 1,
         |      "liabilityTypeStartDate": "1983-11-06",
         |      "liabilityTypeEndDateReason": "END DATE HELD",
         |      "liabilityType": 13,
         |      "nino": "$nino",
         |      "awardAmount": null
         |    },
         |    {
         |      "liabilityTypeEndDate": "2006-07-08",
         |      "liabilityOccurrenceNo": 2,
         |      "liabilityTypeStartDate": "1995-09-24",
         |      "liabilityTypeEndDateReason": "END DATE HELD",
         |      "liabilityType": 13,
         |      "nino": "$nino",
         |      "awardAmount": null
         |    },
         |    {
         |      "liabilityTypeEndDate": "2006-07-15",
         |      "liabilityOccurrenceNo": 3,
         |      "liabilityTypeStartDate": "2006-07-09",
         |      "liabilityTypeEndDateReason": "END DATE HELD",
         |      "liabilityType": 13,
         |      "nino": "$nino",
         |      "awardAmount": null
         |    },
         |    {
         |      "liabilityTypeEndDate": "2012-01-21",
         |      "liabilityOccurrenceNo": 4,
         |      "liabilityTypeStartDate": "2006-09-24",
         |      "liabilityTypeEndDateReason": "END DATE HELD",
         |      "liabilityType": 13,
         |      "nino": "$nino",
         |      "awardAmount": null
         |    }
         |  ]
         |}
      """.stripMargin))))

    connector.getLiabilities(nino)

    "make an http request to hod-url/individuals/ninoWithoutSuffix/pensions/liabilities" in {
      verify(connector.http, times(1)).GET[HttpResponse](Matchers.eq(s"test-url/individuals/$ninoWithSuffix/pensions/liabilities"))(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "add the originator id to the header" ignore {
      val header = headerCarrier
      verify(connector.http, times(1)).GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.eq(header.copy(extraHeaders = Seq("a_key" -> "a_value"))), Matchers.any())
    }

    "parse the json and return a Future[List[DesLiability]" in {
      val summary = await(connector.getLiabilities(nino))
      summary shouldBe List(
        DesLiability(13),
        DesLiability(13),
        DesLiability(13),
        DesLiability(13)
      )
    }

    "return a failed future with a json validation exception when it cannot parse to an DesLiabilities" in {
      val connector = new DesConnector {
        override val http = mock[HttpGet]

        override def desBaseUrl: String = "test-url"
        override def token: String = "token"
        override def environment: (String, String) = ("environment", "unit test")
        override val serviceOriginatorId: (String, String) = ("a_key", "a_value")
        override val metrics: Metrics = StubMetrics
      }

      when(connector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
        s"""
           |{
           |  "liabilities": [
           |    {
           |      "liabilityTypeEndDate": "1992-11-21",
           |      "liabilityOccurrenceNo": 1,
           |      "liabilityTypeStartDate": "1983-11-06",
           |      "liabilityTypeEndDateReason": "END DATE HELD",
           |      "liabilityType": false,
           |      "nino": "$nino",
           |      "awardAmount": null
           |    },
           |    {
           |      "liabilityTypeEndDate": "2006-07-08",
           |      "liabilityOccurrenceNo": 2,
           |      "liabilityTypeStartDate": "1995-09-24",
           |      "liabilityTypeEndDateReason": "END DATE HELD",
           |      "liabilityType": 13,
           |      "nino": "$nino",
           |      "awardAmount": null
           |    },
           |    {
           |      "liabilityTypeEndDate": "2006-07-15",
           |      "liabilityOccurrenceNo": 3,
           |      "nino": "$nino",
           |      "awardAmount": null
           |    },
           |    {
           |      "liabilityTypeEndDate": "2012-01-21",
           |      "liabilityOccurrenceNo": 4,
           |      "liabilityTypeStartDate": "2006-09-24",
           |      "liabilityTypeEndDateReason": "END DATE HELD",
           |      "liabilityType": 13,
           |      "nino": "$nino",
           |      "awardAmount": null
           |    }
           |  ]
           |}
      """.stripMargin))))

      ScalaFutures.whenReady(connector.getLiabilities(nino).failed) { ex =>
        ex shouldBe a[connector.JsonValidationException]
        ex.getMessage shouldBe "/liabilities(0)/liabilityType - error.expected.jsnumber | /liabilities(2)/liabilityType - error.path.missing"
      }
    }
  }

  "getNIRecord" should {
    val connector = new DesConnector {
      override val http = mock[HttpGet]

      override def desBaseUrl: String = "test-url"
      override def token: String = "token"
      override def environment: (String, String) = ("environment", "unit test")
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

    "add the originator id to the header" ignore {
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
      val connector = new DesConnector {

        override val http = mock[HttpGet]

        override def desBaseUrl: String = "test-url"
        override def token: String = "token"
        override def environment: (String, String) = ("environment", "unit test")
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
