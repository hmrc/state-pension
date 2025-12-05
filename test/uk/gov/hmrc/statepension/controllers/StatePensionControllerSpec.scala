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

package uk.gov.hmrc.statepension.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.Json
import uk.gov.hmrc.statepension.controllers.auth.{ApiAuthAction, FakeMdtpAuthorizeAction}
import play.api.mvc.{AnyContentAsEmpty, BodyParsers, ControllerComponents}
import uk.gov.hmrc.statepension.controllers.statepension.StatePensionController
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers, Injecting}
import uk.gov.hmrc.domain.{NinoGenerator, Nino}
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.controllers.auth.FakeApiAuthAction
import uk.gov.hmrc.statepension.domain.Exclusion.{IsleOfMan, ManualCorrespondenceIndicator}
import uk.gov.hmrc.statepension.domain._
import uk.gov.hmrc.statepension.services.CheckPensionService
import utils.{CopeRepositoryHelper, StatePensionBaseSpec}

import java.time.LocalDate
import scala.concurrent.Future
import scala.util.Random

trait StatePensionControllerSpec extends StatePensionBaseSpec with GuiceOneAppPerSuite with Injecting with CopeRepositoryHelper {

  def testCheckPensionController(cpService: CheckPensionService): StatePensionController
  def linkUrl: String

  val nino: Nino = new NinoGenerator().nextNino

  val emptyRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val emptyRequestWithHeader: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")

  val controllerComponents: ControllerComponents = Helpers.stubControllerComponents()
  val _appContext: AppConfig = inject[AppConfig]
  val fakeApiAuthAction: ApiAuthAction = inject[FakeApiAuthAction]
  val fakeMdtpAuthorizationAction: FakeMdtpAuthorizeAction = inject[FakeMdtpAuthorizeAction]
  val fakeErrorHandling: ErrorHandling = inject[ErrorHandling]
  val parser: BodyParsers.Default = inject[BodyParsers.Default]

  val testStatePension: StatePension = StatePension(
    LocalDate.of(2015, 4, 5),
    StatePensionAmounts(
      protectedPayment = false,
      StatePensionAmount(None, None, 123.65),
      StatePensionAmount(Some(4), None, 151.25),
      StatePensionAmount(Some(4), Some(1), 155.65),
      StatePensionAmount(None, None, 0.25),
      StatePensionAmount(None, None, 161.18),
      OldRules(
        basicStatePension = 119.30,
        additionalStatePension = 39.22,
        graduatedRetirementBenefit = 2.66
      ),
      NewRules(
        grossStatePension = 155.40,
        rebateDerivedAmount = 0.25
      )
    ),
    67,
    LocalDate.of(2019, 7, 1),
    "2018-19",
    30,
    pensionSharingOrder = false,
    155.65,
    false,
    None,
    false
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCopeRepository)
  }

  "get" should {
    "return status code 406 when the headers are invalid" in {
      val mockCheckPensionService = mock[CheckPensionService]

      when(mockCheckPensionService.getStatement(any())(any())).thenReturn(Future.successful(Right(testStatePension)))

      val response = testCheckPensionController(mockCheckPensionService).get(nino)(emptyRequest)

      status(response) shouldBe 406
      contentAsJson(response) shouldBe Json.parse("""{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}""")
    }

    "return 200 with a Response" in {
      val mockCheckPensionService = mock[CheckPensionService]
      when(mockCheckPensionService.getStatement(any())(any())).thenReturn(Future.successful(Right(testStatePension)))

      val response = testCheckPensionController(mockCheckPensionService).get(nino)(emptyRequestWithHeader)

      status(response) shouldBe 200
      val json = contentAsJson(response)
      (json \ "earningsIncludedUpTo").as[LocalDate] shouldBe LocalDate.of(2015, 4, 5)
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
      (json \ "pensionDate").as[LocalDate] shouldBe LocalDate.of(2019, 7, 1)
      (json \ "finalRelevantYear").as[String] shouldBe "2018-19"
      (json \ "numberOfQualifyingYears").as[Int] shouldBe 30
      (json \ "pensionSharingOrder").as[Boolean] shouldBe false
      (json \ "reducedRateElection").as[Boolean] shouldBe false
      (json \ "reducedRateElectionCurrentWeeklyAmount").asOpt[BigDecimal] shouldBe None
      (json \ "currentFullWeeklyPensionAmount").as[BigDecimal] shouldBe 155.65
      (json \ "statePensionAgeUnderConsideration").as[Boolean] shouldBe false
      (json \ "_links" \ "self" \ "href").as[String] shouldBe s"/state-pension/$linkUrl$nino"

      verify(mockCopeRepository, times(1)).delete(HashedNino(nino))
    }

    "return 200 with a Response for RRE" in {
      val testStatePensionRRE = testStatePension.copy(reducedRateElection = true, reducedRateElectionCurrentWeeklyAmount = Some(155.65))

      val mockCheckPensionService = mock[CheckPensionService]
      when(mockCheckPensionService.getStatement(any())(any())).thenReturn(Future.successful(Right(testStatePensionRRE)))

      val response = testCheckPensionController(mockCheckPensionService).get(nino)(emptyRequestWithHeader)

      status(response) shouldBe 200
      val json = contentAsJson(response)
      (json \ "earningsIncludedUpTo").as[LocalDate] shouldBe LocalDate.of(2015, 4, 5)
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
      (json \ "pensionDate").as[LocalDate] shouldBe LocalDate.of(2019, 7, 1)
      (json \ "finalRelevantYear").as[String] shouldBe "2018-19"
      (json \ "numberOfQualifyingYears").as[Int] shouldBe 30
      (json \ "pensionSharingOrder").as[Boolean] shouldBe false
      (json \ "reducedRateElection").as[Boolean] shouldBe true
      (json \ "reducedRateElectionCurrentWeeklyAmount").asOpt[BigDecimal] shouldBe Some(155.65)
      (json \ "currentFullWeeklyPensionAmount").as[BigDecimal] shouldBe 155.65
      (json \ "statePensionAgeUnderConsideration").as[Boolean] shouldBe false
      (json \ "_links" \ "self" \ "href").as[String] shouldBe s"/state-pension/$linkUrl$nino"
    }

    "return 200 with a Response for Customers with date of birth within the correct range for state pension age under consideration flag" in {

      val testStatePensionAgeUnderConsideration = testStatePension.copy(statePensionAgeUnderConsideration = true)

      val mockCheckPensionService = mock[CheckPensionService]
      when(mockCheckPensionService.getStatement(any())(any())).thenReturn(Future.successful(Right(testStatePensionAgeUnderConsideration)))

      val response = testCheckPensionController(mockCheckPensionService).get(nino)(emptyRequestWithHeader)

      status(response) shouldBe 200
      val json = contentAsJson(response)
      (json \ "earningsIncludedUpTo").as[LocalDate] shouldBe LocalDate.of(2015, 4, 5)
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
      (json \ "pensionDate").as[LocalDate] shouldBe LocalDate.of(2019, 7, 1)
      (json \ "finalRelevantYear").as[String] shouldBe "2018-19"
      (json \ "numberOfQualifyingYears").as[Int] shouldBe 30
      (json \ "pensionSharingOrder").as[Boolean] shouldBe false
      (json \ "reducedRateElection").as[Boolean] shouldBe false
      (json \ "reducedRateElectionCurrentWeeklyAmount").asOpt[BigDecimal] shouldBe None
      (json \ "currentFullWeeklyPensionAmount").as[BigDecimal] shouldBe 155.65
      (json \ "statePensionAgeUnderConsideration").as[Boolean] shouldBe true
      (json \ "_links" \ "self" \ "href").as[String] shouldBe s"/state-pension/$linkUrl$nino"
    }

    "return 200 with an exclusion" in {
      val mockCheckPensionService = mock[CheckPensionService]
      when(mockCheckPensionService.getStatement(any())(any())).thenReturn(Future.successful(
        Left(
          StatePensionExclusion(
            exclusionReasons = List(IsleOfMan),
            pensionAge = 0,
            pensionDate = LocalDate.of(2050, 1, 1),
            statePensionAgeUnderConsideration = false
          )
        ))
      )

      val response = testCheckPensionController(mockCheckPensionService).get(nino)(emptyRequestWithHeader)

      status(response) shouldBe 200

      val json = contentAsJson(response)
      (json \ "_links" \ "self" \ "href").as[String] shouldBe s"/state-pension/$linkUrl$nino"
      (json \ "exclusionReasons").as[List[Exclusion]] shouldBe List(IsleOfMan)
      (json \ "pensionAge").as[Int] shouldBe 0
      (json \ "pensionDate").as[String] shouldBe "2050-01-01"
      (json \ "statePensionAgeUnderConsideration").as[Boolean] shouldBe false
    }

    "return 403 with an error message for an MCI exclusion" in {
      val mockCheckPensionService = mock[CheckPensionService]

      when(mockCheckPensionService.getStatement(any())(any())).thenReturn(Future.successful(
        Left(
          StatePensionExclusion(
            exclusionReasons = List(ManualCorrespondenceIndicator),
            pensionAge = 0,
            pensionDate = LocalDate.of(2050, 1, 1),
            statePensionAgeUnderConsideration = false
          )
        ))
      )

      val response = testCheckPensionController(mockCheckPensionService).get(nino)(emptyRequestWithHeader)

      status(response) shouldBe 403

      contentAsJson(response) shouldBe Json.parse("""{"code":"EXCLUSION_MANUAL_CORRESPONDENCE","message":"The customer cannot access the service, they should contact HMRC"}""")
    }

    "return 403 with an error message for a Dead exclusion" in {
      val mockCheckPensionService = mock[CheckPensionService]

      when(mockCheckPensionService.getStatement(any())(any())).thenReturn(Future.successful(
        Left(
          StatePensionExclusion(List(Exclusion.Dead),
            0,
            LocalDate.of(2050, 1, 1),
            false)
        ))
      )

      val response = testCheckPensionController(mockCheckPensionService).get(nino)(emptyRequestWithHeader)

      status(response) shouldBe 403
      contentAsJson(response) shouldBe Json.parse("""{"code":"EXCLUSION_DEAD","message":"The customer needs to contact the National Insurance helpline"}""")
    }

    "return 403 with the dead error message if user is Dead and has MCI" in {
      val mockCheckPensionService = mock[CheckPensionService]

      when(mockCheckPensionService.getStatement(any())(any())).thenReturn(Future.successful(
        Left(StatePensionExclusion(
          List(Exclusion.Dead, Exclusion.ManualCorrespondenceIndicator),
          0,
          LocalDate.of(2050, 1, 1),
          statePensionAgeUnderConsideration = false
        )
        ))
      )

      val response = testCheckPensionController(mockCheckPensionService).get(nino)(emptyRequestWithHeader)

      status(response) shouldBe 403
      contentAsJson(response) shouldBe Json.parse("""{"code":"EXCLUSION_DEAD","message":"The customer needs to contact the National Insurance helpline"}""")
    }
  }
}
