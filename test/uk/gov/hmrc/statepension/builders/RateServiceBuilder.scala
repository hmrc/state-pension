/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.statepension.builders

import org.joda.time.LocalDate
import uk.gov.hmrc.statepension.StatePensionBaseSpec
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.models.TaxRates
import uk.gov.hmrc.statepension.services.RateService
import uk.gov.hmrc.statepension.util.SystemLocalDate

object RateServiceBuilder extends StatePensionBaseSpec {

  val appConfig: AppConfig = mock[AppConfig]
  val systemLocalDate = new SystemLocalDate {
    override def currentLocalDate: LocalDate = LocalDate.now()
  }
  def apply(testRates: TaxRates): RateService = new RateService(appConfig, systemLocalDate) {
    override lazy val taxRates: TaxRates = testRates
  }

  val default: RateService = apply(TaxRates(1, 1, Seq(
    0,
    4.45,
    8.89,
    13.34,
    17.79,
    22.24,
    26.68,
    31.13,
    35.58,
    40.02,
    44.47,
    48.92,
    53.37,
    57.81,
    62.26,
    66.71,
    71.15,
    75.6,
    80.05,
    84.5,
    88.94,
    93.39,
    97.84,
    102.28,
    106.73,
    111.18,
    115.63,
    120.07,
    124.52,
    128.97,
    133.41,
    137.86,
    142.31,
    146.76,
    151.2,
    155.65
  )))

  val twentySeventeenToTwentyEighteen: RateService = apply(TaxRates(1.025056, 1.01, Seq(
    0,
    4.56,
    9.12,
    13.68,
    18.23,
    22.79,
    27.35,
    31.91,
    36.47,
    41.03,
    45.59,
    50.14,
    54.7,
    59.26,
    63.82,
    68.38,
    72.94,
    77.5,
    82.05,
    86.61,
    91.17,
    95.73,
    100.29,
    104.85,
    109.41,
    113.96,
    118.52,
    123.08,
    127.64,
    132.2,
    136.76,
    141.32,
    145.87,
    150.43,
    154.99,
    159.55
  )))

  val twentyEighteenToTwentyNineteen: RateService = apply(TaxRates(1.055895, 1.0403, Seq(
    0,
    4.70,
    9.39,
    14.09,
    18.78,
    23.48,
    28.17,
    32.87,
    37.57,
    42.26,
    46.96,
    51.65,
    56.35,
    61.04,
    65.74,
    70.44,
    75.13,
    79.83,
    84.52,
    89.22,
    93.91,
    98.61,
    103.31,
    108.00,
    112.70,
    117.39,
    122.09,
    126.78,
    131.48,
    136.18,
    140.87,
    145.57,
    150.26,
    154.96,
    159.65,
    164.35
  )))

  val twentyNineteenToTwentyTwenty: RateService = apply(TaxRates( 1.083199, 1.065267, Seq(
    0,
    4.82,
    9.63,
    14.45,
    19.27,
    24.09,
    28.90,
    33.72,
    38.54,
    43.35,
    48.17,
    52.99,
    57.81,
    62.62,
    67.44,
    72.26,
    77.07,
    81.89,
    86.71,
    91.53,
    96.34,
    101.16,
    105.98,
    110.79,
    115.61,
    120.43,
    125.25,
    130.06,
    134.88,
    139.70,
    144.51,
    149.33,
    154.15,
    158.97,
    163.78,
    168.60
  )))
}
