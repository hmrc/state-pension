/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.statepension

import org.joda.time.LocalDate
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.statepension.controllers.StatePensionController
import uk.gov.hmrc.statepension.domain.{StatePension, StatePensionAmount, StatePensionAmounts, StatePensionExclusion}
import uk.gov.hmrc.statepension.services.StatePensionService
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.{HeaderCarrier, BadRequestException}

import scala.concurrent.Future
import scala.util.Random

class StatePensionControllerSpec extends UnitSpec with WithFakeApplication {

  val nino: Nino = new Generator(new Random()).nextNino

  val emptyRequest = FakeRequest()
  val emptyRequestWithHeader = FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")

  def testStatePensionController(spService: StatePensionService): StatePensionController = new StatePensionController {
    override val statePensionService: StatePensionService = spService

    override val app: String = "Test State Pension"
    override val context: String = "test"
  }

  val testStatePension = StatePension(
    new LocalDate(2015, 4, 5),
    StatePensionAmounts(
      protectedPayment = false,
      StatePensionAmount(None, None, 123.65, 537.66, 6451.88),
      StatePensionAmount(Some(4), None, 151.25, 657.67, 7892.01),
      StatePensionAmount(Some(4), Some(1), 155.65, 676.80, 8121.59),
      StatePensionAmount(None, None, 0.25, 1.09, 13.04)
    ),
    67,
    new LocalDate(2019, 7 ,1),
    2018,
    30,
    pensionSharingOrder = false,
    155.65
  )

  "get" should {
    "return status code 406 when the headers are invalid" in {
      val response = testStatePensionController(new StatePensionService {
        override def getStatement(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExclusion, StatePension]] = ???
      }).get(nino)(emptyRequest)

      status(response) shouldBe 406
      contentAsJson(response) shouldBe Json.parse("""{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}""")
    }

    "return 200 with a Response" in {
      val response = testStatePensionController(new StatePensionService {
        override def getStatement(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExclusion, StatePension]] = Right(testStatePension)
      }).get(nino)(emptyRequestWithHeader)

      status(response) shouldBe 200
      val json = contentAsJson(response)
      (json \ "earningsIncludedUpTo").as[LocalDate] shouldBe new LocalDate(2015, 4, 5)
      (json \ "amounts" \ "protectedPayment").as[Boolean] shouldBe false
      (json \ "amounts" \ "maximum" \ "yearsToWork").as[Int] shouldBe 4
      (json \ "amounts" \ "maximum" \ "gapsToFill").as[Int] shouldBe 1
      (json \ "amounts" \ "maximum" \ "weeklyAmount").as[BigDecimal] shouldBe 155.65
      (json \ "amounts" \ "maximum" \ "monthlyAmount").as[BigDecimal] shouldBe 676.80
      (json \ "amounts" \ "maximum" \ "annualAmount").as[BigDecimal] shouldBe 8121.59
      (json \ "pensionAge").as[Int] shouldBe 67
      (json \ "pensionDate").as[LocalDate] shouldBe new LocalDate(2019, 7, 1)
      (json \ "finalRelevantYear").as[Int] shouldBe 2018
      (json \ "numberOfQualifyingYears").as[Int] shouldBe 30
      (json \ "pensionSharingOrder").as[Boolean] shouldBe false
      (json \ "currentFullWeeklyPensionAmount").as[BigDecimal] shouldBe 155.65
      (json \ "_links" \ "self" \ "href").as[String] shouldBe s"/test/paye/$nino"
    }

    "return BadRequest and message for Upstream BadRequest" in {
      val response = testStatePensionController(new StatePensionService {
        override def getStatement(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExclusion, StatePension]] = Future.failed(new BadRequestException("Upstream 400"))
      }).get(nino)(emptyRequestWithHeader)

      status(response) shouldBe 400
      contentAsJson(response) shouldBe Json.parse("""{"code":"BAD_REQUEST","message":"Upstream Bad Request. Is this customer below State Pension Age?"}""")
    }

  }


}
