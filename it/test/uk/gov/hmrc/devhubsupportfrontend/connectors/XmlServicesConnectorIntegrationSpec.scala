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

package uk.gov.hmrc.devhubsupportfrontend.connectors

import org.scalatest.EitherValues
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application => PlayApplication, Configuration, Mode}
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.devhubsupportfrontend.stubs.XmlServicesStub
import uk.gov.hmrc.devhubsupportfrontend.utils.WireMockExtensions

class XmlServicesConnectorIntegrationSpec
    extends BaseConnectorIntegrationSpec
    with GuiceOneAppPerSuite
    with WireMockExtensions
    with FixedClock
    with EitherValues {

  private val stubConfig = Configuration(
    "microservice.services.api-platform-xml-services.port" -> stubPort
  )

  override def fakeApplication(): PlayApplication =
    GuiceApplicationBuilder()
      .configure(stubConfig)
      .overrides(bind[ConnectorMetrics].to[NoopConnectorMetrics])
      .in(Mode.Test)
      .build()

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val underTest                  = app.injector.instanceOf[XmlServicesConnector]

  }

  "fetchApiDefinitionsVisibleToUser" should {
    "retrieve a list of service a user can see" in new Setup {
      val serviceName = ServiceName("api1")
      val name        = "API 1"
      XmlServicesStub.FetchAllXmlApis.succeeds(
        s"""[{ "name": "$name", "serviceName": "$serviceName", "context": "context", "description": "description", "categories": ["AGENTS", "VAT"] }]"""
      )

      val result: Seq[XmlApi] = await(underTest.fetchAllXmlApis())

      result.head.serviceName shouldBe serviceName
      result.head.name shouldBe name
    }

    "fail on Upstream5xxResponse when the call return a 500" in new Setup {
      XmlServicesStub.FetchAllXmlApis.fails()

      intercept[UpstreamErrorResponse] {
        await(underTest.fetchAllXmlApis())
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
