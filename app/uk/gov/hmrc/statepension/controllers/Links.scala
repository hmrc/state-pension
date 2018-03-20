/*
 * Copyright 2018 HM Revenue & Customs
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

import uk.gov.hmrc.domain.Nino

trait Links {

  val context: String

  private def createLink(endpointUrl: String) = if(context.isEmpty) endpointUrl else s"/$context$endpointUrl"

  def statePensionHref(nino: Nino): String =
    createLink(uk.gov.hmrc.statepension.controllers.live.routes.StatePensionController.get(nino).url)

}
