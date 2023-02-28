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

import java.time.LocalDate
import org.mockito.Mockito.{reset, when}
import org.scalatest.{Assertion, BeforeAndAfterEach}
import play.api.inject.Injector
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.statepension.UnitSpec
import uk.gov.hmrc.statepension.models.TaxRates
import uk.gov.hmrc.statepension.util.SystemLocalDate

class AppConfigSpec extends UnitSpec with BeforeAndAfterEach {

  val mockSystemLocalDate: SystemLocalDate = mock[SystemLocalDate]

  val injector: Injector = new GuiceApplicationBuilder()
    .configure(
      ("microservice.services.if-hod.host", "testLocalHost"),
      ("microservice.services.if-hod.port", "1234"),
      ("microservice.services.if-hod.environment", "test"),
      ("microservice.services.if-hod.token", "testABC123"),
      ("microservice.services.if-hod.originatoridkey", "testOriginatorId"),
      ("microservice.services.if-hod.originatoridvalue", "testOriginatorIdValue"),
      ("rates.effectiveFromDate", "2023-04-10")
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

        when(mockSystemLocalDate.currentLocalDate).thenReturn(LocalDate.of(2023, 4, 10))

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

        appConfig.taxRates shouldBe TaxRates(130.9668, 123.5924, spRates)
      }

      "return current tax year rates when currentLocalDate is after effectiveFromDate and is after 1st January in current tax year" in {
        when(mockSystemLocalDate.currentLocalDate).thenReturn(LocalDate.of(2024, 3, 3))

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

        appConfig.taxRates shouldBe TaxRates(130.9668, 123.5924, spRates)
      }

      "return current tax year rates when currentLocalDate is after effectiveFromDate and is before 1st January in current tax year" in {
        when(mockSystemLocalDate.currentLocalDate).thenReturn(LocalDate.of(2023, 4, 11))

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

        appConfig.taxRates shouldBe TaxRates(130.9668, 123.5924, spRates)
      }

      "return previous tax year rates when currentLocalDate is before effectiveFromDate" in {
        when(mockSystemLocalDate.currentLocalDate).thenReturn(LocalDate.of(2023, 2, 23))

        val spRates: Seq[BigDecimal] = Seq(0,
          5.29,
          10.58,
          15.87,
          21.16,
          26.45,
          31.74,
          37.03,
          42.32,
          47.61,
          52.90,
          58.19,
          63.48,
          68.77,
          74.06,
          79.35,
          84.64,
          89.93,
          95.22,
          100.51,
          105.80,
          111.09,
          116.38,
          121.67,
          126.96,
          132.25,
          137.54,
          142.83,
          148.12,
          153.41,
          158.70,
          163.99,
          169.28,
          174.57,
          179.86,
          185.15)

        appConfig.taxRates shouldBe TaxRates(1.189527, 1.122547, spRates)
      }
    }
  }

  def serviceUrl(serviceUrl: String): Assertion = {
    serviceUrl should include("testLocalHost")
    serviceUrl should include("1234")
  }
}
