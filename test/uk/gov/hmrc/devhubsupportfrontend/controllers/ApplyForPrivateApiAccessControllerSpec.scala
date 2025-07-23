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

package uk.gov.hmrc.devhubsupportfrontend.controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.tpd.test.builders.UserBuilder
import uk.gov.hmrc.apiplatform.modules.tpd.test.utils.LocalUserIdTracker

import uk.gov.hmrc.devhubsupportfrontend.config.ErrorHandler
import uk.gov.hmrc.devhubsupportfrontend.domain.models.{SupportFlow, SupportSessionId}
import uk.gov.hmrc.devhubsupportfrontend.mocks.connectors.ThirdPartyDeveloperConnectorMockModule
import uk.gov.hmrc.devhubsupportfrontend.mocks.services.SupportServiceMockModule
import uk.gov.hmrc.devhubsupportfrontend.utils.WithCSRFAddToken
import uk.gov.hmrc.devhubsupportfrontend.utils.WithLoggedInSession._
import uk.gov.hmrc.devhubsupportfrontend.views.html.ApplyForPrivateApiAccessView

class ApplyForPrivateApiAccessControllerSpec extends BaseControllerSpec with WithCSRFAddToken {

  trait Setup extends SupportServiceMockModule with ThirdPartyDeveloperConnectorMockModule with UserBuilder with LocalUserIdTracker {
    val applyForPrivateApiAccessView = app.injector.instanceOf[ApplyForPrivateApiAccessView]

    lazy val request = FakeRequest()
      .withSupport(underTest)(supportSessionId)
      .withUser(underTest)(sessionId)

    val underTest = new ApplyForPrivateApiAccessController(
      mcc,
      SupportServiceMock.aMock,
      cookieSigner,
      mock[ErrorHandler],
      ThirdPartyDeveloperConnectorMock.aMock,
      applyForPrivateApiAccessView
    )

    val supportSessionId = SupportSessionId.random
    val basicFlow        = SupportFlow(supportSessionId, SupportData.UsingAnApi.id)
    val appropriateFlow  = basicFlow.copy(subSelection = Some(SupportData.PrivateApiDocumentation.id), privateApi = Some("xxx"))

    ThirdPartyDeveloperConnectorMock.FetchSession.succeeds()

    def shouldBeRedirectedToPreviousPage(result: Future[Result]) = {
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe "/devhub-support/api/private-api"
    }

    def shouldBeRedirectedToConfirmationPage(result: Future[Result]) = {
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe "/devhub-support/confirmation"
    }
  }

  "ApplyForPrivateApiAccessController" when {
    "invoke page" should {
      "render the page when flow has private api present" in new Setup {
        SupportServiceMock.GetSupportFlow.succeeds(appropriateFlow)

        val result = addToken(underTest.page())(request)

        status(result) shouldBe OK
      }

      "render the page when flow has no private api present" in new Setup {
        SupportServiceMock.GetSupportFlow.succeeds(appropriateFlow.copy(privateApi = None))

        val result = addToken(underTest.page())(request)

        status(result) shouldBe OK
      }

      "render the previous page when there is no flow" in new Setup {
        SupportServiceMock.GetSupportFlow.succeeds(basicFlow)

        val result = addToken(underTest.page())(request)

        shouldBeRedirectedToPreviousPage(result)
      }
    }

    "invoke submit" should {
      "submit new valid request from form" in new Setup {
        val formRequest = request.withFormUrlEncodedBody(
          "fullName"      -> "Bob",
          "emailAddress"  -> "bob@example.com",
          "organisation"  -> "org",
          "privateApi"    -> "my API",
          "applicationId" -> "123456"
        )
        SupportServiceMock.GetSupportFlow.succeeds(appropriateFlow)
        SupportServiceMock.SubmitTicket.succeeds()

        val result = addToken(underTest.submit())(formRequest)

        shouldBeRedirectedToConfirmationPage(result)
      }

      "submit invalid request returns BAD_REQUEST" in new Setup {
        val formRequest = request.withFormUrlEncodedBody(
          "fullName"      -> "Bob",
          "emailAddress"  -> "bob@example.com",
          "applicationId" -> "123456"
        )
        SupportServiceMock.GetSupportFlow.succeeds(appropriateFlow)

        val result = addToken(underTest.submit())(formRequest)

        status(result) shouldBe BAD_REQUEST
      }

      "submit valid request but no session" in new Setup {
        val formRequest = request.withFormUrlEncodedBody(
          "fullName"      -> "Bob",
          "emailAddress"  -> "bob@example.com",
          "organisation"  -> "org",
          "applicationId" -> "123456"
        )
        SupportServiceMock.GetSupportFlow.succeeds()

        val result = addToken(underTest.submit())(formRequest)

        shouldBeRedirectedToPreviousPage(result)
      }
    }
  }
}
