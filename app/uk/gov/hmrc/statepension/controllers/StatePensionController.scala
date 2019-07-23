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

package uk.gov.hmrc.statepension.controllers

import com.google.inject.Inject
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.statepension.config.AppContext
import uk.gov.hmrc.statepension.connectors.CustomAuditConnector
import uk.gov.hmrc.statepension.domain.Exclusion
import uk.gov.hmrc.statepension.services.StatePensionService
import uk.gov.hmrc.statepension.events.{StatePension, StatePensionExclusion}

class StatePensionController @Inject()(val statePensionService: StatePensionService,
                                       val customAuditConnector: CustomAuditConnector)
  extends BaseController
    with HeaderValidator
    with ErrorHandling
    with HalSupport
    with Links {

  override val app: String = "State-Pension"
  override val context: String = AppContext.apiGatewayContext

  def get(nino: Nino): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      errorWrapper(statePensionService.getStatement(nino).map {

        case Left(exclusion) if exclusion.exclusionReasons.contains(Exclusion.Dead) =>
          customAuditConnector.sendEvent(StatePensionExclusion(nino, List(Exclusion.Dead),
            exclusion.pensionAge, exclusion.pensionDate, exclusion.statePensionAgeUnderConsideration))
          Forbidden(Json.toJson(ErrorResponses.ExclusionDead))

        case Left(exclusion) if exclusion.exclusionReasons.contains(Exclusion.ManualCorrespondenceIndicator) =>
          customAuditConnector.sendEvent(StatePensionExclusion(nino, List(Exclusion.ManualCorrespondenceIndicator),
            exclusion.pensionAge, exclusion.pensionDate, exclusion.statePensionAgeUnderConsideration))
          Forbidden(Json.toJson(ErrorResponses.ExclusionManualCorrespondence))

        case Left(exclusion) =>
          customAuditConnector.sendEvent(StatePensionExclusion(nino, exclusion.exclusionReasons,
            exclusion.pensionAge, exclusion.pensionDate, exclusion.statePensionAgeUnderConsideration))
          Ok(halResourceSelfLink(Json.toJson(exclusion), statePensionHref(nino)))

        case Right(statePension) =>
          customAuditConnector.sendEvent(StatePension(nino, statePension.earningsIncludedUpTo, statePension.amounts,
            statePension.pensionAge, statePension.pensionDate, statePension.finalRelevantYear, statePension.numberOfQualifyingYears,
            statePension.pensionSharingOrder, statePension.currentFullWeeklyPensionAmount,
            statePension.amounts.starting.weeklyAmount, statePension.amounts.oldRules.basicStatePension,
            statePension.amounts.oldRules.additionalStatePension, statePension.amounts.oldRules.graduatedRetirementBenefit,
            statePension.amounts.newRules.grossStatePension, statePension.amounts.newRules.rebateDerivedAmount,
            statePension.reducedRateElection, statePension.reducedRateElectionCurrentWeeklyAmount, statePension.abroadAutoCredit,
            statePension.statePensionAgeUnderConsideration))

          Ok(halResourceSelfLink(Json.toJson(statePension), statePensionHref(nino)))
      })
  }
}
