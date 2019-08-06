/*
 * Copyright 2019 HM Revenue & Customs
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

import com.codahale.metrics.Timer
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.ScalaFutures._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.Json
import play.api.{Configuration, Environment}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HttpGet, HttpResponse}
import uk.gov.hmrc.statepension.domain.nps._
import uk.gov.hmrc.statepension.services.ApplicationMetrics
import uk.gov.hmrc.statepension.{StatePensionUnitSpec, WSHttp}

import scala.language.postfixOps

class DesConnectorSpec extends StatePensionUnitSpec with MockitoSugar with OneAppPerSuite {

  val http: WSHttp = mock[WSHttp]
  val timerContext = mock[Timer.Context]
  val metrics: ApplicationMetrics = mock[ApplicationMetrics]
  val environment: Environment = app.injector.instanceOf[Environment]
  val configuration: Configuration = app.injector.instanceOf[Configuration]

  when(metrics.startTimer(Matchers.any())).thenReturn(timerContext)

  val nino: Nino = generateNino()
  val ninoWithSuffix: String = nino.toString().take(8)

  val jsonWithNoTaxYears =
    s"""
       |{
       |  "yearsToFry": 3,
       |  "nonQualifyingYears": 10,
       |  "dateOfEntry": "1969-08-01",
       |  "employmentDetails": [],
       |  "pre75CcCount": 250,
       |  "numberOfQualifyingYears": 36,
       |  "nonQualifyingYearsPayable": 5,
       |  "nino": "$nino"
       |}""".stripMargin

  val jsonNiRecord =
    s"""
       |{
       |  "yearsToFry": 3,
       |  "nonQualifyingYears": 10,
       |  "dateOfEntry": "1969-08-01",
       |  "employmentDetails": [],
       |  "pre75CcCount": 250,
       |  "numberOfQualifyingYears": 36,
       |  "nonQualifyingYearsPayable": 5,
       |  "taxYears": [
       |    {
       |      "classThreePayableByPenalty": null,
       |      "classTwoOutstandingWeeks": null,
       |      "classTwoPayable": null,
       |      "qualifying": true,
       |      "underInvestigationFlag": false,
       |      "classTwoPayableBy": null,
       |      "coClassOnePaid": null,
       |      "classTwoPayableByPenalty": null,
       |      "coPrimaryPaidEarnings": null,
       |      "payable": false,
       |      "rattdTaxYear": "1975",
       |      "niEarnings": null,
       |      "amountNeeded": null,
       |      "primaryPaidEarnings": "1285.4500",
       |      "classThreePayable": null,
       |      "niEarningsEmployed": "70.6700",
       |      "otherCredits": [
       |        {
       |          "creditSourceType": 0,
       |          "ccType": 23,
       |          "numberOfCredits": 20
       |        },
       |        {
       |          "creditSourceType": 24,
       |          "ccType": 23,
       |          "numberOfCredits": 6
       |        }
       |      ],
       |      "niEarningsSelfEmployed": null,
       |      "classThreePayableBy": null,
       |      "niEarningsVoluntary": null
       |    },
       |    {
       |      "classThreePayableByPenalty": null,
       |      "classTwoOutstandingWeeks": null,
       |      "classTwoPayable": null,
       |      "qualifying": true,
       |      "underInvestigationFlag": false,
       |      "classTwoPayableBy": null,
       |      "coClassOnePaid": null,
       |      "classTwoPayableByPenalty": null,
       |      "coPrimaryPaidEarnings": null,
       |      "payable": false,
       |      "rattdTaxYear": "1976",
       |      "niEarnings": null,
       |      "amountNeeded": null,
       |      "primaryPaidEarnings": "932.1700",
       |      "classThreePayable": null,
       |      "niEarningsEmployed": "53.5000",
       |      "otherCredits": [
       |        {
       |          "creditSourceType": 0,
       |          "ccType": 23,
       |          "numberOfCredits": 4
       |        },
       |        {
       |          "creditSourceType": 24,
       |          "ccType": 23,
       |          "numberOfCredits": 30
       |        }
       |      ],
       |      "niEarningsSelfEmployed": null,
       |      "classThreePayableBy": null,
       |      "niEarningsVoluntary": null
       |    },
       |    {
       |      "classThreePayableByPenalty": null,
       |      "classTwoOutstandingWeeks": null,
       |      "classTwoPayable": null,
       |      "qualifying": true,
       |      "underInvestigationFlag": false,
       |      "classTwoPayableBy": null,
       |      "coClassOnePaid": null,
       |      "classTwoPayableByPenalty": null,
       |      "coPrimaryPaidEarnings": null,
       |      "payable": false,
       |      "rattdTaxYear": "1977",
       |      "niEarnings": null,
       |      "amountNeeded": null,
       |      "primaryPaidEarnings": "1433.0400",
       |      "classThreePayable": null,
       |      "niEarningsEmployed": "82.1300",
       |      "otherCredits": [
       |        {
       |          "creditSourceType": 24,
       |          "ccType": 23,
       |          "numberOfCredits": 28
       |        }
       |      ],
       |      "niEarningsSelfEmployed": null,
       |      "classThreePayableBy": null,
       |      "niEarningsVoluntary": null
       |    }
       |  ],
       |  "nino": "$nino"
       |}""".stripMargin

  "getSummary" should {

    val connector = new DesConnector(http, metrics, environment, configuration) {

      override val desBaseUrl: String = "test-url"

      override val serviceOriginatorId: (String, String) = ("a_key", "a_value")

      override val token: String = "token"
    }

    when(http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
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
      verify(http, times(1)).GET[HttpResponse](Matchers.eq(s"test-url/individuals/$ninoWithSuffix/pensions/summary"))(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "add the originator id to the header" ignore {
      val header = headerCarrier
      verify(http, times(1)).GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.eq(header.copy(extraHeaders = Seq("a_key" -> "a_value"))), Matchers.any())
    }

    "parse the json and return a Future[DesSummary]" in {

      when(http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
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
      when(http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
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
    val connector = new DesConnector(http, metrics, environment, configuration) {

      override val desBaseUrl: String = "test-url"

      override val token: String = "token"

      override val desEnvironment: (String, String) = ("environment", "unit test")

      override val serviceOriginatorId: (String, String) = ("a_key", "a_value")
    }

    when(http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
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
      verify(http, times(1)).GET[HttpResponse](Matchers.eq(s"test-url/individuals/$ninoWithSuffix/pensions/liabilities"))(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "add the originator id to the header" ignore {
      val header = headerCarrier
      verify(http, times(1)).GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.eq(header.copy(extraHeaders = Seq("a_key" -> "a_value"))), Matchers.any())
    }

    "parse the json and return a Future[List[DesLiability]" in {

      when(http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
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

      val summary = await(connector.getLiabilities(nino))
      summary shouldBe List(
        DesLiability(Some(13)),
        DesLiability(Some(13)),
        DesLiability(Some(13)),
        DesLiability(Some(13))
      )
    }

    "return a failed future with a json validation exception when it cannot parse to an DesLiabilities" in {
      val connector = new DesConnector(http, metrics, environment, configuration) {

        override val desBaseUrl: String = "test-url"

        override val token: String = "token"

        override val desEnvironment: (String, String) = ("environment", "unit test")

        override val serviceOriginatorId: (String, String) = ("a_key", "a_value")
      }

      when(http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
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
        ex.getMessage shouldBe "/liabilities(0)/liabilityType - error.expected.jsnumber"
      }
    }
  }

  "return a future with a json list for empty DesLiability" in {
    val connector = new DesConnector(http, metrics, environment, configuration) {

      override val desBaseUrl: String = "test-url"

      override val token: String = "token"

      override val desEnvironment: (String, String) = ("environment", "unit test")

      override val serviceOriginatorId: (String, String) = ("a_key", "a_value")
    }

    when(http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
      s"""
         |{
         |  "liabilities": [
         |    {
         |      }
         |      ]
         |}
      """.stripMargin))))

    ScalaFutures.whenReady(connector.getLiabilities(nino)) { ldl =>
      ldl.length shouldBe 0
    }
  }


  "getNIRecord" should {
    val mockHttpGet = mock[HttpGet]
    val connector = new DesConnector(http, metrics, environment, configuration) {

      override val desBaseUrl: String = "test-url"

      override val token: String = "token"

      override val desEnvironment: (String, String) = ("environment", "unit test")

      override val serviceOriginatorId: (String, String) = ("a_key", "a_value")
    }

    when(http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(jsonNiRecord))))
    connector.getNIRecord(nino)


    "make an http request to hod-url/individuals/ninoWithoutSuffix/pensions/ni" in {
      verify(http, times(1)).GET[HttpResponse](Matchers.eq(s"test-url/individuals/$ninoWithSuffix/pensions/ni"))(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "add the originator id to the header" ignore {
      val header = headerCarrier
      connector.getNIRecord(nino)
      verify(http, times(1)).GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.eq(header.copy(extraHeaders = Seq("a_key" -> "a_value"))), Matchers.any())
    }

    "parse the json and return a Future[DesNIRecord]" in {

      when(http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(jsonNiRecord))))
      connector.getNIRecord(nino)

      val summary = await(connector.getNIRecord(nino))
      summary shouldBe DesNIRecord(
        qualifyingYears = 36,
        List(
          DesNITaxYear(Some(1975), qualifying = Some(true), underInvestigation = Some(false), payableFlag = Some(false)),
          DesNITaxYear(Some(1976), qualifying = Some(true), underInvestigation = Some(false), payableFlag = Some(false)),
          DesNITaxYear(Some(1977), qualifying = Some(true), underInvestigation = Some(false), payableFlag = Some(false))
        ))
    }

    "parse the json and return a Future[DesNIRecord] when tax years are not present" in {
      val optJson = Some(Json.parse(jsonWithNoTaxYears))
      when(http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, optJson))

      val summary = await(connector.getNIRecord(nino))
      summary shouldBe DesNIRecord(qualifyingYears = 36, List.empty)
    }

    "return a failed future with a json validation exception when it cannot parse to an DesNIRecord" in {

      
      
      
      val connector = new DesConnector(http, metrics, environment, configuration) {
        
        override val desBaseUrl: String = "test-url"

        override val token: String = "token"

        override val desEnvironment: (String, String) = ("environment", "unit test")

        override val serviceOriginatorId: (String, String) = ("a_key", "a_value")
        
      }

      when(http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
        s"""
           |{
           |  "yearsToFry": 3,
           |  "nonQualifyingYears": 10,
           |  "dateOfEntry": "1969-08-01",
           |  "employmentDetails": [],
           |  "pre75CcCount": 250,
           |  "numberOfQualifyingYears": "36",
           |  "nonQualifyingYearsPayable": "5",
           |  "nino": "$nino"
           |}
      """.stripMargin))))

      ScalaFutures.whenReady(connector.getNIRecord(nino).failed) { ex =>
        ex shouldBe a[connector.JsonValidationException]
        ex.getMessage shouldBe "/numberOfQualifyingYears - error.expected.jsnumber"
      }
    }


    "return a failed future with a json validation exception when it cannot parse to an DesNIRecord when taxyear is invalid" in {
      val connector = new DesConnector(http, metrics, environment, configuration) {

        override val desBaseUrl: String = "test-url"

        override val token: String = "token"

        override val desEnvironment: (String, String) = ("environment", "unit test")

        override val serviceOriginatorId: (String, String) = ("a_key", "a_value")
      }

      when(http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(HttpResponse(200, Some(Json.parse(
        s"""
           |{
           |  "yearsToFry": 3,
           |  "nonQualifyingYears": 10,
           |  "dateOfEntry": "1969-08-01",
           |  "employmentDetails": [],
           |  "pre75CcCount": 250,
           |  "numberOfQualifyingYears": 36,
           |  "nonQualifyingYearsPayable": 5,
           |  "taxYears": [
           |    {
           |      "classThreePayableByPenalty": null,
           |      "classTwoOutstandingWeeks": null,
           |      "classTwoPayable": null,
           |      "qualifying": true,
           |      "underInvestigationFlag": false,
           |      "classTwoPayableBy": null,
           |      "coClassOnePaid": null,
           |      "classTwoPayableByPenalty": null,
           |      "coPrimaryPaidEarnings": null,
           |      "payable": false,
           |      "rattdTaxYear": "not a real year",
           |      "niEarnings": null,
           |      "amountNeeded": null,
           |      "primaryPaidEarnings": "1285.4500",
           |      "classThreePayable": null,
           |      "niEarningsEmployed": "70.6700",
           |      "otherCredits": [
           |        {
           |          "creditSourceType": 0,
           |          "ccType": 23,
           |          "numberOfCredits": 20
           |        },
           |        {
           |          "creditSourceType": 24,
           |          "ccType": 23,
           |          "numberOfCredits": 6
           |        }
           |      ],
           |      "niEarningsSelfEmployed": null,
           |      "classThreePayableBy": null,
           |      "niEarningsVoluntary": null
           |    }]
           |}
      """.stripMargin))))

      ScalaFutures.whenReady(connector.getNIRecord(nino).failed) { ex =>
        ex shouldBe a[Exception]
        ex.getMessage shouldBe "/rattdTaxYear is not a valid integer"
      }
    }
  }







}
