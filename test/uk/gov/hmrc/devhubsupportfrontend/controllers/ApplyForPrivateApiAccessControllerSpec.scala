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
import uk.gov.hmrc.devhubsupportfrontend.views.html.{ApplyForPrivateApiAccessView, SupportPageConfirmationForHoneyPotFieldView}

class ApplyForPrivateApiAccessControllerSpec extends BaseControllerSpec with WithCSRFAddToken {

  trait Setup extends SupportServiceMockModule with ThirdPartyDeveloperConnectorMockModule with UserBuilder with LocalUserIdTracker {
    val applyForPrivateApiAccessView                = app.injector.instanceOf[ApplyForPrivateApiAccessView]
    val supportPageConfirmationForHoneyPotFieldView = app.injector.instanceOf[SupportPageConfirmationForHoneyPotFieldView]

    val underTest = new ApplyForPrivateApiAccessController(
      mcc,
      SupportServiceMock.aMock,
      cookieSigner,
      mock[ErrorHandler],
      ThirdPartyDeveloperConnectorMock.aMock,
      applyForPrivateApiAccessView,
      supportPageConfirmationForHoneyPotFieldView
    )

    val supportSessionId = SupportSessionId.random
    val basicFlow        = SupportFlow(supportSessionId, SupportData.UsingAnApi.id)
    val appropriateFlow  = basicFlow.copy(subSelection = Some(SupportData.PrivateApiDocumentation.id), privateApi = Some("xxx"))

    def shouldBeRedirectedToPreviousPage(result: Future[Result]) = {
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe "/devhub-support/api/private-api"
    }

    def shouldBeRedirectedToConfirmationPage(result: Future[Result]) = {
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe "/devhub-support/confirmation"
    }
  }

  trait IsLoggedIn {
    self: Setup =>

    lazy val request = FakeRequest()
      .withSupport(underTest)(supportSessionId)
      .withUser(underTest)(sessionId)

    ThirdPartyDeveloperConnectorMock.FetchSession.succeeds()
  }

  trait NotLoggedIn {
    self: Setup =>

    lazy val request = FakeRequest()

    ThirdPartyDeveloperConnectorMock.FetchSession.fails()
  }

  "ApplyForPrivateApiAccessController" when {
    "invoke page" should {
      "render the page when flow has private api present" in new Setup with IsLoggedIn {
        SupportServiceMock.GetSupportFlow.succeeds(appropriateFlow)

        val result = addToken(underTest.page())(request)

        status(result) shouldBe OK
        contentAsString(result) should include("Your website")
      }

      "render the page when flow has no private api present" in new Setup with IsLoggedIn {
        SupportServiceMock.GetSupportFlow.succeeds(appropriateFlow.copy(privateApi = None))

        val result = addToken(underTest.page())(request)

        status(result) shouldBe OK

        contentAsString(result) should include("Which private API do you want to use?")
        contentAsString(result) should include("Full name")
        contentAsString(result) should include("Email address")
        contentAsString(result) should include("Your website")
      }

      "render the previous page when there is no flow" in new Setup with IsLoggedIn {
        SupportServiceMock.GetSupportFlow.succeeds(basicFlow)

        val result = addToken(underTest.page())(request)

        shouldBeRedirectedToPreviousPage(result)
      }
    }

    "invoke submit" should {
      "submit new valid request from form" in new Setup with IsLoggedIn {
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

      "create new support ticket when valid request with url (honeypot field) on form and user is logged in" in new Setup with IsLoggedIn {
        val formRequest = request.withFormUrlEncodedBody(
          "fullName"      -> "Bob",
          "emailAddress"  -> "bob@example.com",
          "organisation"  -> "org",
          "privateApi"    -> "my API",
          "applicationId" -> "123456",
          "url"           -> "It's a trap"
        )
        SupportServiceMock.GetSupportFlow.succeeds(appropriateFlow)
        SupportServiceMock.SubmitTicket.succeeds()

        val result = addToken(underTest.submit())(formRequest)

        shouldBeRedirectedToConfirmationPage(result)
      }

      "not create new ticket but show dummy confirmation page when url (honeypot field) on form and user not logged in" in new Setup with NotLoggedIn {
        val newRequest = request
          .withFormUrlEncodedBody(
            "fullName"      -> "Bob",
            "emailAddress"  -> "bob@example.com",
            "organisation"  -> "org",
            "privateApi"    -> "my API",
            "applicationId" -> "123456",
            "url"           -> "It's a trap"
          )
        SupportServiceMock.GetSupportFlow.succeeds(appropriateFlow)

        val result = addToken(underTest.submit())(newRequest)

        status(result) shouldBe OK
        contentAsString(result) should include("We've sent you a confirmation email")
        contentAsString(result) should include("Your request will be sent to the right team as quickly as possible")
        contentAsString(result) should include("We'll send an email to your email address whenever there's an update")
      }

      "submit invalid request returns BAD_REQUEST" in new Setup with IsLoggedIn {
        val formRequest = request.withFormUrlEncodedBody(
          "fullName"      -> "Bob",
          "emailAddress"  -> "bob@example.com",
          "applicationId" -> "123456"
        )
        SupportServiceMock.GetSupportFlow.succeeds(appropriateFlow)

        val result = addToken(underTest.submit())(formRequest)

        status(result) shouldBe BAD_REQUEST
      }

      "submit valid request but no session" in new Setup with IsLoggedIn {
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
