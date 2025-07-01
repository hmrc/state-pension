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

import java.time.{LocalDate, LocalDateTime, ZoneId}

trait TaxYearResolver {

  lazy val now: () => LocalDateTime = ???

  private val ukTime : ZoneId = ZoneId.of("Europe/London")

  def taxYearFor(dateToResolve: LocalDate): Int = {
    val year = dateToResolve.getYear

    if (dateToResolve.isBefore(LocalDate.of(year, 4, 6)))
      year - 1
    else
      year
  }

  def fallsInThisTaxYear(currentDate: LocalDate): Boolean = {
    val earliestDateForCurrentTaxYear = LocalDate.of(taxYearFor(now().toLocalDate), 4, 6)
    println(s"$earliestDateForCurrentTaxYear \n\n\n\n")
    earliestDateForCurrentTaxYear.isBefore(currentDate) || earliestDateForCurrentTaxYear.isEqual(currentDate)
  }

  def currentTaxYear: Int = taxYearFor(now().atZone(ukTime).toLocalDate)

  def currentTaxYearYearsRange = currentTaxYear to currentTaxYear + 1

  def startOfTaxYear(year: Int) = LocalDate.of(year, 4, 6)

  def endOfTaxYear(year: Int) = LocalDate.of(year + 1, 4, 5)

  def startOfCurrentTaxYear = startOfTaxYear(currentTaxYear)

}


object TaxYearResolver extends TaxYearResolver {
  override lazy val now: () => LocalDateTime = () => LocalDateTime.now.truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
}