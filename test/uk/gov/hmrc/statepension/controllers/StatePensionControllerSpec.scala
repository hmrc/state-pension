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

package uk.gov.hmrc.statepension.controllers

import org.joda.time.LocalDate
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers, Injecting}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.statepension.config.AppContext
import uk.gov.hmrc.statepension.controllers.auth.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.statepension.controllers.statepension.StatePensionController
import uk.gov.hmrc.statepension.domain._
import uk.gov.hmrc.statepension.services.StatePensionService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random
import play.api.libs.json.JodaReads._


class StatePensionControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with Injecting {

  val nino: Nino = new Generator(new Random()).nextNino

  val controllerComponents = Helpers.stubControllerComponents()
  val emptyRequest = FakeRequest()
  val emptyRequestWithHeader = FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")

  val _appContext: AppContext = inject[AppContext]
  val fakeAuthAction: AuthAction = inject[FakeAuthAction]

  def testStatePensionController(spService: StatePensionService): StatePensionController =
    new StatePensionController(controllerComponents) {
      override val app: String = "Test State Pension"
      override lazy val context: String = "test"
      override val appContext: AppContext = _appContext
      override val statePensionService: StatePensionService = spService
      override val customAuditConnector: AuditConnector = mock[AuditConnector]
      override val authAction: AuthAction = fakeAuthAction
      override def endpointUrl(nino: Nino): String = s"/ni/$nino"
      override val executionContext: ExecutionContext = controllerComponents.executionContext
      override val parser = controllerComponents.parsers.default
    }

  val testStatePension = StatePension(
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
        graduatedRetirementBenefit=2.66),
      NewRules(grossStatePension=155.40,
        rebateDerivedAmount= 0.25
      )
    ),
    67,
    new LocalDate(2019, 7 ,1),
    "2018-19",
    30,
    pensionSharingOrder = false,
    155.65,
    false,
    None,
    false
  )

  "get" should {
    "return status code 406 when the headers are invalid" in {
      val mockStatePensionService = mock[StatePensionService]

      when(mockStatePensionService.getStatement(any())(any()))
        .thenReturn(Right(testStatePension))

      val response = testStatePensionController(mockStatePensionService).get(nino)(emptyRequest)

      status(response) shouldBe 406
      contentAsJson(response) shouldBe Json.parse("""{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}""")
    }

    "return 200 with a Response" in {
      val mockStatePensionService = mock[StatePensionService]

      when(mockStatePensionService.getStatement(any())(any()))
        .thenReturn(Right(testStatePension))

      val response = testStatePensionController(mockStatePensionService).get(nino)(emptyRequestWithHeader)

      status(response) shouldBe 200
      val json = contentAsJson(response)
      (json \ "earningsIncludedUpTo").as[LocalDate] shouldBe new LocalDate(2015, 4, 5)
      (json \ "amounts" \ "protectedPayment").as[Boolean] shouldBe false
      (json \ "amounts" \ "maximum" \ "yearsToWork").as[Int] shouldBe 4
      (json \ "amounts" \ "maximum" \ "gapsToFill").as[Int] shouldBe 1
      (json \ "amounts" \ "maximum" \ "weeklyAmount").as[BigDecimal] shouldBe 155.65
      (json \ "amounts" \ "maximum" \ "monthlyAmount").as[BigDecimal] shouldBe 676.80
      (json \ "amounts" \ "maximum" \ "annualAmount").as[BigDecimal] shouldBe 8121.59
      (json \ "amounts" \ "starting" \ "weeklyAmount").as[BigDecimal] shouldBe 161.18
      (json \ "amounts" \ "starting" \ "monthlyAmount").as[BigDecimal] shouldBe 700.85
      (json \ "amounts" \ "starting" \ "annualAmount").as[BigDecimal] shouldBe 8410.14
      (json \ "amounts" \ "oldRules" \ "basicStatePension").as[BigDecimal] shouldBe 119.30
      (json \ "amounts" \ "oldRules" \ "additionalStatePension").as[BigDecimal] shouldBe 39.22
      (json \ "amounts" \ "oldRules" \ "graduatedRetirementBenefit").as[BigDecimal] shouldBe 2.66
      (json \ "amounts" \ "newRules" \ "grossStatePension").as[BigDecimal] shouldBe 155.40
      (json \ "amounts" \ "newRules" \ "rebateDerivedAmount").as[BigDecimal] shouldBe 0.25
      (json \ "pensionAge").as[Int] shouldBe 67
      (json \ "pensionDate").as[LocalDate] shouldBe new LocalDate(2019, 7, 1)
      (json \ "finalRelevantYear").as[String] shouldBe "2018-19"
      (json \ "numberOfQualifyingYears").as[Int] shouldBe 30
      (json \ "pensionSharingOrder").as[Boolean] shouldBe false
      (json \ "reducedRateElection").as[Boolean] shouldBe false
      (json \ "reducedRateElectionCurrentWeeklyAmount").asOpt[BigDecimal] shouldBe None
      (json \ "currentFullWeeklyPensionAmount").as[BigDecimal] shouldBe 155.65
      (json \ "statePensionAgeUnderConsideration").as[Boolean] shouldBe false
      (json \ "_links" \ "self" \ "href").as[String] shouldBe s"/test/ni/$nino"
    }

    "return 200 with a Response for RRE" in {
      val mockStatePensionService = mock[StatePensionService]

      val testStatePensionRRE = testStatePension.copy(reducedRateElection=true, reducedRateElectionCurrentWeeklyAmount=Some(155.65))

      when(mockStatePensionService.getStatement(any())(any()))
        .thenReturn(Right(testStatePensionRRE))

      val response = testStatePensionController(mockStatePensionService).get(nino)(emptyRequestWithHeader)

      status(response) shouldBe 200
      val json = contentAsJson(response)
      (json \ "earningsIncludedUpTo").as[LocalDate] shouldBe new LocalDate(2015, 4, 5)
      (json \ "amounts" \ "protectedPayment").as[Boolean] shouldBe false
      (json \ "amounts" \ "maximum" \ "yearsToWork").as[Int] shouldBe 4
      (json \ "amounts" \ "maximum" \ "gapsToFill").as[Int] shouldBe 1
      (json \ "amounts" \ "maximum" \ "weeklyAmount").as[BigDecimal] shouldBe 155.65
      (json \ "amounts" \ "maximum" \ "monthlyAmount").as[BigDecimal] shouldBe 676.80
      (json \ "amounts" \ "maximum" \ "annualAmount").as[BigDecimal] shouldBe 8121.59
      (json \ "amounts" \ "starting" \ "weeklyAmount").as[BigDecimal] shouldBe 161.18
      (json \ "amounts" \ "starting" \ "monthlyAmount").as[BigDecimal] shouldBe 700.85
      (json \ "amounts" \ "starting" \ "annualAmount").as[BigDecimal] shouldBe 8410.14
      (json \ "amounts" \ "oldRules" \ "basicStatePension").as[BigDecimal] shouldBe 119.30
      (json \ "amounts" \ "oldRules" \ "additionalStatePension").as[BigDecimal] shouldBe 39.22
      (json \ "amounts" \ "oldRules" \ "graduatedRetirementBenefit").as[BigDecimal] shouldBe 2.66
      (json \ "amounts" \ "newRules" \ "grossStatePension").as[BigDecimal] shouldBe 155.40
      (json \ "amounts" \ "newRules" \ "rebateDerivedAmount").as[BigDecimal] shouldBe 0.25
      (json \ "pensionAge").as[Int] shouldBe 67
      (json \ "pensionDate").as[LocalDate] shouldBe new LocalDate(2019, 7, 1)
      (json \ "finalRelevantYear").as[String] shouldBe "2018-19"
      (json \ "numberOfQualifyingYears").as[Int] shouldBe 30
      (json \ "pensionSharingOrder").as[Boolean] shouldBe false
      (json \ "reducedRateElection").as[Boolean] shouldBe true
      (json \ "reducedRateElectionCurrentWeeklyAmount").asOpt[BigDecimal] shouldBe Some(155.65)
      (json \ "currentFullWeeklyPensionAmount").as[BigDecimal] shouldBe 155.65
      (json \ "statePensionAgeUnderConsideration").as[Boolean] shouldBe false
      (json \ "_links" \ "self" \ "href").as[String] shouldBe s"/test/ni/$nino"
    }

    "return 200 with a Response for Customers with date of birth within the correct range for state pension age under consideration flag" in {
      val mockStatePensionService = mock[StatePensionService]

      val testStatePensionAgeUnderConsideration = testStatePension.copy(statePensionAgeUnderConsideration=true)

      when(mockStatePensionService.getStatement(any())(any()))
        .thenReturn(Right(testStatePensionAgeUnderConsideration))

      val response = testStatePensionController(mockStatePensionService).get(nino)(emptyRequestWithHeader)

      status(response) shouldBe 200
      val json = contentAsJson(response)
      (json \ "earningsIncludedUpTo").as[LocalDate] shouldBe new LocalDate(2015, 4, 5)
      (json \ "amounts" \ "protectedPayment").as[Boolean] shouldBe false
      (json \ "amounts" \ "maximum" \ "yearsToWork").as[Int] shouldBe 4
      (json \ "amounts" \ "maximum" \ "gapsToFill").as[Int] shouldBe 1
      (json \ "amounts" \ "maximum" \ "weeklyAmount").as[BigDecimal] shouldBe 155.65
      (json \ "amounts" \ "maximum" \ "monthlyAmount").as[BigDecimal] shouldBe 676.80
      (json \ "amounts" \ "maximum" \ "annualAmount").as[BigDecimal] shouldBe 8121.59
      (json \ "amounts" \ "starting" \ "weeklyAmount").as[BigDecimal] shouldBe 161.18
      (json \ "amounts" \ "starting" \ "monthlyAmount").as[BigDecimal] shouldBe 700.85
      (json \ "amounts" \ "starting" \ "annualAmount").as[BigDecimal] shouldBe 8410.14
      (json \ "amounts" \ "oldRules" \ "basicStatePension").as[BigDecimal] shouldBe 119.30
      (json \ "amounts" \ "oldRules" \ "additionalStatePension").as[BigDecimal] shouldBe 39.22
      (json \ "amounts" \ "oldRules" \ "graduatedRetirementBenefit").as[BigDecimal] shouldBe 2.66
      (json \ "amounts" \ "newRules" \ "grossStatePension").as[BigDecimal] shouldBe 155.40
      (json \ "amounts" \ "newRules" \ "rebateDerivedAmount").as[BigDecimal] shouldBe 0.25
      (json \ "pensionAge").as[Int] shouldBe 67
      (json \ "pensionDate").as[LocalDate] shouldBe new LocalDate(2019, 7, 1)
      (json \ "finalRelevantYear").as[String] shouldBe "2018-19"
      (json \ "numberOfQualifyingYears").as[Int] shouldBe 30
      (json \ "pensionSharingOrder").as[Boolean] shouldBe false
      (json \ "reducedRateElection").as[Boolean] shouldBe false
      (json \ "reducedRateElectionCurrentWeeklyAmount").asOpt[BigDecimal] shouldBe None
      (json \ "currentFullWeeklyPensionAmount").as[BigDecimal] shouldBe 155.65
      (json \ "statePensionAgeUnderConsideration").as[Boolean] shouldBe true
      (json \ "_links" \ "self" \ "href").as[String] shouldBe s"/test/ni/$nino"
    }

    "return BadRequest and message for Upstream BadRequest" in {
      val mockStatePensionService = mock[StatePensionService]

      when(mockStatePensionService.getStatement(any())(any()))
        .thenReturn(Future.failed(new BadRequestException("Upstream 400")))

      val response = testStatePensionController(mockStatePensionService).get(nino)(emptyRequestWithHeader)

      status(response) shouldBe 400
      contentAsJson(response) shouldBe Json.parse("""{"code":"BAD_REQUEST","message":"Upstream Bad Request. Is this customer below State Pension Age?"}""")
    }

    "return 403 with an error message for an MCI exclusion" in {
      val mockStatePensionService = mock[StatePensionService]

      when(mockStatePensionService.getStatement(any())(any()))
        .thenReturn(Left(
          StatePensionExclusion(List(Exclusion.ManualCorrespondenceIndicator),
            0,
            new LocalDate(2050, 1, 1),
            false)
        ))

      val response = testStatePensionController(mockStatePensionService).get(nino)(emptyRequestWithHeader)

      status(response) shouldBe 403
      contentAsJson(response) shouldBe Json.parse("""{"code":"EXCLUSION_MANUAL_CORRESPONDENCE","message":"The customer cannot access the service, they should contact HMRC"}""")
    }

    "return 403 with an error message for a Dead exclusion" in {
      val mockStatePensionService = mock[StatePensionService]

      when(mockStatePensionService.getStatement(any())(any()))
        .thenReturn(Left(
          StatePensionExclusion(List(Exclusion.Dead),
            0,
            new LocalDate(2050, 1, 1),
            false)
        ))

      val response = testStatePensionController(mockStatePensionService).get(nino)(emptyRequestWithHeader)

      status(response) shouldBe 403
      contentAsJson(response) shouldBe Json.parse("""{"code":"EXCLUSION_DEAD","message":"The customer needs to contact the National Insurance helpline"}""")
    }

    "return 403 with the dead error message if user is Dead and has MCI" in {
      val mockStatePensionService = mock[StatePensionService]

      when(mockStatePensionService.getStatement(any())(any()))
        .thenReturn(Left(StatePensionExclusion(
            List(Exclusion.Dead, Exclusion.ManualCorrespondenceIndicator),
            0,
            new LocalDate(2050, 1, 1),
            false
          )
        ))

      val response = testStatePensionController(mockStatePensionService).get(nino)(emptyRequestWithHeader)

      status(response) shouldBe 403
      contentAsJson(response) shouldBe Json.parse("""{"code":"EXCLUSION_DEAD","message":"The customer needs to contact the National Insurance helpline"}""")
    }

  }

}
