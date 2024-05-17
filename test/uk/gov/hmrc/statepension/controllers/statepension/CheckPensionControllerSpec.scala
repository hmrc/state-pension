/*
 * Copyright 2024 HM Revenue & Customs
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

import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc._
import play.api.test.{FakeRequest, Helpers, Injecting}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.connectors.{DesConnector, ProxyCacheConnector}
import uk.gov.hmrc.statepension.controllers.ErrorHandling
import uk.gov.hmrc.statepension.controllers.auth.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.statepension.repositories.CopeProcessingRepository
import uk.gov.hmrc.statepension.services._
import utils.{CopeRepositoryHelper, StatePensionBaseSpec}

import scala.concurrent.{ExecutionContext, Future}


class CheckPensionControllerSpec extends StatePensionBaseSpec with GuiceOneAppPerSuite with Injecting with CopeRepositoryHelper {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  val mockDesConnector: DesConnector = mock[DesConnector]
  val mockProxyCacheConnector: ProxyCacheConnector = mock[ProxyCacheConnector]
  val mockForecastingService: ForecastingService = mock[ForecastingService]
  val mockRateService: RateService = mock[RateService]
  val mockMetrics: ApplicationMetrics = mock[ApplicationMetrics]
  val mockCustomAuditConnector: AuditConnector = mock[AuditConnector]
  val mockFeatureFlagService: FeatureFlagService = mock[FeatureFlagService]
  val mockCitizenDetailsService: CitizenDetailsService = mock[CitizenDetailsService]
  val mockAppConfig: AppConfig = mock[AppConfig]
  val mockCopeProcessingRepository = mock[CopeProcessingRepository]

  val controllerComponents: ControllerComponents = Helpers.stubControllerComponents()
  val fakeAuthAction: AuthAction = inject[FakeAuthAction]
  val fakeErrorHandling: ErrorHandling = inject[ErrorHandling]
  val parser = mock[BodyParsers.Default]

  val mockCheckPensionService = new CheckPensionService(
    mockDesConnector,
    mockProxyCacheConnector,
    mockForecastingService,
    mockRateService,
    mockMetrics,
    mockCustomAuditConnector,
    ec,
    mockFeatureFlagService,
    mockCitizenDetailsService
  )

  val controller = new CheckPensionController(
    fakeAuthAction,
    mockAppConfig,
    mockCheckPensionService,
    mockCustomAuditConnector,
    controllerComponents,
    parser,
    ec,
    fakeErrorHandling,
    mockCopeProcessingRepository
  )

  val nino: Nino = Nino("AB123456C")
  val emptyRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val emptyRequestWithHeader: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")

  "CheckPensionController" should {
    "return OK" when {
      "calling endpointUrl with valid Nino" in {
//        val nino = Nino("AB123456C")
//        val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/").withBody(AnyContentAsEmpty)
//
//        ???

      }
    }


}
