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

package uk.gov.hmrc.devhubsupportfrontend.controllers

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.tpd.test.utils.LocalUserIdTracker

import uk.gov.hmrc.devhubsupportfrontend.config.ErrorHandler
import uk.gov.hmrc.devhubsupportfrontend.domain.models.SupportSessionId
import uk.gov.hmrc.devhubsupportfrontend.mocks.connectors.ThirdPartyDeveloperConnectorMockModule
import uk.gov.hmrc.devhubsupportfrontend.mocks.services.SupportServiceMockModule
import uk.gov.hmrc.devhubsupportfrontend.utils.WithCSRFAddToken
import uk.gov.hmrc.devhubsupportfrontend.utils.WithLoggedInSession._

class GoToSupportFlowControllerSpec extends BaseControllerSpec with WithCSRFAddToken {

  trait Setup extends SupportServiceMockModule with ThirdPartyDeveloperConnectorMockModule with LocalUserIdTracker {

    val supportSessionId = SupportSessionId.random

    lazy val request = FakeRequest()
      .withSupport(underTest)(supportSessionId)
      .withUser(underTest)(sessionId)

    val underTest = new GoToSupportFlowController(
      mcc,
      cookieSigner,
      mock[ErrorHandler],
      ThirdPartyDeveloperConnectorMock.aMock,
      SupportServiceMock.aMock
    )

    ThirdPartyDeveloperConnectorMock.FetchSession.succeeds()

  }

  "GoToSupportFlowController" when {
    "goToFlow" should {
      "redirect to the 'Accessing private API documentation' page when valid flow id passed in" in new Setup {

        val result = addToken(underTest.goToFlow(SupportData.PrivateApiDocumentation.id))(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/devhub-support/api/private-api/apply")
      }

      "return BAD_REQUEST when any other support flow is set on the form" in new Setup {

        val result = addToken(underTest.goToFlow(SupportData.FindingAnApi.id))(request)

        status(result) shouldBe BAD_REQUEST
        redirectLocation(result) shouldBe None
      }

      "return BAD_REQUEST when flow id is empty" in new Setup {

        val result = addToken(underTest.goToFlow(""))(request)

        status(result) shouldBe BAD_REQUEST
        redirectLocation(result) shouldBe None
      }

    }
  }
}
