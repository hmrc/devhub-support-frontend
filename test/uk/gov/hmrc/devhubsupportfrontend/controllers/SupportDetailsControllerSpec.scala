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
import uk.gov.hmrc.devhubsupportfrontend.controllers.BaseControllerSpec
import uk.gov.hmrc.devhubsupportfrontend.domain.models.SupportSessionId
import uk.gov.hmrc.devhubsupportfrontend.mocks.connectors.ThirdPartyDeveloperConnectorMockModule
import uk.gov.hmrc.devhubsupportfrontend.mocks.services.SupportServiceMockModule
import uk.gov.hmrc.devhubsupportfrontend.utils.WithCSRFAddToken
import uk.gov.hmrc.devhubsupportfrontend.utils.WithLoggedInSession._
import uk.gov.hmrc.devhubsupportfrontend.utils.WithSupportSession._
import uk.gov.hmrc.devhubsupportfrontend.views.html.support.{SupportPageConfirmationView, SupportPageDetailView}

class SupportDetailsControllerSpec extends BaseControllerSpec with WithCSRFAddToken {

  trait Setup extends SupportServiceMockModule with ThirdPartyDeveloperConnectorMockModule with UserBuilder with LocalUserIdTracker {
    val supportPageDetailView       = app.injector.instanceOf[SupportPageDetailView]
    val supportPageConfirmationView = app.injector.instanceOf[SupportPageConfirmationView]

    val underTest = new SupportDetailsController(
      mcc,
      cookieSigner,
      mock[ErrorHandler],
      ThirdPartyDeveloperConnectorMock.aMock,
      SupportServiceMock.aMock,
      supportPageDetailView,
      supportPageConfirmationView
    )

    val sessionParams: Seq[(String, String)] = Seq("csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken)
    val supportSessionId                     = SupportSessionId.random
  }

  trait IsLoggedIn {
    self: Setup =>

    lazy val request = FakeRequest()
      .withUser(underTest, cookieSigner)(sessionId)
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
      .withUser(underTest, cookieSigner)(sessionId)
      .withSession(sessionParams: _*)

    ThirdPartyDeveloperConnectorMock.FetchSession.succeedsPartLoggedInEnablingMfa()
  }

  "SupportDetailsController" when {
    "invoke supportDetailsPage" should {
      "render the new support details page" in new Setup with IsLoggedIn {
        SupportServiceMock.GetSupportFlow.succeeds()

        val result = addToken(underTest.supportDetailsPage())(request)

        status(result) shouldBe OK
      }
    }

    "invoke submitSupportDetails" should {
      "submit new request with name, email & comments from form" in new Setup {
        val request = FakeRequest()
          .withSession(sessionParams: _*)
          .withFormUrlEncodedBody(
            "fullName"     -> "Peter Smith",
            "emailAddress" -> "peter@example.com",
            "details"      -> "A+++, good seller, would buy again, this is a long comment"
          )
        SupportServiceMock.GetSupportFlow.succeeds()
        SupportServiceMock.SubmitTicket.succeeds()

        val result = addToken(underTest.submitSupportDetails())(request)

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/devhub-support/new-support/confirmation")
      }

      "submit request with name, email and invalid details returns BAD_REQUEST" in new Setup {
        val request = FakeRequest()
          .withSession(sessionParams: _*)
          .withFormUrlEncodedBody(
            "fullName"     -> "Peter Smith",
            "emailAddress" -> "peter@example.com",
            "details"      -> "A+++, good como  puedo iniciar, would buy again"
          )
        SupportServiceMock.GetSupportFlow.succeeds()
        SupportServiceMock.SubmitTicket.succeeds()

        val result = addToken(underTest.submitSupportDetails())(request)

        status(result) shouldBe 400
      }

      "submit request with name, email, details and invalid team member email returns BAD_REQUEST" in new Setup {
        val request = FakeRequest()
          .withSession(sessionParams: _*)
          .withFormUrlEncodedBody(
            "fullName"               -> "Peter Smith",
            "emailAddress"           -> "peter@example.com",
            "details"                -> "Blah blah blah",
            "teamMemberEmailAddress" -> "abc"
          )
        SupportServiceMock.GetSupportFlow.succeeds()
        SupportServiceMock.SubmitTicket.succeeds()

        val result = addToken(underTest.submitSupportDetails())(request)

        status(result) shouldBe 400
      }
    }

    "invoke supportConfirmationPage" should {
      "succeed when session exists" in new Setup with IsLoggedIn {
        val requestWithSupportCookie = request.withSupportSession(underTest, cookieSigner)(supportSessionId)
        SupportServiceMock.GetSupportFlow.succeeds()

        val result = addToken(underTest.supportConfirmationPage())(requestWithSupportCookie)

        status(result) shouldBe OK
      }

      "succeed when session does not exist" in new Setup {
        val request = FakeRequest()

        val result = addToken(underTest.supportConfirmationPage())(request)

        status(result) shouldBe SEE_OTHER
      }

    }
  }
}
