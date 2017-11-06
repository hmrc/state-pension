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
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.test.WithFakeApplication
import uk.gov.hmrc.statepension.StatePensionUnitSpec
import uk.gov.hmrc.statepension.domain._
import uk.gov.hmrc.statepension.connectors.NispConnector.JsonValidationException

import scala.concurrent.Future
import uk.gov.hmrc.http.{ BadRequestException, HeaderCarrier, HttpGet, HttpResponse }

class NispConnectorSpec extends StatePensionUnitSpec with MockitoSugar with WithFakeApplication {

  val testNispConnector = new NispConnector {
    override def nispBaseUrl: String = ""
    override val http: HttpGet = mock[HttpGet]
  }

  implicit val dummyHeaderCarrier = HeaderCarrier()

  "NispConnector" should {
    "return Left(Exclusion) when there is Exclusion JSON" in {
      when(testNispConnector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(
        200,
        Some(Json.parse(
          """
            |{
            |  "exclusionReasons": [
            |    "AmountDissonance",
            |    "Dead"
            |  ],
            |  "pensionAge": 65,
            |  "pensionDate": "2018-07-05"
            |}
          """.stripMargin
        ))
      )))

      await(testNispConnector.getStatePension(generateNino())) shouldBe Left(StatePensionExclusion(
        List(
          Exclusion.AmountDissonance,
          Exclusion.Dead
        ),
        pensionAge = 65,
        pensionDate = new LocalDate(2018, 7, 5)
      ))
    }

    "return Right(StatePension) when there is StatePensionJson" in {
      when(testNispConnector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(
        200,
        Some(Json.parse(
          """
            |{
            |  "earningsIncludedUpTo": "2015-04-05",
            |  "amounts": {
            |    "protectedPayment": false,
            |    "current": {
            |      "weeklyAmount": 123.65,
            |      "monthlyAmount": 537.66,
            |      "annualAmount": 6451.88
            |    },
            |    "forecast": {
            |      "yearsToWork": 4,
            |      "weeklyAmount": 151.25,
            |      "monthlyAmount": 657.67,
            |      "annualAmount": 7892.01
            |    },
            |    "maximum": {
            |      "yearsToWork": 4,
            |      "gapsToFill": 1,
            |      "weeklyAmount": 155.65,
            |      "monthlyAmount": 676.80,
            |      "annualAmount": 8121.59
            |    },
            |    "cope": {
            |      "weeklyAmount": 0.25,
            |      "monthlyAmount": 1.09,
            |      "annualAmount": 13.04
            |    },
            |    "starting": {
            |      "weeklyAmount": 161.18,
            |      "monthlyAmount": 700.85,
            |      "annualAmount": 8410.14
            |    },
            |    "oldRules": {
            |      "basicStatePension" : 119.30,
            |      "additionalStatePension": 39.22,
            |      "graduatedRetirementBenefit":2.66
            |    },
            |    "newRules": {
            |      "grossStatePension" : 155.40,
            |      "rebateDerivedAmount": 0.25
            |    }
            |  },
            |  "pensionAge": 67,
            |  "pensionDate": "2019-07-01",
            |  "finalRelevantYear": "2018-19",
            |  "numberOfQualifyingYears": 30,
            |  "pensionSharingOrder": false,
            |  "currentFullWeeklyPensionAmount": 155.65,
            |  "reducedRateElection": false
            |}
          """.stripMargin
        ))
      )))

      val responseF = testNispConnector.getStatePension(generateNino())
      await(testNispConnector.getStatePension(generateNino())) shouldBe Right(StatePension(
        new LocalDate(2015, 4, 5),
        StatePensionAmounts(
          protectedPayment = false,
          StatePensionAmount(None, None, 123.65),
          StatePensionAmount(Some(4), None, 151.25),
          StatePensionAmount(Some(4), Some(1), 155.65),
          StatePensionAmount(None, None, 0.25),
          StatePensionAmount(None, None, 161.18),
          OldRules(basicStatePension = 119.30,
                   additionalStatePension=39.22,
                   graduatedRetirementBenefit = 2.66),
          NewRules(grossStatePension = 155.40,
            rebateDerivedAmount=0.25)
        ),
        67,
        new LocalDate(2019, 7 ,1),
        "2018-19",
        30,
        pensionSharingOrder = false,
        155.65,
        false,
        None
      ))
    }

    "return a failed future when it cannot parse to Either an exclusion or statement and report validation errors" in {
      when(testNispConnector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(
        200,
        Some(Json.parse(
          """
            |{
            |  "earningsIncludedUpTo": "2015-04-05",
            |  "amounts": {
            |    "protectedPayment": false,
            |    "iamcorruptbutvalid": true
            |   }
            |}
          """.stripMargin
        ))
      )))

      ScalaFutures.whenReady(testNispConnector.getStatePension(generateNino()).failed) { ex =>
        ex shouldBe a [JsonValidationException]
        ex.getMessage.contains("JSON Validation Error:") shouldBe true
      }
    }

    "return a failed future when there is an http error and pass on the exception" in {
      when(testNispConnector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.failed(new BadRequestException("You want me to do what?")))
      ScalaFutures.whenReady(testNispConnector.getStatePension(generateNino()).failed) { ex =>
        ex shouldBe a [BadRequestException]
      }
    }
  }

}
