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

package uk.gov.hmrc.statepension.domain

import org.joda.time.LocalDate
import org.scalatest.Matchers._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Injecting
import uk.gov.hmrc.statepension.StatePensionBaseSpec
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.domain.CopeDatePeriod._

class CopeRecordSpec extends StatePensionBaseSpec with GuiceOneAppPerSuite with Injecting {

  val nino = generateNino()

  "defineCopePeriod" must {
    "return Initial Period when today is before 4 weeks of initialLoginDate" in {
      val copePeriod = CopeRecord(nino, LocalDate.now().minusWeeks(3))
        .defineCopePeriod(LocalDate.now(), inject[AppConfig])

      copePeriod shouldBe Initial
    }

    "return Extended Period when today is after 4 weeks and before 13 weeks of the initialLoginDate" in {
      val copePeriod = CopeRecord(nino, LocalDate.now().minusWeeks(10))
        .defineCopePeriod(LocalDate.now(), inject[AppConfig])

      copePeriod shouldBe Extended
    }

    "return Expired Period when today is after 13 weeks of the initialLoginDate" in {
      val copePeriod = CopeRecord(nino, LocalDate.now().minusWeeks(14))
        .defineCopePeriod(LocalDate.now(), inject[AppConfig])

      copePeriod shouldBe Expired
    }
  }

}
