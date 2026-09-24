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

import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider
import uk.gov.hmrc.apiplatform.modules.tpd.test.builders.UserBuilder
import uk.gov.hmrc.apiplatform.modules.tpd.test.utils.LocalUserIdTracker

import uk.gov.hmrc.devhubsupportfrontend.config.ErrorHandler
import uk.gov.hmrc.devhubsupportfrontend.domain.models.SupportSessionId
import uk.gov.hmrc.devhubsupportfrontend.mocks.connectors.ThirdPartyDeveloperConnectorMockModule
import uk.gov.hmrc.devhubsupportfrontend.mocks.services.SupportServiceMockModule
import uk.gov.hmrc.devhubsupportfrontend.utils.WithCSRFAddToken
import uk.gov.hmrc.devhubsupportfrontend.utils.WithLoggedInSession._
import uk.gov.hmrc.devhubsupportfrontend.utils.WithSupportSession._
import uk.gov.hmrc.devhubsupportfrontend.views.html.{FeedbackConfirmationView, FeedbackView}

class FeedbackControllerSpec extends BaseControllerSpec with WithCSRFAddToken {

  trait Setup extends SupportServiceMockModule with ThirdPartyDeveloperConnectorMockModule with UserBuilder with LocalUserIdTracker {
    val feedbackView             = app.injector.instanceOf[FeedbackView]
    val feedbackConfirmationView = app.injector.instanceOf[FeedbackConfirmationView]

    val underTest = new FeedbackController(
      mcc,
      cookieSigner,
      mock[ErrorHandler],
      ThirdPartyDeveloperConnectorMock.aMock,
      SupportServiceMock.aMock,
      feedbackView,
      feedbackConfirmationView
    )

    val sessionParams: Seq[(String, String)] = Seq("csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken)
    val supportSessionId                     = SupportSessionId.random

    val fullName: String         = "John Doe"
    val emailAddress: String     = "something@example.com"
    val whatWereYouDoing: String = "I was trying to check the status of my application"
    val feedback: String         = "Couldn't figure it out"
    val honeypotUrl              = "It's a trap"
  }

  trait IsLoggedIn {
    self: Setup =>

    lazy val request = FakeRequest()
      .withUser(underTest)(sessionId)
      .withSession(sessionParams: _*)

    ThirdPartyDeveloperConnectorMock.FetchSession.succeeds()
  }

  trait NotLoggedIn {
    self: Setup =>

    lazy val request = FakeRequest()
      .withSession(sessionParams: _*)

    ThirdPartyDeveloperConnectorMock.FetchSession.fails()
  }

  trait IsPartLoggedInEnablingMFA {
    self: Setup =>

    lazy val request = FakeRequest()
      .withUser(underTest)(sessionId)
      .withSession(sessionParams: _*)

    ThirdPartyDeveloperConnectorMock.FetchSession.succeedsPartLoggedInEnablingMfa()
  }

  "FeedbackController" when {
    "invoke page" should {
      "succeed when session exists" in new Setup with IsLoggedIn {
        val requestWithSupportCookie = request.withSupportSession(underTest)(supportSessionId)
        SupportServiceMock.GetSupportFlow.succeeds()

        val result = addToken(underTest.page())(requestWithSupportCookie)

        status(result) shouldBe OK
        contentAsString(result) should include("Give feedback")
      }

      "succeed when session does not exist" in new Setup {
        val request = FakeRequest()

        val result = addToken(underTest.page())(request)

        status(result) shouldBe OK
        contentAsString(result) should include("Give feedback")
      }
    }

    "invoke action" should {
      "submit new request with comments" in new Setup with IsLoggedIn {
        val newRequest = request
          .withFormUrlEncodedBody(
            "whatWereYouDoing" -> whatWereYouDoing,
            "feedback"         -> feedback
          )
        SupportServiceMock.GiveFeedback.succeeds()

        val result = addToken(underTest.action())(newRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/devhub-support/feedback-confirm")

        SupportServiceMock.GiveFeedback.verifyCalledWith(
          fullName,
          emailAddress,
          whatWereYouDoing,
          feedback,
          None,
          Some(sessionId.toString())
        )
      }

      "submit new request with no details" in new Setup with IsLoggedIn {
        val newRequest = request
          .withFormUrlEncodedBody(
            "whatWereYouDoing" -> "",
            "feedback"         -> ""
          )
        SupportServiceMock.GiveFeedback.succeeds()

        val result = addToken(underTest.action())(newRequest)

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("Enter details of what you were doing")
        contentAsString(result) should include("Enter details of your experience")
      }

      "submit new request when honeypot field filled in but logged in" in new Setup with IsLoggedIn {
        val newRequest = request
          .withFormUrlEncodedBody(
            "whatWereYouDoing" -> whatWereYouDoing,
            "feedback"         -> feedback,
            "url"              -> honeypotUrl
          )
        SupportServiceMock.GiveFeedback.succeeds()

        val result = addToken(underTest.action())(newRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/devhub-support/feedback-confirm")

        SupportServiceMock.GiveFeedback.verifyCalledWith(
          fullName,
          emailAddress,
          whatWereYouDoing,
          feedback,
          None,
          Some(sessionId.toString())
        )
      }

      "show dummy confirmation page when honeypot field filled in but not logged in" in new Setup with NotLoggedIn {
        val newRequest = request
          .withFormUrlEncodedBody(
            "whatWereYouDoing" -> whatWereYouDoing,
            "feedback"         -> feedback,
            "url"              -> honeypotUrl
          )

        val result = addToken(underTest.action())(newRequest)

        status(result) shouldBe OK
        contentAsString(result) should include("Thank you for your feedback")
        contentAsString(result) should include("We will use your feedback to make our services better.")

        verifyZeroInteractions(SupportServiceMock.aMock)
      }
    }

    "invoke confirmation page" should {
      "succeed when session exists" in new Setup with IsLoggedIn {
        val requestWithSupportCookie = request.withSupportSession(underTest)(supportSessionId)
        SupportServiceMock.GetSupportFlow.succeeds()

        val result = addToken(underTest.confirmationPage())(requestWithSupportCookie)

        status(result) shouldBe OK
        contentAsString(result) should include("Thank you for your feedback")
        contentAsString(result) should include("We will use your feedback to make our services better.")
      }
    }
  }
}
