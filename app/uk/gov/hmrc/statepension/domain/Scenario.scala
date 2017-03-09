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

package uk.gov.hmrc.statepension.domain

sealed trait Scenario
object Scenario {
  case object Reached extends Scenario
  case object ContinueWorkingMax extends Scenario
  case object ContinueWorkingNonMax extends Scenario
  case object FillGaps extends Scenario
  case object ForecastOnly extends Scenario
  case object CantGetPension extends Scenario
}

sealed trait MQPScenario
object MQPScenario {
  case object ContinueWorking extends MQPScenario
  case object CantGet extends MQPScenario
  case object CanGetWithGaps extends MQPScenario
}