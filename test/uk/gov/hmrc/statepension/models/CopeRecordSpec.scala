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

package uk.gov.hmrc.statepension.models

import org.joda.time.LocalDate
import org.mockito.Mockito.when
import org.scalatest.Matchers._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Injecting
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.statepension.StatePensionBaseSpec
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.controllers.HashedNino
import uk.gov.hmrc.statepension.models.CopeDatePeriod._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
class CopeRecordSpec extends StatePensionBaseSpec with GuiceOneAppPerSuite with Injecting {

  val nino: Nino = generateNino()
  lazy val appConfig: AppConfig = inject[AppConfig]

  "defineCopePeriod" must {
    "return Initial Period when firstLoginDate + returnToServiceWeeks is before the copeAvailableDate" in {
      val firstLoginDate: LocalDate = LocalDate.now().minusWeeks(1)
      val copeAvailableDate: LocalDate = firstLoginDate.plusWeeks(7)

      val copePeriod = CopeRecord(HashedNino(nino), firstLoginDate, copeAvailableDate).defineCopePeriod(appConfig)

      copePeriod shouldBe Initial
    }

    "return Initial Period when firstLoginDate + returnToServiceWeeks is on the copeAvailableDate" in {
      val firstLoginDate: LocalDate = LocalDate.now().minusWeeks(appConfig.returnToServiceWeeks)
      val copeAvailableDate: LocalDate = firstLoginDate.plusWeeks(appConfig.returnToServiceWeeks)

      val copePeriod = CopeRecord(HashedNino(nino), firstLoginDate, copeAvailableDate).defineCopePeriod(appConfig)

      copePeriod shouldBe Initial
    }

    "return Extended Period when firstLoginDate + returnToServiceWeeks is after the copeAvailableDate" in {
      val mocAppConfig = mock[AppConfig]

      when(mocAppConfig.returnToServiceWeeks).thenReturn(100)

      val firstLoginDate: LocalDate = LocalDate.now().minusWeeks(1)
      val copeAvailableDate: LocalDate = firstLoginDate.plusWeeks(5)
      
      val copePeriod = CopeRecord(HashedNino(nino), firstLoginDate, copeAvailableDate).defineCopePeriod(mocAppConfig)

      copePeriod shouldBe Extended
    }

  }

}
