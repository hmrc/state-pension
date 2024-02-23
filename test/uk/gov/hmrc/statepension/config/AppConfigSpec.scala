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
      ("rates.effectiveFromDate", "2024-04-08")
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

        when(mockSystemLocalDate.currentLocalDate).thenReturn(LocalDate.of(2024, 4, 8))

        val spRates: Seq[BigDecimal] = Seq(0,
          6.32,
          12.64,
          18.96,
          25.28,
          31.60,
          37.92,
          44.24,
          50.56,
          56.88,
          63.20,
          69.52,
          75.84,
          82.16,
          88.48,
          94.80,
          101.12,
          107.44,
          113.76,
          120.08,
          126.40,
          132.72,
          139.04,
          145.36,
          151.68,
          158.00,
          164.32,
          170.64,
          176.96,
          183.28,
          189.60,
          195.92,
          202.24,
          208.56,
          214.88,
          221.20)

        appConfig.taxRates shouldBe TaxRates(1.421136, 1.318731, spRates)
      }

      "return current tax year rates when currentLocalDate is after effectiveFromDate and is after 1st January in current tax year" in {
        when(mockSystemLocalDate.currentLocalDate).thenReturn(LocalDate.of(2025, 1, 1))

        val spRates: Seq[BigDecimal] = Seq(0,
          6.32,
          12.64,
          18.96,
          25.28,
          31.60,
          37.92,
          44.24,
          50.56,
          56.88,
          63.20,
          69.52,
          75.84,
          82.16,
          88.48,
          94.80,
          101.12,
          107.44,
          113.76,
          120.08,
          126.40,
          132.72,
          139.04,
          145.36,
          151.68,
          158.00,
          164.32,
          170.64,
          176.96,
          183.28,
          189.60,
          195.92,
          202.24,
          208.56,
          214.88,
          221.20)

        appConfig.taxRates shouldBe TaxRates(1.421136, 1.318731, spRates)
      }

      "return current tax year rates when currentLocalDate is after effectiveFromDate and is before 1st January in current tax year" in {
        when(mockSystemLocalDate.currentLocalDate).thenReturn(LocalDate.of(2024, 12, 31))

        val spRates: Seq[BigDecimal] = Seq(0,
          6.32,
          12.64,
          18.96,
          25.28,
          31.60,
          37.92,
          44.24,
          50.56,
          56.88,
          63.20,
          69.52,
          75.84,
          82.16,
          88.48,
          94.80,
          101.12,
          107.44,
          113.76,
          120.08,
          126.40,
          132.72,
          139.04,
          145.36,
          151.68,
          158.00,
          164.32,
          170.64,
          176.96,
          183.28,
          189.60,
          195.92,
          202.24,
          208.56,
          214.88,
          221.20)

        appConfig.taxRates shouldBe TaxRates(1.421136, 1.318731, spRates)
      }

      "return previous tax year rates when currentLocalDate is before effectiveFromDate" in {
        when(mockSystemLocalDate.currentLocalDate).thenReturn(LocalDate.of(2024, 4, 7))

        val spRates: Seq[BigDecimal] = Seq(0,
          5.82,
          11.65,
          17.47,
          23.30,
          29.12,
          34.95,
          40.77,
          46.59,
          52.42,
          58.24,
          64.07,
          69.89,
          75.72,
          81.54,
          87.36,
          93.19,
          99.01,
          104.84,
          110.66,
          116.49,
          122.31,
          128.13,
          133.96,
          139.78,
          145.61,
          151.43,
          157.26,
          163.08,
          168.90,
          174.73,
          180.55,
          186.38,
          192.20,
          198.03,
          203.85)

        appConfig.taxRates shouldBe TaxRates(1.309668, 1.235924, spRates)
      }
    }
  }

  def serviceUrl(serviceUrl: String): Assertion = {
    serviceUrl should include("testLocalHost")
    serviceUrl should include("1234")
  }
}
