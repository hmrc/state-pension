package uk.gov.hmrc.statepension.domain

import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.domain.Nino

case class CopeMongo(nino: Nino, firstLoginDate: LocalDate)

object CopeMongo {
  implicit val format: Format[CopeMongo] = Json.format[CopeMongo]
}
