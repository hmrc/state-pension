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

package uk.gov.hmrc.statepension.services

import com.codahale.metrics.Timer.Context
import com.codahale.metrics.{Counter, Histogram, Timer}
import com.google.inject.Inject
import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.statepension.domain.nps.APIType
import uk.gov.hmrc.statepension.domain.Exclusion._
import uk.gov.hmrc.statepension.domain.{Exclusion, MQPScenario, Scenario}

class ApplicationMetrics @Inject()(metrics: Metrics) {

  val timers: APIType => Timer = {
    case APIType.Summary        => metrics.defaultRegistry.timer("summary-response-timer")
    case APIType.NIRecord       => metrics.defaultRegistry.timer("nirecord-response-timer")
    case APIType.Liabilities    => metrics.defaultRegistry.timer("liabilities-response-timer")
    case APIType.CitizenDetails => metrics.defaultRegistry.timer("citizen-details-timer")
    case APIType.IfSummary      => metrics.defaultRegistry.timer("if-summary-response-timer")
    case APIType.IfNIRecord     => metrics.defaultRegistry.timer("if-nirecord-response-timer")
    case APIType.IfLiabilities  => metrics.defaultRegistry.timer("if-liabilities-response-timer")
    case APIType.ProxyCache     => metrics.defaultRegistry.timer("proxy-cache-response-timer")
  }

  val failedCounters: APIType => Counter = {
    case APIType.Summary        => metrics.defaultRegistry.counter("summary-failed-counter")
    case APIType.NIRecord       => metrics.defaultRegistry.counter("nirecord-failed-counter")
    case APIType.Liabilities    => metrics.defaultRegistry.counter("liabilities-failed-counter")
    case APIType.CitizenDetails => metrics.defaultRegistry.counter("citizen-details-failed-counter")
    case APIType.IfSummary      => metrics.defaultRegistry.counter("if-summary-failed-counter")
    case APIType.IfNIRecord     => metrics.defaultRegistry.counter("if-nirecord-failed-counter")
    case APIType.IfLiabilities  => metrics.defaultRegistry.counter("if-liabilities-failed-counter")
    case APIType.ProxyCache     => metrics.defaultRegistry.counter("proxy-cache-failed-counter")
  }

  def startTimer(api: APIType): Context = timers(api).time()
  def incrementFailedCounter(api: APIType): Unit = failedCounters(api).inc()

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
  val rreCurrentWeeklyAmount:Histogram=metrics.defaultRegistry.histogram("reduced-rate-election-current-weekly-amount")
  val statePensionAgeUnderConsiderationMeter: Counter = metrics.defaultRegistry.counter("state-pension-age-under-consideration")

  def summary(forecast: BigDecimal, current: BigDecimal, contractedOut: Boolean,
                       forecastScenario: Scenario, personalMaximum: BigDecimal, yearsToContribute: Int,
                       mqpScenario : Option[MQPScenario], starting: BigDecimal, basicStatePension:BigDecimal,
                       additionalStatePension: BigDecimal, graduatedRetirementBenefit:BigDecimal,
                       grossStatePension:BigDecimal, rebateDerivedAmount:BigDecimal,
                       reducedRateElection: Boolean,reducedRateElectionCurrentWeeklyAmount:Option[BigDecimal],
                       statePensionAgeUnderConsideration: Boolean): Unit = {
    startingAmount.update(starting.toInt)
    oldRulesBasicStatePension.update(basicStatePension.toInt)
    oldRulesAdditionalStatePension.update(additionalStatePension.toInt)
    oldRulesGraduatedRetirementBenefit.update(graduatedRetirementBenefit.toInt)
    newRulesGrossStatePension.update(grossStatePension.toInt)
    newRulesRebateDerivedAmount.update(rebateDerivedAmount.toInt)
    if(reducedRateElection) {
      metrics.defaultRegistry.counter("exclusion-mwrre").inc()
      rreCurrentWeeklyAmount.update(reducedRateElectionCurrentWeeklyAmount.getOrElse[BigDecimal](0).toInt)
    }
    forecastAmountMeter.update(forecast.toInt)
    currentAmountMeter.update(current.toInt)
    personalMaxAmountMeter.update(personalMaximum.toInt)
    forecastScenarioMeters(forecastScenario).inc()
    mqpScenario.foreach(mqpScenarioMeters(_).inc())
    yearsNeededToContribute.update(yearsToContribute)
    if(contractedOut) contractedOutMeter.inc() else notContractedOutMeter.inc()
    if(statePensionAgeUnderConsideration) statePensionAgeUnderConsiderationMeter.inc()
  }

  val exclusionMeters: Map[Exclusion, Counter] = Map(
    Dead -> metrics.defaultRegistry.counter("exclusion-dead"),
    IsleOfMan -> metrics.defaultRegistry.counter("exclusion-isle-of-man"),
    AmountDissonance -> metrics.defaultRegistry.counter("amount-dissonance"),
    PostStatePensionAge -> metrics.defaultRegistry.counter("exclusion-post-spa"),
    ManualCorrespondenceIndicator -> metrics.defaultRegistry.counter("exclusion-manual-correspondence")
  )

  def exclusion(exclusion: Exclusion): Unit = exclusionMeters(exclusion).inc()

}
