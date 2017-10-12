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
import uk.gov.hmrc.statepension.domain.Exclusion.Exclusion
import uk.gov.hmrc.statepension.domain.nps.APIType
import uk.gov.hmrc.statepension.domain.{Exclusion, MQPScenario, OldRules, Scenario}
import uk.gov.hmrc.play.graphite.MicroserviceMetrics

trait Metrics {
  def startTimer(api: APIType): Timer.Context
  def incrementFailedCounter(api: APIType): Unit

  def summary(forecast: BigDecimal, current: BigDecimal, contractedOut: Boolean, forecastScenario: Scenario,
              personalMaximum: BigDecimal, yearsToContribute: Int, mqpScenario: Option[MQPScenario],
              starting: BigDecimal, basicStatePension:BigDecimal,  additionalStatePension: BigDecimal,
              graduatedRetirementBenefit:BigDecimal,grossStatePension:BigDecimal, rebateDerivedAmount:BigDecimal,
              reducedRateElection: Boolean): Unit

  def exclusion(exclusion: Exclusion): Unit
}

object Metrics extends Metrics with MicroserviceMetrics {

  val timers: Map[APIType, Timer] = Map(
    APIType.Summary -> metrics.defaultRegistry.timer("summary-response-timer"),
    APIType.NIRecord -> metrics.defaultRegistry.timer("nirecord-response-timer"),
    APIType.Liabilities -> metrics.defaultRegistry.timer("liabilities-response-timer"),
    APIType.CitizenDetails -> metrics.defaultRegistry.timer("citizen-details-timer")
  )

  val failedCounters: Map[APIType, Counter] = Map(
    APIType.Summary -> metrics.defaultRegistry.counter("summary-failed-counter"),
    APIType.NIRecord -> metrics.defaultRegistry.counter("nirecord-failed-counter"),
    APIType.Liabilities -> metrics.defaultRegistry.counter("liabilities-failed-counter")
  )

  override def startTimer(api: APIType): Context = timers(api).time()
  override def incrementFailedCounter(api: APIType): Unit = failedCounters(api).inc()

  val forecastScenarioMeters: Map[Scenario, Counter] = Map(
    Scenario.Reached -> metrics.defaultRegistry.counter("forecastscenario-reached"),
    Scenario.ContinueWorkingMax -> metrics.defaultRegistry.counter("forecastscenario-continueworkingmax"),
    Scenario.ContinueWorkingNonMax -> metrics.defaultRegistry.counter("forecastscenario-continueworkingnonmax"),
    Scenario.FillGaps -> metrics.defaultRegistry.counter("forecastscenario-fillgaps"),
    Scenario.ForecastOnly -> metrics.defaultRegistry.counter("forecastscenario-forecastonly"),
    Scenario.CantGetPension -> metrics.defaultRegistry.counter("forecastscenario-cantgetpension")
  )

  val mqpScenarioMeters: Map[MQPScenario, Counter] = Map(
    MQPScenario.CantGet -> metrics.defaultRegistry.counter("mqpscenario-cantget"),
    MQPScenario.ContinueWorking -> metrics.defaultRegistry.counter("mqpscenario-continueworking"),
    MQPScenario.CanGetWithGaps -> metrics.defaultRegistry.counter("mqpscenario-cangetwithgaps")
  )

  val currentAmountMeter: Histogram = metrics.defaultRegistry.histogram("current-amount")
  val forecastAmountMeter: Histogram = metrics.defaultRegistry.histogram("forecast-amount")
  val personalMaxAmountMeter: Histogram = metrics.defaultRegistry.histogram("personal-maximum-amount")
  val yearsNeededToContribute: Histogram = metrics.defaultRegistry.histogram("years-needed-to-contribute")
  val contractedOutMeter: Counter = metrics.defaultRegistry.counter("contracted-out")
  val notContractedOutMeter: Counter = metrics.defaultRegistry.counter("not-contracted-out")
  val startingAmount: Histogram=metrics.defaultRegistry.histogram("starting-amount")
  val oldRulesBasicStatePension: Histogram=metrics.defaultRegistry.histogram("oldrules-basic-state-pension")
  val oldRulesAdditionalStatePension: Histogram=metrics.defaultRegistry.histogram("oldrules-additional-state-pension")
  val oldRulesGraduatedRetirementBenefit: Histogram=metrics.defaultRegistry.histogram("oldrules-graduated-retirement-benefit")
  val newRulesGrossStatePension: Histogram=metrics.defaultRegistry.histogram("newrules-gross-state-pension")
  val newRulesRebateDerivedAmount: Histogram=metrics.defaultRegistry.histogram("oldrules-rebate-derived-amount")

  override def summary(forecast: BigDecimal, current: BigDecimal, contractedOut: Boolean,
                       forecastScenario: Scenario, personalMaximum: BigDecimal, yearsToContribute: Int,
                       mqpScenario : Option[MQPScenario], starting: BigDecimal, basicStatePension:BigDecimal,
                       additionalStatePension: BigDecimal, graduatedRetirementBenefit:BigDecimal,
                       grossStatePension:BigDecimal, rebateDerivedAmount:BigDecimal,
                       reducedRateElection: Boolean): Unit = {
    startingAmount.update(starting.toInt)
    oldRulesBasicStatePension.update(basicStatePension.toInt)
    oldRulesAdditionalStatePension.update(additionalStatePension.toInt)
    oldRulesGraduatedRetirementBenefit.update(graduatedRetirementBenefit.toInt)
    newRulesGrossStatePension.update(grossStatePension.toInt)
    newRulesRebateDerivedAmount.update(rebateDerivedAmount.toInt)
    if(reducedRateElection) metrics.defaultRegistry.counter("exclusion-mwrre").inc()

    forecastAmountMeter.update(forecast.toInt)
    currentAmountMeter.update(current.toInt)
    personalMaxAmountMeter.update(personalMaximum.toInt)
    forecastScenarioMeters(forecastScenario).inc()
    mqpScenario.foreach(mqpScenarioMeters(_).inc())
    yearsNeededToContribute.update(yearsToContribute)
    if(contractedOut) contractedOutMeter.inc() else notContractedOutMeter.inc()
  }

  val exclusionMeters: Map[Exclusion, Counter] = Map(
    Exclusion.Abroad -> metrics.defaultRegistry.counter("exclusion-abroad"),
    Exclusion.Dead -> metrics.defaultRegistry.counter("exclusion-dead"),
    Exclusion.IsleOfMan -> metrics.defaultRegistry.counter("exclusion-isle-of-man"),
    Exclusion.AmountDissonance -> metrics.defaultRegistry.counter("amount-dissonance"),
    Exclusion.PostStatePensionAge -> metrics.defaultRegistry.counter("exclusion-post-spa"),
    Exclusion.ManualCorrespondenceIndicator -> metrics.defaultRegistry.counter("exclusion-manual-correspondence")
  )

  override def exclusion(exclusion: Exclusion): Unit = exclusionMeters(exclusion).inc()

}
