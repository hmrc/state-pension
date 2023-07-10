package test_utils

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.domain.{Generator, Nino}
import utils.{UnitSpec, WireMockHelper}

import java.util.UUID
import scala.util.Random

trait IntegrationBaseSpec extends UnitSpec
  with GuiceOneAppPerSuite
  with WireMockHelper {

  def generateNino: Nino = new Generator(new Random).nextNino

  def generateUUId: UUID = UUID.randomUUID()
}
