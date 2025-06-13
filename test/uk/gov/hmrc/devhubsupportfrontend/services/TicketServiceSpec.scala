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

package uk.gov.hmrc.devhubsupportfrontend.services

import scala.concurrent.ExecutionContext.Implicits.global

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.devhubsupportfrontend.connectors.ApiPlatformDeskproConnector.{DeskproTicketCloseFailure, DeskproTicketCloseNotFound, DeskproTicketCloseSuccess}
import uk.gov.hmrc.devhubsupportfrontend.domain.models.DeskproTicket
import uk.gov.hmrc.devhubsupportfrontend.mocks.connectors.ApiPlatformDeskproConnectorMockModule
import uk.gov.hmrc.devhubsupportfrontend.utils.AsyncHmrcSpec

class TicketServiceSpec extends AsyncHmrcSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup extends ApiPlatformDeskproConnectorMockModule with FixedClock {

    val ticketId: Int = 4232
    val userEmail     = LaxEmailAddress("bob@example.com")

    val ticket    = DeskproTicket(
      ticketId,
      "SDST-2025XON927",
      61,
      LaxEmailAddress("bob@example.com"),
      "awaiting_user",
      instant,
      Some(instant),
      "HMRC Developer Hub: Support Enquiry",
      List.empty
    )
    val underTest = new TicketService(ApiPlatformDeskproConnectorMock.aMock)
  }

  "fetchTicket" should {
    "fetch a ticket from Deskpro" in new Setup {
      ApiPlatformDeskproConnectorMock.FetchTicket.succeeds(Some(ticket))

      val result = await(underTest.fetchTicket(ticketId))

      result shouldBe Some(ticket)
    }
  }

  "closeTicket" should {
    "close a Deskpro ticket" in new Setup {
      ApiPlatformDeskproConnectorMock.CloseTicket.succeeds()

      val result = await(underTest.closeTicket(ticketId))

      result shouldBe DeskproTicketCloseSuccess
    }

    "return DeskproTicketCloseNotFound if ticket not found" in new Setup {
      ApiPlatformDeskproConnectorMock.CloseTicket.notFound()

      val result = await(underTest.closeTicket(ticketId))

      result shouldBe DeskproTicketCloseNotFound
    }

    "return DeskproTicketCloseFailure if ticket close failed" in new Setup {
      ApiPlatformDeskproConnectorMock.CloseTicket.fails()

      val result = await(underTest.closeTicket(ticketId))

      result shouldBe DeskproTicketCloseFailure
    }
  }

  "getTicketsForUser" should {
    "get a list of open tickets from Deskpro" in new Setup {
      ApiPlatformDeskproConnectorMock.GetTicketsForUser.succeeds(List(ticket))

      val result = await(underTest.getTicketsForUser(userEmail, false))

      result shouldBe List(ticket)
      ApiPlatformDeskproConnectorMock.GetTicketsForUser.verifyCalledWith(userEmail, None)
    }

    "get a list of resolved tickets from Deskpro" in new Setup {
      ApiPlatformDeskproConnectorMock.GetTicketsForUser.succeeds(List(ticket))

      val result = await(underTest.getTicketsForUser(userEmail, true))

      result shouldBe List(ticket)
      ApiPlatformDeskproConnectorMock.GetTicketsForUser.verifyCalledWith(userEmail, Some("resolved"))
    }
  }
}
