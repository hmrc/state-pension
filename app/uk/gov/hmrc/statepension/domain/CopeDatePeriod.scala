package uk.gov.hmrc.statepension.domain

trait CopeDatePeriod

object CopeDatePeriod {
  case object Initial extends CopeDatePeriod
  case object Extended extends CopeDatePeriod
  case object Expired extends CopeDatePeriod
}
