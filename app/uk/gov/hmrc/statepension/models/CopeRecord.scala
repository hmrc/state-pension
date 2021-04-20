/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.statepension.models

import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json}
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import uk.gov.hmrc.statepension.config.AppConfig
import uk.gov.hmrc.statepension.controllers.HashedNino

case class CopeRecord(
  hashedNino: HashedNino,
  firstLoginDate: LocalDate,
  copeAvailableDate: LocalDate
) {
  def defineCopePeriod(appConfig: AppConfig): CopeDatePeriod = firstLoginDate match {
    case initialDate if (initialDate.plusWeeks(appConfig.returnToServiceWeeks).isAfter(copeAvailableDate)) => CopeDatePeriod.Extended
    case _ => CopeDatePeriod.Initial
  }
}

object CopeRecord {
  implicit val format: Format[CopeRecord] = Json.format[CopeRecord]
}
