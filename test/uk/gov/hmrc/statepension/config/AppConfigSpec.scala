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

package uk.gov.hmrc.statepension.config

import org.mockito.Mockito.{reset, when}
import org.scalatest.Assertion
import play.api.inject.{Injector, bind}
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.statepension.models.TaxRates
import uk.gov.hmrc.statepension.util.SystemLocalDate
import utils.StatePensionBaseSpec

import java.time.LocalDate

class AppConfigSpec extends StatePensionBaseSpec {

  val mockSystemLocalDate: SystemLocalDate = mock[SystemLocalDate]

  val injector: Injector = new GuiceApplicationBuilder()
    .configure(
      ("microservice.services.if-hod.host", "testLocalHost"),
      ("microservice.services.if-hod.port", "1234"),
      ("microservice.services.if-hod.environment", "test"),
      ("microservice.services.if-hod.token", "testABC123"),
      ("microservice.services.if-hod.originatoridkey", "testOriginatorId"),
      ("microservice.services.if-hod.originatoridvalue", "testOriginatorIdValue"),
      ("rates.effectiveFromDate", "2026-04-07")
    ).overrides(
      bind[SystemLocalDate].toInstance(mockSystemLocalDate)
    ).injector()

  val appConfig: AppConfig = injector.instanceOf[AppConfig]

  override def beforeEach(): Unit = {
    reset(mockSystemLocalDate)
    super.beforeEach()
  }

  "AppConfig" must {
    "return the correct values for a ConnectorConfig" in {
      val ifConnectorConfig: ConnectorConfig = appConfig.ifConnectorConfig

      behave like serviceUrl(ifConnectorConfig.serviceUrl)
      ifConnectorConfig.authorizationToken shouldBe "testABC123"
      ifConnectorConfig.environment shouldBe "test"
      ifConnectorConfig.serviceOriginatorIdValue shouldBe "testOriginatorIdValue"
      ifConnectorConfig.serviceOriginatorIdKey shouldBe "testOriginatorId"
    }

    "taxRates" should {
      "return current tax year rates file as TaxRates case class when effectiveDate matches currentLocalDate" in {

        when(mockSystemLocalDate.currentLocalDate).thenReturn(LocalDate.of(2026, 4, 7))

        val spRates: Seq[BigDecimal] = Seq(
          0,
          6.89,
          13.79,
          20.68,
          27.58,
          34.47,
          41.37,
          48.26,
          55.15,
          62.05,
          68.94,
          75.84,
          82.73,
          89.63,
          96.52,
          103.41,
          110.31,
          117.20,
          124.10,
          130.99,
          137.89,
          144.78,
          151.67,
          158.57,
          165.46,
          172.36,
          179.25,
          186.15,
          193.04,
          199.93,
          206.83,
          213.72,
          220.62,
          227.51,
          234.41,
          241.30)

        appConfig.taxRates shouldBe TaxRates(1.550271, 1.392113, spRates)
      }

      "return current tax year rates when currentLocalDate is after effectiveFromDate and is after 1st January in current tax year" in {
        when(mockSystemLocalDate.currentLocalDate).thenReturn(LocalDate.of(2027, 1, 1))

        val spRates: Seq[BigDecimal] = Seq(
          0,
          6.89,
          13.79,
          20.68,
          27.58,
          34.47,
          41.37,
          48.26,
          55.15,
          62.05,
          68.94,
          75.84,
          82.73,
          89.63,
          96.52,
          103.41,
          110.31,
          117.20,
          124.10,
          130.99,
          137.89,
          144.78,
          151.67,
          158.57,
          165.46,
          172.36,
          179.25,
          186.15,
          193.04,
          199.93,
          206.83,
          213.72,
          220.62,
          227.51,
          234.41,
          241.30)

        appConfig.taxRates shouldBe TaxRates(1.550271, 1.392113, spRates)
      }

      "return current tax year rates when currentLocalDate is after effectiveFromDate and is before 1st January in current tax year" in {
        when(mockSystemLocalDate.currentLocalDate).thenReturn(LocalDate.of(2026, 12, 31))

        val spRates: Seq[BigDecimal] = Seq(
          0,
          6.89,
          13.79,
          20.68,
          27.58,
          34.47,
          41.37,
          48.26,
          55.15,
          62.05,
          68.94,
          75.84,
          82.73,
          89.63,
          96.52,
          103.41,
          110.31,
          117.20,
          124.10,
          130.99,
          137.89,
          144.78,
          151.67,
          158.57,
          165.46,
          172.36,
          179.25,
          186.15,
          193.04,
          199.93,
          206.83,
          213.72,
          220.62,
          227.51,
          234.41,
          241.30)

        appConfig.taxRates shouldBe TaxRates(1.550271, 1.392113, spRates)
      }

      "return previous tax year rates when currentLocalDate is before effectiveFromDate" in {
        when(mockSystemLocalDate.currentLocalDate).thenReturn(LocalDate.of(2026, 4, 6))

        val spRates: Seq[BigDecimal] = Seq(
          0,
          6.58,
          13.16,
          19.74,
          26.31,
          32.89,
          39.47,
          46.05,
          52.63,
          59.21,
          65.79,
          72.36,
          78.94,
          85.52,
          92.10,
          98.68,
          105.26,
          111.84,
          118.41,
          124.99,
          131.57,
          138.15,
          144.73,
          151.31,
          157.89,
          164.46,
          171.04,
          177.62,
          184.20,
          190.78,
          197.36,
          203.94,
          210.51,
          217.09,
          223.67,
          230.25)

        appConfig.taxRates shouldBe TaxRates(1.479279, 1.341149, spRates)
      }
    }
  }

  def serviceUrl(serviceUrl: String): Assertion = {
    serviceUrl should include("testLocalHost")
    serviceUrl should include("1234")
  }
}
