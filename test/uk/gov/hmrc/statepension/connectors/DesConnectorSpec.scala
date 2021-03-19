/*
 * Copyright 2021 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.joda.time.LocalDate
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.Matchers._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, RequestId, SessionId}
import uk.gov.hmrc.statepension.domain.nps._
import uk.gov.hmrc.statepension.fixtures.NIRecordFixture
import uk.gov.hmrc.statepension.services.ApplicationMetrics
import uk.gov.hmrc.statepension.{StatePensionBaseSpec, WireMockHelper}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

class DesConnectorSpec extends StatePensionBaseSpec
  with GuiceOneAppPerSuite
  with MockitoSugar
  with ScalaFutures
  with IntegrationPatience
  with WireMockHelper {

  val mockMetrics: ApplicationMetrics = mock[ApplicationMetrics](Mockito.RETURNS_DEEP_STUBS)
  val nino: Nino = generateNino()
  val ninoWithoutSuffix: String = nino.withoutSuffix

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("testSessionId")),
    requestId = Some(RequestId("testRequestId")))

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure("microservice.services.des-hod.port" -> server.port(),
                      "microservice.services.des-hod.token" -> "testToken",
                      "microservice.services.des-hod.originatoridkey" -> "testOriginatorKey",
                      "microservice.services.des-hod.originatoridvalue" -> "testOriginatorId",
                      "microservice.services.des-hod.environment" -> "testEnvironment",
                      "api.access.whitelist.applicationIds.0" -> "abcdefg-12345-abddefg-12345",
                      "api.access.type" -> "PRIVATE",
                      "cope.dwp.originatorId" -> "dwpId"
    )
    .overrides(
      bind[ApplicationMetrics].toInstance(mockMetrics)
    ).build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockMetrics)
  }

  lazy val desConnector = app.injector.instanceOf[DesConnector]

  "getSummary" should {

    val summaryUrl: String = s"/individuals/${nino.withoutSuffix}/pensions/summary"

    "return a Summary object when successful" in {
      val expectedJson: JsValue = Json.parse(
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
      """.stripMargin)

      server.stubFor(
        get(urlEqualTo(summaryUrl)).willReturn(ok().withBody(expectedJson.toString()))
      )

      val expectedPensionAmounts = PensionAmounts(
        pensionEntitlement = 161.18,
        startingAmount2016 = 161.18,
        protectedPayment2016 = 5.53,
        amountA2016 = AmountA2016(119.3,17.79,6.03,15.4,0,0,0,0,2.66),
        amountB2016 = AmountB2016(155.65,0))

      val expectedSummary =  Summary(
        earningsIncludedUpTo = new LocalDate(2016, 4,5),
        statePensionAgeDate = new LocalDate(2019, 9, 6),
        finalRelevantStartYear = 2018,
        pensionSharingOrderSERPS = true,
        dateOfBirth = new LocalDate(1954, 3, 9),
        dateOfDeath = None,
        countryCode = 1,
        amounts = expectedPensionAmounts,
        manualCorrespondenceIndicator = None
      )

      val response: Summary = desConnector.getSummary(nino).futureValue
      response shouldBe expectedSummary

      server.verify(getRequestedFor(urlEqualTo(summaryUrl))
        .withHeader("Authorization", equalTo("Bearer testToken"))
        .withHeader("testOriginatorKey", equalTo("testOriginatorId"))
        .withHeader("Environment", equalTo("testEnvironment"))
        .withHeader("X-Request-ID", equalTo("testRequestId"))
        .withHeader("X-Session-ID", equalTo("testSessionId"))
      )

      withClue("timer did not stop") {
        Mockito.verify(mockMetrics.startTimer(ArgumentMatchers.eq(APIType.Summary))).stop()
      }
    }

    "return a failed future with a json validation exception when it cannot parse to an NpsSummary" in {
        val invalidJson = Json.parse(
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
        """.stripMargin)

      server.stubFor(
        get(urlEqualTo(summaryUrl)).willReturn(ok().withBody(invalidJson.toString()))
      )

      val exception = intercept[desConnector.JsonValidationException]{
        desConnector.getSummary(nino).futureValue
      }

      exception.getMessage shouldBe "/earningsIncludedUpto - error.path.missing | /statePensionAmount/amountA2016/ltbPost88CodCashValue - error.expected.jsnumberorjsstring"

      withClue("timer did not stop") {
        Mockito.verify(mockMetrics.startTimer(ArgumentMatchers.eq(APIType.Summary))).stop()
      }
    }
  }

  "getLiabilities" should {

    val liabilitiesUrl = s"/individuals/${nino.withoutSuffix}/pensions/liabilities"
    "return a list of liabilities when successful" in {

      val expectedJson: JsValue = Json.parse(
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
      """.stripMargin)
      server.stubFor(
        get(urlEqualTo(liabilitiesUrl)).willReturn(ok().withBody(expectedJson.toString()))
      )
      val response: List[Liability] = desConnector.getLiabilities(nino).futureValue
      val liabilityType = 13
      response shouldBe List(
        Liability(Some(liabilityType)),
        Liability(Some(liabilityType)),
        Liability(Some(liabilityType)),
        Liability(Some(liabilityType))
      )
      server.verify(getRequestedFor(urlEqualTo(liabilitiesUrl))
        .withHeader("Authorization", equalTo("Bearer testToken"))
        .withHeader("testOriginatorKey", equalTo("testOriginatorId"))
        .withHeader("Environment", equalTo("testEnvironment"))
        .withHeader("X-Request-ID", equalTo("testRequestId"))
        .withHeader("X-Session-ID", equalTo("testSessionId"))
      )
    }

    "return a failed future with a json validation exception when it cannot parse to an DesLiabilities" in {
      val invalidJson: JsValue = Json.parse(
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
      """.stripMargin)

      server.stubFor(
        get(urlEqualTo(liabilitiesUrl)).willReturn(ok().withBody(invalidJson.toString()))
      )

      val exception = intercept[desConnector.JsonValidationException] {
        desConnector.getLiabilities(nino).futureValue
      }
      exception.getMessage shouldBe "/liabilities(0)/liabilityType - error.expected.jsnumber"
    }
  }

  "getNIRecord" should {

    val jsonNiRecord: JsValue = Json.parse(
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
         |}""".stripMargin)

    val niRecordUrl = s"/individuals/${nino.withoutSuffix}/pensions/ni"

    "return an NiRecord object when successful" in {

     server.stubFor(
       get(urlEqualTo(niRecordUrl)).willReturn(ok().withBody(jsonNiRecord.toString()))
     )

     val expectedNiRecord = NIRecord(
       qualifyingYears = 36,
       List(
         NITaxYear(Some(1975), qualifying = Some(true), underInvestigation = Some(false), payableFlag = Some(false)),
         NITaxYear(Some(1976), qualifying = Some(true), underInvestigation = Some(false), payableFlag = Some(false)),
         NITaxYear(Some(1977), qualifying = Some(true), underInvestigation = Some(false), payableFlag = Some(false))
       ))

     val response: NIRecord = desConnector.getNIRecord(nino).futureValue
     response shouldBe expectedNiRecord

     server.verify(getRequestedFor(urlEqualTo(niRecordUrl))
       .withHeader("Authorization", equalTo("Bearer testToken"))
       .withHeader("testOriginatorKey", equalTo("testOriginatorId"))
       .withHeader("Environment", equalTo("testEnvironment"))
       .withHeader("X-Request-ID", equalTo("testRequestId"))
       .withHeader("X-Session-ID", equalTo("testSessionId"))
     )

      withClue("timer did not stop") {
        Mockito.verify(mockMetrics.startTimer(ArgumentMatchers.eq(APIType.NIRecord))).stop()
      }

    }

   "parse the json and return a Future[DesNIRecord] when tax years are not present" in {
     val jsonWithNoTaxYears: String = NIRecordFixture.exampleDesNiRecordJson(nino.nino)
     val optJson: JsValue = Json.parse(jsonWithNoTaxYears)

     server.stubFor(
       get(urlEqualTo(niRecordUrl)).willReturn(ok().withBody(optJson.toString()))
     )

     val response: NIRecord = desConnector.getNIRecord(nino).futureValue
     response shouldBe NIRecord(qualifyingYears = 36, List.empty)

     withClue("timer did not stop") {
       Mockito.verify(mockMetrics.startTimer(ArgumentMatchers.eq(APIType.NIRecord))).stop()
     }
   }

    "return a failed future with a json validation exception when it cannot parse to a DesNIRecord" in {

      val invalidJson = Json.parse(
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
      """.stripMargin)

      server.stubFor(
        get(urlEqualTo(niRecordUrl)).willReturn(ok().withBody(invalidJson.toString()))
      )

      val exception = intercept[desConnector.JsonValidationException]{
        desConnector.getNIRecord(nino).futureValue
      }

      exception.getMessage shouldBe "/numberOfQualifyingYears - error.expected.jsnumber"

      withClue("timer did not stop") {
        Mockito.verify(mockMetrics.startTimer(ArgumentMatchers.eq(APIType.NIRecord))).stop()
      }
    }

    "return the correct headers for requests made by the DWP" in {
      val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("testSessionId")),
        requestId = Some(RequestId("testRequestId")))
        .withExtraHeaders(("x-application-id", "abcdefg-12345-abddefg-12345"))
      val requestBody: String = "{}"

      server.stubFor(
        get(urlEqualTo(niRecordUrl)).willReturn(ok().withBody(requestBody))
      )

      desConnector.getNIRecord(nino)(hc).futureValue

      server.verify(
        getRequestedFor(urlEqualTo(niRecordUrl))
          .withHeader("testOriginatorKey", equalTo("dwpId"))
      )
    }

  }
}
