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

package uk.gov.hmrc.statepension.services

import com.codahale.metrics.Timer.Context
import com.codahale.metrics.{Counter, Histogram, Timer}
import com.kenshoo.play.metrics.MetricsRegistry
import uk.gov.hmrc.statepension.domain.Exclusion.Exclusion
import uk.gov.hmrc.statepension.domain.nps.APIType
import uk.gov.hmrc.statepension.domain.{Exclusion, MQPScenario, Scenario}

trait Metrics {
  def startTimer(api: APIType): Timer.Context
  def incrementFailedCounter(api: APIType): Unit

  def summary(forecast: BigDecimal, current: BigDecimal, contractedOut: Boolean, forecastScenario: Scenario,
              personalMaximum: BigDecimal, yearsToContribute: Int, mqpScenario: Option[MQPScenario]): Unit

  def exclusion(exclusion: Exclusion): Unit
}

object Metrics extends Metrics {

  val timers: Map[APIType, Timer] = Map(
    APIType.Summary -> MetricsRegistry.defaultRegistry.timer("summary-response-timer"),
    APIType.NIRecord -> MetricsRegistry.defaultRegistry.timer("nirecord-response-timer"),
    APIType.Liabilities -> MetricsRegistry.defaultRegistry.timer("liabilities-response-timer"),
    APIType.CitizenDetails -> MetricsRegistry.defaultRegistry.timer("citizen-details-timer")
  )

  val failedCounters: Map[APIType, Counter] = Map(
    APIType.Summary -> MetricsRegistry.defaultRegistry.counter("summary-failed-counter"),
    APIType.NIRecord -> MetricsRegistry.defaultRegistry.counter("nirecord-failed-counter"),
    APIType.Liabilities -> MetricsRegistry.defaultRegistry.counter("liabilities-failed-counter")
  )

  override def startTimer(api: APIType): Context = timers(api).time()
  override def incrementFailedCounter(api: APIType): Unit = failedCounters(api).inc()

  val forecastScenarioMeters: Map[Scenario, Counter] = Map(
    Scenario.Reached -> MetricsRegistry.defaultRegistry.counter("forecastscenario-reached"),
    Scenario.ContinueWorkingMax -> MetricsRegistry.defaultRegistry.counter("forecastscenario-continueworkingmax"),
    Scenario.ContinueWorkingNonMax -> MetricsRegistry.defaultRegistry.counter("forecastscenario-continueworkingnonmax"),
    Scenario.FillGaps -> MetricsRegistry.defaultRegistry.counter("forecastscenario-fillgaps"),
    Scenario.ForecastOnly -> MetricsRegistry.defaultRegistry.counter("forecastscenario-forecastonly"),
    Scenario.CantGetPension -> MetricsRegistry.defaultRegistry.counter("forecastscenario-cantgetpension")
  )

  val mqpScenarioMeters: Map[MQPScenario, Counter] = Map(
    MQPScenario.CantGet -> MetricsRegistry.defaultRegistry.counter("mqpscenario-cantget"),
    MQPScenario.ContinueWorking -> MetricsRegistry.defaultRegistry.counter("mqpscenario-continueworking"),
    MQPScenario.CanGetWithGaps -> MetricsRegistry.defaultRegistry.counter("mqpscenario-cangetwithgaps")
  )

  val currentAmountMeter: Histogram = MetricsRegistry.defaultRegistry.histogram("current-amount")
  val forecastAmountMeter: Histogram = MetricsRegistry.defaultRegistry.histogram("forecast-amount")
  val personalMaxAmountMeter: Histogram = MetricsRegistry.defaultRegistry.histogram("personal-maximum-amount")
  val yearsNeededToContribute: Histogram = MetricsRegistry.defaultRegistry.histogram("years-needed-to-contribute")
  val contractedOutMeter: Counter = MetricsRegistry.defaultRegistry.counter("contracted-out")
  val notContractedOutMeter: Counter = MetricsRegistry.defaultRegistry.counter("not-contracted-out")

  override def summary(forecast: BigDecimal, current: BigDecimal, contractedOut: Boolean,
                       forecastScenario: Scenario, personalMaximum: BigDecimal, yearsToContribute: Int,
                       mqpScenario : Option[MQPScenario]): Unit = {
    forecastAmountMeter.update(forecast.toInt)
    currentAmountMeter.update(current.toInt)
    personalMaxAmountMeter.update(personalMaximum.toInt)
    forecastScenarioMeters(forecastScenario).inc()
    mqpScenario.foreach(mqpScenarioMeters(_).inc())
    yearsNeededToContribute.update(yearsToContribute)
    if(contractedOut) contractedOutMeter.inc() else notContractedOutMeter.inc()
  }


  val exclusionMeters: Map[Exclusion, Counter] = Map(
    Exclusion.Abroad -> MetricsRegistry.defaultRegistry.counter("exclusion-abroad"),
    Exclusion.MarriedWomenReducedRateElection -> MetricsRegistry.defaultRegistry.counter("exclusion-mwrre"),
    Exclusion.Dead -> MetricsRegistry.defaultRegistry.counter("exclusion-dead"),
    Exclusion.IsleOfMan -> MetricsRegistry.defaultRegistry.counter("exclusion-isle-of-man"),
    Exclusion.AmountDissonance -> MetricsRegistry.defaultRegistry.counter("amount-dissonance"),
    Exclusion.PostStatePensionAge -> MetricsRegistry.defaultRegistry.counter("exclusion-post-spa"),
    Exclusion.ManualCorrespondenceIndicator -> MetricsRegistry.defaultRegistry.counter("exclusion-manual-correspondence")
  )

  override def exclusion(exclusion: Exclusion): Unit = exclusionMeters(exclusion).inc()
}