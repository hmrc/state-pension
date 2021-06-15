package test_utils

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse

trait ResponseHelpers {
  def badGateway(): ResponseDefinitionBuilder = {
    aResponse().withStatus(502)
  }

  def closedCopeWorkItem(): ResponseDefinitionBuilder = {
    aResponse().withStatus(422).withBody("CLOSED_COPE_WORK_ITEM")
  }

  def gatewayTimeout(): ResponseDefinitionBuilder = {
    aResponse().withStatus(504)
  }

  def noOpenCopeWorkItem(): ResponseDefinitionBuilder = {
    aResponse().withStatus(422).withBody("NO_OPEN_COPE_WORK_ITEM")
  }
}
