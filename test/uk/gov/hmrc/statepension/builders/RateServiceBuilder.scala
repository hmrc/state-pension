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

package uk.gov.hmrc.statepension.builders

import play.api.Configuration
import uk.gov.hmrc.statepension.services.RateService

object RateServiceBuilder {

  private def rateToConfig(pair: (Int, BigDecimal)): (String, Any) = pair._1.toString -> pair._2

  def apply(rates: Map[Int, BigDecimal], revaluationStartingAmount: BigDecimal = 1, revaluationProtectedPayment: BigDecimal = 1): RateService = new RateService {
    override lazy val ratesConfig: Configuration = Configuration.from(rates.map(rateToConfig))
    override lazy val revaluationConfig: Option[Configuration] = Some(Configuration.from(Map("startingAmount" -> revaluationStartingAmount, "protectedPayment" -> revaluationProtectedPayment)))
  }

  val default: RateService = apply(Map(
    0 -> 0,
    1 -> 4.45,
    2 -> 8.89,
    3 -> 13.34,
    4 -> 17.79,
    5 -> 22.24,
    6 -> 26.68,
    7 -> 31.13,
    8 -> 35.58,
    9 -> 40.02,
    10 -> 44.47,
    11 -> 48.92,
    12 -> 53.37,
    13 -> 57.81,
    14 -> 62.26,
    15 -> 66.71,
    16 -> 71.15,
    17 -> 75.6,
    18 -> 80.05,
    19 -> 84.5,
    20 -> 88.94,
    21 -> 93.39,
    22 -> 97.84,
    23 -> 102.28,
    24 -> 106.73,
    25 -> 111.18,
    26 -> 115.63,
    27 -> 120.07,
    28 -> 124.52,
    29 -> 128.97,
    30 -> 133.41,
    31 -> 137.86,
    32 -> 142.31,
    33 -> 146.76,
    34 -> 151.2,
    35 -> 155.65
  ))

  val twentySeventeenToTwentyEighteen: RateService = apply(Map(
    0 -> 0,
    1 -> 4.56,
    2 -> 9.12,
    3 -> 13.68,
    4 -> 18.23,
    5 -> 22.79,
    6 -> 27.35,
    7 -> 31.91,
    8 -> 36.47,
    9 -> 41.03,
    10 -> 45.59,
    11 -> 50.14,
    12 -> 54.7,
    13 -> 59.26,
    14 -> 63.82,
    15 -> 68.38,
    16 -> 72.94,
    17 -> 77.5,
    18 -> 82.05,
    19 -> 86.61,
    20 -> 91.17,
    21 -> 95.73,
    22 -> 100.29,
    23 -> 104.85,
    24 -> 109.41,
    25 -> 113.96,
    26 -> 118.52,
    27 -> 123.08,
    28 -> 127.64,
    29 -> 132.2,
    30 -> 136.76,
    31 -> 141.32,
    32 -> 145.87,
    33 -> 150.43,
    34 -> 154.99,
    35 -> 159.55
  ), revaluationStartingAmount = 1.025056, revaluationProtectedPayment = 1.01)
}
