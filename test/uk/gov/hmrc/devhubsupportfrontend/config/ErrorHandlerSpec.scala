/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.devhubsupportfrontend.config

import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import uk.gov.hmrc.mongo.play.PlayMongoModule

import uk.gov.hmrc.devhubsupportfrontend.mocks.repositories.SupportFlowRepositoryMockModule
import uk.gov.hmrc.devhubsupportfrontend.repositories.SupportFlowRepository
import uk.gov.hmrc.devhubsupportfrontend.utils.AsyncHmrcSpec

class ErrorHandlerSpec extends AsyncHmrcSpec
    with GuiceOneAppPerSuite
    with SupportFlowRepositoryMockModule {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .disable[PlayMongoModule]
      .overrides(bind[SupportFlowRepository].toInstance(SupportFlowRepositoryMock.aMock))
      .build()

  private val fakeRequest = FakeRequest("GET", "/")

  private val handler = app.injector.instanceOf[ErrorHandler]

  "standardErrorTemplate" should {
    "render HTML" in {
      val html = await(handler.standardErrorTemplate("title", "heading", "message")(using fakeRequest))
      html.contentType shouldBe "text/html"
    }
  }

}
