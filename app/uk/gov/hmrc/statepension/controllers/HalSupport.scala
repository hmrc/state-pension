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

package uk.gov.hmrc.statepension.controllers

import play.api.hal._
import play.api.libs.json._
import play.api.mvc.Result

trait HalSupport {

  def halResource(jsValue: JsValue, links: Set[HalLink]): HalResource = {
    val halState = Hal.state(jsValue)
    links.foldLeft(halState)((res, link) => res ++ link)
  }

  def halResourceSelfLink(value: JsValue, self: String): HalResource = {
    halResource(
      value,
      Set(HalLink("self", self))
    )
  }

  // scalastyle:off method.name
  def Ok(hal: HalResource): Result = {
    play.api.mvc.Results.Ok(Json.toJson(hal)).withHeaders("Content-Type" -> "application/hal+json")
  }

}
