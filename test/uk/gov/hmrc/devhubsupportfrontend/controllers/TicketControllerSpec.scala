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
import uk.gov.hmrc.devhubsupportfrontend.domain.models.{DeskproMessage, DeskproTicket, SupportSessionId}
import uk.gov.hmrc.devhubsupportfrontend.mocks.connectors.ThirdPartyDeveloperConnectorMockModule
import uk.gov.hmrc.devhubsupportfrontend.mocks.services.TicketServiceMockModule
import uk.gov.hmrc.devhubsupportfrontend.utils.WithCSRFAddToken
import uk.gov.hmrc.devhubsupportfrontend.utils.WithLoggedInSession._
import uk.gov.hmrc.devhubsupportfrontend.views.html.{TicketListView, TicketView}

class TicketControllerSpec extends BaseControllerSpec with WithCSRFAddToken {

  trait Setup extends TicketServiceMockModule with ThirdPartyDeveloperConnectorMockModule with UserBuilder with LocalUserIdTracker {
    val ticketListView = app.injector.instanceOf[TicketListView]
    val ticketView     = app.injector.instanceOf[TicketView]

    val underTest = new TicketController(
      mcc,
      cookieSigner,
      mock[ErrorHandler],
      ThirdPartyDeveloperConnectorMock.aMock,
      TicketServiceMock.aMock,
      ticketListView,
      ticketView
    )

    val ticketId: Int = 4232

    val message = DeskproMessage(
      3467,
      ticketId,
      33,
      instant,
      false,
      "Test message contents"
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

        val result = addToken(underTest.ticketListPage())(request)

        status(result) shouldBe OK
        contentAsString(result) should include("SDST-2025XON927")
        contentAsString(result) should include("HMRC Developer Hub: Support Enquiry")
      }

      "redirect to logon page if not logged in" in new Setup with NotLoggedIn {
        val result = addToken(underTest.ticketListPage())(request)

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
        contentAsString(result) should include("Close this support request")
      }

      "hide the close button when ticket is resolved" in new Setup with IsLoggedIn {
        TicketServiceMock.FetchTicket.succeeds(Some(ticket.copy(status = "resolved")))

        val result = addToken(underTest.ticketPage(ticketId))(request)

        status(result) shouldBe OK
        contentAsString(result) should include("SDST-2025XON927")
        contentAsString(result) should not include ("Close this support request")
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

  "Close a ticket" when {
    "invoke closeTicket" should {
      "return to the tickets list when ticket closed successfully" in new Setup with IsLoggedIn {
        TicketServiceMock.CloseTicket.succeeds()

        val result = addToken(underTest.closeTicket(ticketId))(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe "/devhub-support/tickets"

      }

      "return 500 if ticket not found" in new Setup with IsLoggedIn {
        TicketServiceMock.CloseTicket.notFound()

        val result = addToken(underTest.closeTicket(ticketId))(request)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "return 500 if ticket close failed" in new Setup with IsLoggedIn {
        TicketServiceMock.CloseTicket.fails()

        val result = addToken(underTest.closeTicket(ticketId))(request)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "redirect to logon page if not logged in" in new Setup with NotLoggedIn {
        val result = addToken(underTest.closeTicket(ticketId))(request)

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
            "response" -> "Test response"
          )

        TicketServiceMock.FetchTicket.succeeds(Some(ticket))
        TicketServiceMock.CreateResponse.succeeds()

        val result = addToken(underTest.submitTicketResponse(ticketId))(ticketResponseRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe "/devhub-support/tickets"
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
            "response" -> "Test response"
          )
        TicketServiceMock.CreateResponse.notFound()

        val result = addToken(underTest.submitTicketResponse(ticketId))(ticketResponseRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "return 500 if submit response failed" in new Setup with IsLoggedIn {
        val ticketResponseRequest = request
          .withFormUrlEncodedBody(
            "response" -> "Test response"
          )
        TicketServiceMock.CreateResponse.fails()

        val result = addToken(underTest.submitTicketResponse(ticketId))(ticketResponseRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "redirect to logon page if not logged in" in new Setup with NotLoggedIn {
        val result = addToken(underTest.submitTicketResponse(ticketId))(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/developer/login")
      }
    }
  }
}
