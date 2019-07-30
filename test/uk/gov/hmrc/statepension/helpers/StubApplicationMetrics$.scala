/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.statepension.helpers

import com.codahale.metrics.Timer
import com.codahale.metrics.Timer.Context
import com.google.inject.Inject
import com.kenshoo.play.metrics.Metrics
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.statepension.domain.Exclusion.Exclusion
import uk.gov.hmrc.statepension.domain.nps.APIType
import uk.gov.hmrc.statepension.domain.{MQPScenario, Scenario}
import uk.gov.hmrc.statepension.services.ApplicationMetrics


class StubApplicationMetrics$ @Inject()(metrics: Metrics) extends ApplicationMetrics(metrics) with MockitoSugar {
  val stubTimerContext: Context = mock[Timer.Context]
  override def startTimer(api: APIType): Context = mock[Timer.Context]
  override def incrementFailedCounter(api: APIType): Unit = {}
  override def summary(forecast: BigDecimal, current: BigDecimal, contractedOut: Boolean, forecastScenario: Scenario,
                       personalMaximum: BigDecimal, yearsToContribute: Int, mqpScenario: Option[MQPScenario],
                       starting: BigDecimal, basicStatePension:BigDecimal,  additionalStatePension: BigDecimal,
                       graduatedRetirementBenefit:BigDecimal,grossStatePension:BigDecimal, rebateDerivedAmount:BigDecimal,
                       reducedRateElection: Boolean,reducedRateElectionCurrentWeeklyAmount:Option[BigDecimal],
                       abroadAutoCredit: Boolean, statePensionAgeUnderConsideration: Boolean): Unit = {}
  override def exclusion(exclusion: Exclusion): Unit = {}
}
