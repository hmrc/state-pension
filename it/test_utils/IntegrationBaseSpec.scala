package test_utils

import java.util.UUID

import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.domain.{Generator, Nino}

import scala.util.Random

trait IntegrationBaseSpec extends WordSpec
  with MustMatchers
  with MockitoSugar
  with GuiceOneAppPerSuite
  with WireMockHelper {

  def generateNino: Nino = new Generator(new Random).nextNino

  def generateUUId: UUID = UUID.randomUUID()
}
