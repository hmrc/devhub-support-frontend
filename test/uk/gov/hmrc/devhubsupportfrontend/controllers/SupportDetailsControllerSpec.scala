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
import uk.gov.hmrc.devhubsupportfrontend.connectors.ApiPlatformDeskproConnector.Attachment
import uk.gov.hmrc.devhubsupportfrontend.domain.models.{SupportFlow, SupportSessionId}
import uk.gov.hmrc.devhubsupportfrontend.mocks.connectors.{ThirdPartyDeveloperConnectorMockModule, UpscanInitiateConnectorMockModule}
import uk.gov.hmrc.devhubsupportfrontend.mocks.services.SupportServiceMockModule
import uk.gov.hmrc.devhubsupportfrontend.utils.WithCSRFAddToken
import uk.gov.hmrc.devhubsupportfrontend.utils.WithLoggedInSession._
import uk.gov.hmrc.devhubsupportfrontend.utils.WithSupportSession._
import uk.gov.hmrc.devhubsupportfrontend.views.html.{SupportPageConfirmationForHoneyPotFieldView, SupportPageConfirmationView, SupportPageDetailView}

class SupportDetailsControllerSpec extends BaseControllerSpec with WithCSRFAddToken {

  trait Setup extends SupportServiceMockModule with ThirdPartyDeveloperConnectorMockModule with UpscanInitiateConnectorMockModule with UserBuilder with LocalUserIdTracker {
    val supportPageDetailView                       = app.injector.instanceOf[SupportPageDetailView]
    val supportPageConfirmationView                 = app.injector.instanceOf[SupportPageConfirmationView]
    val supportPageConfirmationForHoneyPotFieldView = app.injector.instanceOf[SupportPageConfirmationForHoneyPotFieldView]

    val underTest = new SupportDetailsController(
      mcc,
      cookieSigner,
      mock[ErrorHandler],
      ThirdPartyDeveloperConnectorMock.aMock,
      UpscanInitiateConnectorMock.aMock,
      SupportServiceMock.aMock,
      supportPageDetailView,
      supportPageConfirmationView,
      supportPageConfirmationForHoneyPotFieldView
    )

    val sessionParams: Seq[(String, String)] = Seq("csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken)
    val supportSessionId                     = SupportSessionId.random

    val fullName               = "Peter Smith"
    val emailAddress           = "peter@example.com"
    val details                = "A+++, good seller, would buy again, this is a long comment"
    val invalidDetails         = "A+++, good como  puedo iniciar, would buy again"
    val validDetails           = "Blah blah blah"
    val honeypotUrl            = "It's a trap"
    val fileReference          = "fileRef1"
    val fileName               = "test.pdf"
    val invalidTeamMemberEmail = "abc"
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

  "SupportDetailsController" when {
    "invoke supportConfirmationPage" should {
      "succeed when session exists" in new Setup with IsLoggedIn {
        val requestWithSupportCookie = request.withSupportSession(underTest)(supportSessionId)
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

    "invoke supportDetailsPageWithAttachments" should {
      "render the support details page with attachment support" in new Setup with IsLoggedIn {
        SupportServiceMock.GetSupportFlow.succeeds()
        UpscanInitiateConnectorMock.Initiate.succeeds()

        val result = addToken(underTest.supportDetailsPage())(request)

        status(result) shouldBe OK
        contentAsString(result) should include("Add files to your support request")
      }

      "populate the post target and the hidden upscan fields in the upscan attachment upload form" in new Setup with IsLoggedIn {
        val upscanPostTarget      = "https://upscan.example.com/upload"
        val upscanKey             = "new-upscan-upload-key"
        val upscanSuccessRedirect = "http://localhost:9685/devhub-support/upscan/support-success"
        val upscanErrorRedirect   = "http://localhost:9685/devhub-support/upscan/support-success"

        SupportServiceMock.GetSupportFlow.succeeds()
        UpscanInitiateConnectorMock.Initiate.succeedsWith(
          upscanPostTarget,
          Map(
            "key"                     -> upscanKey,
            "success_action_redirect" -> upscanSuccessRedirect,
            "error_action_redirect"   -> upscanErrorRedirect
          )
        )

        val result = addToken(underTest.supportDetailsPage())(request)

        status(result) shouldBe OK
        contentAsString(result) should include(s"form action=\"$upscanPostTarget\"")
        contentAsString(result) should include(s"name=\"key\" value=\"$upscanKey\"")
        contentAsString(result) should include(s"name=\"success_action_redirect\" value=\"$upscanSuccessRedirect\"")
        contentAsString(result) should include(s"name=\"error_action_redirect\" value=\"$upscanErrorRedirect\"")
      }

      "not include upload attachment section when not logged in" in new Setup with NotLoggedIn {
        val upscanPostTarget      = "https://upscan.example.com/upload"
        val upscanKey             = "new-upscan-upload-key"
        val upscanSuccessRedirect = "http://localhost:9685/devhub-support/upscan/support-success"
        val upscanErrorRedirect   = "http://localhost:9685/devhub-support/upscan/support-success"

        SupportServiceMock.GetSupportFlow.succeeds()
        UpscanInitiateConnectorMock.Initiate.succeedsWith(
          upscanPostTarget,
          Map(
            "key"                     -> upscanKey,
            "success_action_redirect" -> upscanSuccessRedirect,
            "error_action_redirect"   -> upscanErrorRedirect
          )
        )

        val result = addToken(underTest.supportDetailsPage())(request)

        status(result) shouldBe OK
        contentAsString(result) should not include (s"class=\"upload-section\"")
      }

    }

    "invoke submitSupportDetails" should {
      "redirect to login when not logged in" in new Setup with NotLoggedIn {
        val supportFlow = SupportFlow(SupportSessionId.random, SupportData.UsingAnApi.id)
        val newRequest  = request
          .withFormUrlEncodedBody(
            "fullName"                         -> fullName,
            "emailAddress"                     -> emailAddress,
            "details"                          -> details,
            "fileAttachments[0].fileReference" -> fileReference,
            "fileAttachments[0].fileName"      -> fileName
          )
        SupportServiceMock.GetSupportFlow.succeeds(supportFlow)

        val result = addToken(underTest.submitSupportDetails())(newRequest)

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/developer/login")

      }

      "submit new request with name, email, comments and attachments from form when logged in" in new Setup with IsLoggedIn {
        val supportFlow = SupportFlow(SupportSessionId.random, SupportData.UsingAnApi.id)
        val newRequest  = request
          .withFormUrlEncodedBody(
            "fullName"                         -> fullName,
            "emailAddress"                     -> emailAddress,
            "details"                          -> details,
            "fileAttachments[0].fileReference" -> fileReference,
            "fileAttachments[0].fileName"      -> fileName
          )
        SupportServiceMock.GetSupportFlow.succeeds(supportFlow)
        SupportServiceMock.SubmitTicket.succeeds()

        val result = addToken(underTest.submitSupportDetails())(newRequest)

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/devhub-support/confirmation")

        SupportServiceMock.SubmitTicket.verifyCalledWith(
          supportFlow,
          SupportDetailsForm(
            details,
            fullName,
            emailAddress,
            None,
            None,
            None,
            List(Attachment(fileReference, fileName))
          )
        )
      }

      "create support ticket when request with attachments and url (honeypot field) on form and user is logged in" in new Setup with IsLoggedIn {
        val newRequest = request
          .withFormUrlEncodedBody(
            "fullName"                         -> fullName,
            "emailAddress"                     -> emailAddress,
            "details"                          -> details,
            "url"                              -> honeypotUrl,
            "fileAttachments[0].fileReference" -> fileReference,
            "fileAttachments[0].fileName"      -> fileName
          )
        SupportServiceMock.GetSupportFlow.succeeds()
        SupportServiceMock.SubmitTicket.succeeds()

        val result = addToken(underTest.submitSupportDetails())(newRequest)

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/devhub-support/confirmation")
      }

      "not create support ticket but show dummy confirmation page when url (honeypot field) on form and user is not logged in" in new Setup with NotLoggedIn {
        val newRequest = request
          .withFormUrlEncodedBody(
            "fullName"     -> fullName,
            "emailAddress" -> emailAddress,
            "details"      -> details,
            "url"          -> honeypotUrl
          )
        SupportServiceMock.GetSupportFlow.succeeds()

        val result = addToken(underTest.submitSupportDetails())(newRequest)

        status(result) shouldBe OK
        contentAsString(result) should include("We've sent you a confirmation email")
        contentAsString(result) should include("Your request will be sent to the right team as quickly as possible")
        contentAsString(result) should include("We'll send an email to your email address whenever there's an update")
      }

      "submit request with attachments and 3000 characters including \r\n details returns success" in new Setup with IsLoggedIn {
        val newRequest = request
          .withFormUrlEncodedBody(
            "fullName"                         -> fullName,
            "emailAddress"                     -> emailAddress,
            "details"                          -> "A\r\n" * 1500,
            "fileAttachments[0].fileReference" -> fileReference,
            "fileAttachments[0].fileName"      -> fileName
          )
        SupportServiceMock.GetSupportFlow.succeeds()
        SupportServiceMock.SubmitTicket.succeeds()

        val result = addToken(underTest.submitSupportDetails())(newRequest)

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/devhub-support/confirmation")
      }

      "submit request with attachments and invalid details returns BAD_REQUEST" in new Setup with IsPartLoggedInEnablingMFA {
        request
          .withFormUrlEncodedBody(
            "fullName"                         -> fullName,
            "emailAddress"                     -> emailAddress,
            "details"                          -> "A+++, good como  puedo iniciar, would buy again",
            "fileAttachments[0].fileReference" -> fileReference,
            "fileAttachments[0].fileName"      -> fileName
          )
        SupportServiceMock.GetSupportFlow.succeeds()
        UpscanInitiateConnectorMock.Initiate.succeeds()

        val result = addToken(underTest.submitSupportDetails())(request)

        status(result) shouldBe 400
      }

      "submit request with attachments, details and invalid team member email returns BAD_REQUEST" in new Setup with IsPartLoggedInEnablingMFA {
        request
          .withFormUrlEncodedBody(
            "fullName"                         -> fullName,
            "emailAddress"                     -> emailAddress,
            "details"                          -> validDetails,
            "teamMemberEmailAddress"           -> "abc",
            "fileAttachments[0].fileReference" -> fileReference,
            "fileAttachments[0].fileName"      -> fileName
          )
        SupportServiceMock.GetSupportFlow.succeeds()
        UpscanInitiateConnectorMock.Initiate.succeeds()

        val result = addToken(underTest.submitSupportDetails())(request)

        status(result) shouldBe 400
      }
    }
  }
}
