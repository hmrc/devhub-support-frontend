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
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.tpd.test.builders.UserBuilder
import uk.gov.hmrc.apiplatform.modules.tpd.test.utils.LocalUserIdTracker

import uk.gov.hmrc.devhubsupportfrontend.config.ErrorHandler
import uk.gov.hmrc.devhubsupportfrontend.domain.models.{DeskproAttachment, DeskproMessage, DeskproTicket, SupportSessionId}
import uk.gov.hmrc.devhubsupportfrontend.mocks.connectors.{ThirdPartyDeveloperConnectorMockModule, UpscanInitiateConnectorMockModule}
import uk.gov.hmrc.devhubsupportfrontend.mocks.services.TicketServiceMockModule
import uk.gov.hmrc.devhubsupportfrontend.utils.WithCSRFAddToken
import uk.gov.hmrc.devhubsupportfrontend.utils.WithLoggedInSession._
import uk.gov.hmrc.devhubsupportfrontend.views.html._

class TicketControllerSpec extends BaseControllerSpec with WithCSRFAddToken {

  trait Setup extends TicketServiceMockModule with ThirdPartyDeveloperConnectorMockModule with UpscanInitiateConnectorMockModule with UserBuilder with LocalUserIdTracker {
    val ticketListView            = app.injector.instanceOf[TicketListView]
    val ticketView                = app.injector.instanceOf[TicketView]
    val ticketViewWithAttachments = app.injector.instanceOf[TicketViewWithAttachments]

    val underTest = new TicketController(
      mcc,
      cookieSigner,
      mock[ErrorHandler],
      ThirdPartyDeveloperConnectorMock.aMock,
      UpscanInitiateConnectorMock.aMock,
      TicketServiceMock.aMock,
      ticketListView,
      ticketView,
      ticketViewWithAttachments
    )

    val ticketId: Int = 4232

    val key = "key1"

    val message = DeskproMessage(
      3467,
      ticketId,
      33,
      instant,
      false,
      "Test message contents",
      List(DeskproAttachment("file.name", "https://example.com"))
    )

    val ticket = DeskproTicket(
      ticketId,
      "SDST-2025XON927",
      61,
      LaxEmailAddress("something@example.com"),
      "awaiting_user",
      instant,
      instant,
      Some(instant),
      "HMRC Developer Hub: Support Enquiry",
      List(message)
    )

    val sessionParams: Seq[(String, String)] = Seq("csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken)
    val supportSessionId                     = SupportSessionId.random

    val statusOpen          = "open"
    val statusAwaitingAgent = "awaiting_agent"
    val actionSend          = "send"
    val response            = "Test response"
    val fileReference       = "abc-123"
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

  "Show list of tickets for the user" when {
    "invoke ticketListPage" should {
      "render the ticket list page" in new Setup with IsLoggedIn {
        TicketServiceMock.GetTicketsForUser.succeeds(List(ticket))

        val result = addToken(underTest.ticketListPage(false))(request)

        status(result) shouldBe OK
        contentAsString(result) should include("SDST-2025XON927")
        contentAsString(result) should include("HMRC Developer Hub: Support Enquiry")
      }

      "show message when unresolved tab selected and not tickets" in new Setup with IsLoggedIn {
        TicketServiceMock.GetTicketsForUser.succeeds(List())

        val result = addToken(underTest.ticketListPage(false))(request)

        status(result) shouldBe OK
        contentAsString(result) should include("You do not have any unresolved requests at the moment.")
      }

      "show message when resolved tab selected and no tickets" in new Setup with IsLoggedIn {
        TicketServiceMock.GetTicketsForUser.succeeds(List())

        val result = addToken(underTest.ticketListPage(true))(request)

        status(result) shouldBe OK
        contentAsString(result) should include("You do not have any resolved requests at the moment.")
      }

      "redirect to logon page if not logged in" in new Setup with NotLoggedIn {
        val result = addToken(underTest.ticketListPage(false))(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/developer/login")
      }
    }
  }

  "Show a ticket" when {
    "invoke ticketPage" should {
      "render the ticket page" in new Setup with IsLoggedIn {
        TicketServiceMock.FetchTicket.succeeds(Some(ticket))

        val result = addToken(underTest.ticketPage(ticketId))(request)

        status(result) shouldBe OK
        contentAsString(result) should include("SDST-2025XON927")
        contentAsString(result) should include("HMRC Developer Hub: Support Enquiry")
        contentAsString(result) should include("We need your reply")
        contentAsString(result) should include("Test message contents")
        contentAsString(result) should include("Mark as resolved")
        contentAsString(result) should not include ("file.name")
      }

      "hide the close button when ticket is resolved" in new Setup with IsLoggedIn {
        TicketServiceMock.FetchTicket.succeeds(Some(ticket.copy(status = "resolved")))

        val result = addToken(underTest.ticketPage(ticketId))(request)

        status(result) shouldBe OK
        contentAsString(result) should include("SDST-2025XON927")
        contentAsString(result) should not include ("Mark as resolved")
      }

      "return 404 if ticket not found" in new Setup with IsLoggedIn {
        TicketServiceMock.FetchTicket.succeeds(None)

        val result = addToken(underTest.ticketPage(ticketId))(request)

        status(result) shouldBe NOT_FOUND
      }

      "return 404 if user email is different from person email in ticket" in new Setup with IsLoggedIn {
        val ticketDiffPersonEmail = ticket.copy(personEmail = LaxEmailAddress("bob@example.com"))
        TicketServiceMock.FetchTicket.succeeds(Some(ticketDiffPersonEmail))

        val result = addToken(underTest.ticketPage(ticketId))(request)

        status(result) shouldBe NOT_FOUND
      }

      "redirect to logon page if not logged in" in new Setup with NotLoggedIn {
        val result = addToken(underTest.ticketPage(ticketId))(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/developer/login")
      }
    }
  }

  "Show a ticket with attachments" when {
    "invoke ticketPageWithAttachments" should {
      "render the ticket page showing attachments" in new Setup with IsLoggedIn {
        TicketServiceMock.FetchTicket.succeeds(Some(ticket))
        UpscanInitiateConnectorMock.Initiate.succeeds()

        val result = addToken(underTest.ticketPageWithAttachments(ticketId))(request)

        status(result) shouldBe OK
        contentAsString(result) should include("SDST-2025XON927")
        contentAsString(result) should include("HMRC Developer Hub: Support Enquiry")
        contentAsString(result) should include("We need your reply")
        contentAsString(result) should include("Test message contents")
        contentAsString(result) should include("Mark as resolved")
        contentAsString(result) should include("file.name")
      }

      "hide the close button when ticket is resolved" in new Setup with IsLoggedIn {
        TicketServiceMock.FetchTicket.succeeds(Some(ticket.copy(status = "resolved")))
        UpscanInitiateConnectorMock.Initiate.succeeds()

        val result = addToken(underTest.ticketPageWithAttachments(ticketId))(request)

        status(result) shouldBe OK
        contentAsString(result) should include("SDST-2025XON927")
        contentAsString(result) should not include ("Mark as resolved")
      }

      "populate the hidden fileReferences with the upscan key set in the request param" in new Setup with IsLoggedIn {
        TicketServiceMock.FetchTicket.succeeds(Some(ticket))
        UpscanInitiateConnectorMock.Initiate.succeeds()

        val upscanKey = "abc-123"
        val result    = addToken(underTest.ticketPageWithAttachments(ticketId, Some(upscanKey)))(request)

        status(result) shouldBe OK
        contentAsString(result) should include("name=\"fileReferences\" value=\"abc-123\"")
      }

      "return 404 if ticket not found" in new Setup with IsLoggedIn {
        TicketServiceMock.FetchTicket.succeeds(None)
        UpscanInitiateConnectorMock.Initiate.succeeds()

        val result = addToken(underTest.ticketPageWithAttachments(ticketId))(request)

        status(result) shouldBe NOT_FOUND
      }

      "return 404 if user email is different from person email in ticket" in new Setup with IsLoggedIn {
        val ticketDiffPersonEmail = ticket.copy(personEmail = LaxEmailAddress("bob@example.com"))
        TicketServiceMock.FetchTicket.succeeds(Some(ticketDiffPersonEmail))
        UpscanInitiateConnectorMock.Initiate.succeeds()

        val result = addToken(underTest.ticketPageWithAttachments(ticketId))(request)

        status(result) shouldBe NOT_FOUND
      }

      "redirect to logon page if not logged in" in new Setup with NotLoggedIn {
        val result = addToken(underTest.ticketPageWithAttachments(ticketId))(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/developer/login")
      }
    }
  }

  "Submit a response" when {
    "invoke submitTicketResponse" should {
      "return to the tickets list when response submitted successfully" in new Setup with IsLoggedIn {
        val ticketResponseRequest = request
          .withFormUrlEncodedBody(
            "status"   -> statusOpen,
            "action"   -> actionSend,
            "response" -> response
          )

        TicketServiceMock.FetchTicket.succeeds(Some(ticket))
        TicketServiceMock.CreateResponse.succeeds()

        val result = addToken(underTest.submitTicketResponse(ticketId))(ticketResponseRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe "/devhub-support/tickets"

        TicketServiceMock.CreateResponse.verifyCalledWith(
          ticketId = ticketId,
          message = response,
          status = statusOpen,
          newStatus = statusAwaitingAgent,
          fileReferences = Nil
        )
      }

      "return to the same page with validation error when response not populated" in new Setup with IsLoggedIn {
        TicketServiceMock.FetchTicket.succeeds(Some(ticket))
        TicketServiceMock.CreateResponse.succeeds()

        val result = addToken(underTest.submitTicketResponse(ticketId))(request)

        status(result) shouldBe OK
        contentAsString(result) should include("Enter a response")
      }

      "return 500 if ticket not found" in new Setup with IsLoggedIn {
        val ticketResponseRequest = request
          .withFormUrlEncodedBody(
            "status"   -> statusOpen,
            "action"   -> actionSend,
            "response" -> response
          )
        TicketServiceMock.CreateResponse.notFound()

        val result = addToken(underTest.submitTicketResponse(ticketId))(ticketResponseRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR

        TicketServiceMock.CreateResponse.verifyCalledWith(
          ticketId = ticketId,
          message = response,
          status = statusOpen,
          newStatus = statusAwaitingAgent,
          fileReferences = Nil
        )
      }

      "return 500 if submit response failed" in new Setup with IsLoggedIn {
        val ticketResponseRequest = request
          .withFormUrlEncodedBody(
            "status"   -> statusOpen,
            "action"   -> actionSend,
            "response" -> response
          )
        TicketServiceMock.CreateResponse.fails()

        val result = addToken(underTest.submitTicketResponse(ticketId))(ticketResponseRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR

        TicketServiceMock.CreateResponse.verifyCalledWith(
          ticketId = ticketId,
          message = response,
          status = statusOpen,
          newStatus = statusAwaitingAgent,
          fileReferences = Nil
        )
      }

      "redirect to logon page if not logged in" in new Setup with NotLoggedIn {
        val result = addToken(underTest.submitTicketResponse(ticketId))(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/developer/login")
      }
    }
  }

  "Submit a response with attachments" when {
    "invoke submitTicketResponseWithAttachments" should {
      "pass the fileReferences to the ticket service" in new Setup with IsLoggedIn {
        val ticketResponseRequest = request
          .withFormUrlEncodedBody(
            "status"         -> statusOpen,
            "action"         -> actionSend,
            "response"       -> response,
            "fileReferences" -> fileReference
          )

        TicketServiceMock.CreateResponse.succeeds()

        val result = addToken(underTest.submitTicketResponseWithAttachments(ticketId))(ticketResponseRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe "/devhub-support/tickets"

        TicketServiceMock.CreateResponse.verifyCalledWith(
          ticketId = ticketId,
          message = response,
          status = statusOpen,
          newStatus = statusAwaitingAgent,
          fileReferences = List(fileReference)
        )
      }
    }
  }
}
