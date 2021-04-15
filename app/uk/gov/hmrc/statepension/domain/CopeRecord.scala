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

package uk.gov.hmrc.statepension.domain

import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.domain.Nino
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import uk.gov.hmrc.statepension.config.AppConfig

case class CopeRecord(nino: Nino, firstLoginDate: LocalDate) {

  def defineCopePeriod(today: LocalDate, appConfig: AppConfig): CopeDatePeriod = today match {
    case td if td.isBefore(firstLoginDate.plusWeeks(appConfig.firstReturnToServiceWeeks)) => CopeDatePeriod.Initial
    case td if td.isAfter(firstLoginDate.plusWeeks(appConfig.firstReturnToServiceWeeks)) &&
      td.isBefore(firstLoginDate.plusDays(appConfig.secondReturnToServiceWeeks)) => CopeDatePeriod.Extended
    case _ => CopeDatePeriod.Expired
  }
}

object CopeRecord {
  implicit val format: Format[CopeRecord] = Json.format[CopeRecord]
}
