/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.statepension.domain.nps

sealed trait APIType
object APIType {
  case object Summary extends APIType
  case object NIRecord extends APIType
  case object Liabilities extends APIType
  case object CitizenDetails extends APIType

  case object IfSummary extends APIType
  case object IfNIRecord extends APIType
  case object IfLiabilities extends APIType
}
