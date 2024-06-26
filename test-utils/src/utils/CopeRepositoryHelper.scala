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

package utils

import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.statepension.repositories.CopeProcessingRepository

import scala.concurrent.Future

trait CopeRepositoryHelper extends StatePensionBaseSpec { self: GuiceOneAppPerSuite =>

  val mockCopeRepository: CopeProcessingRepository = mock[CopeProcessingRepository]

  when(mockCopeRepository.find(any())).thenReturn(Future.successful(None))
  when(mockCopeRepository.insert(any())).thenReturn(Future.successful(Done))
  when(mockCopeRepository.update(any(), any(), any())).thenReturn(Future.successful(None))

  val fakeAppWithOverrides: GuiceApplicationBuilder =
    new GuiceApplicationBuilder().overrides(
      bind[CopeProcessingRepository].to(mockCopeRepository)
    )

  override def fakeApplication(): Application = fakeAppWithOverrides.build()
}
