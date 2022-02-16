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

package uk.gov.hmrc.statepension.controllers.statepension

import com.google.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.api.controllers.{ErrorResponse, HeaderValidator}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.controllers.auth.AuthAction
import uk.gov.hmrc.statepension.controllers.{ErrorHandling, ErrorResponses, HalSupport, Links}
import uk.gov.hmrc.statepension.domain.Exclusion._
import uk.gov.hmrc.statepension.events.{StatePension, StatePensionExclusion}
import uk.gov.hmrc.statepension.services.StatePensionService

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
abstract class StatePensionController @Inject()(controllerComponents: ControllerComponents, errorHandling: ErrorHandling)
  extends BackendController(controllerComponents)
    with HeaderValidator
    with HalSupport
    with Links {

  val appConfig: AppConfig
  val statePensionService: StatePensionService
  val customAuditConnector: AuditConnector
  val authAction: AuthAction

  override lazy val context: String = appConfig.apiGatewayContext

  def get(nino: Nino): Action[AnyContent] = (authAction andThen validateAccept(acceptHeaderValidationRules)).async {
    implicit request =>
      errorHandling.errorWrapper(statePensionService.getStatement(nino).map {

        case Left(exclusion) if exclusion.exclusionReasons.contains(Dead) =>
          customAuditConnector.sendEvent(StatePensionExclusion(nino, List(Dead),
            exclusion.pensionAge, exclusion.pensionDate, exclusion.statePensionAgeUnderConsideration))
          Forbidden(Json.toJson[ErrorResponse](ErrorResponses.ExclusionDead))

        case Left(exclusion) if exclusion.exclusionReasons.contains(ManualCorrespondenceIndicator) =>
          customAuditConnector.sendEvent(StatePensionExclusion(nino, List(ManualCorrespondenceIndicator),
            exclusion.pensionAge, exclusion.pensionDate, exclusion.statePensionAgeUnderConsideration))
          Forbidden(Json.toJson[ErrorResponse](ErrorResponses.ExclusionManualCorrespondence))

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
            statePension.reducedRateElection, statePension.reducedRateElectionCurrentWeeklyAmount,
            statePension.statePensionAgeUnderConsideration))

          Ok(halResourceSelfLink(Json.toJson(statePension), statePensionHref(nino)))
      }, nino)
  }
}
