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

package uk.gov.hmrc.devhubsupportfrontend.mocks.connectors

import scala.concurrent.Future

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

import uk.gov.hmrc.devhubsupportfrontend.connectors.ApiPlatformDeskproConnector
import uk.gov.hmrc.devhubsupportfrontend.connectors.ApiPlatformDeskproConnector._
import uk.gov.hmrc.devhubsupportfrontend.domain.models.DeskproTicket

trait ApiPlatformDeskproConnectorMockModule extends MockitoSugar with ArgumentMatchersSugar {

  object ApiPlatformDeskproConnectorMock {
    val aMock = mock[ApiPlatformDeskproConnector]

    object CreateTicket {

      def succeeds() = {
        when(aMock.createTicket(*[ApiPlatformDeskproConnector.CreateTicketRequest], *)).thenReturn(Future.successful(Some("test")))
      }

      def verifyCalledWith(request: CreateTicketRequest) = {
        verify(aMock).createTicket(eqTo(request), *)
      }
    }

    object FetchTicket {

      def succeeds(ticket: Option[DeskproTicket]) = {
        when(aMock.fetchTicket(*, *)).thenReturn(Future.successful(ticket))
      }
    }

    object CloseTicket {

      def succeeds() = {
        when(aMock.closeTicket(*, *)).thenReturn(Future.successful(DeskproTicketCloseSuccess))
      }

      def notFound() = {
        when(aMock.closeTicket(*, *)).thenReturn(Future.successful(DeskproTicketCloseNotFound))
      }

      def fails() = {
        when(aMock.closeTicket(*, *)).thenReturn(Future.successful(DeskproTicketCloseFailure))
      }
    }

    object CreateResponse {

      def succeeds() = {
        when(aMock.createResponse(*, *[LaxEmailAddress], *, *, *, *)).thenReturn(Future.successful(DeskproTicketResponseSuccess))
      }

      def notFound() = {
        when(aMock.createResponse(*, *[LaxEmailAddress], *, *, *, *)).thenReturn(Future.successful(DeskproTicketResponseNotFound))
      }

      def fails() = {
        when(aMock.createResponse(*, *[LaxEmailAddress], *, *, *, *)).thenReturn(Future.successful(DeskproTicketResponseFailure))
      }

      def verifyCalledWith(ticketId: Int, email: LaxEmailAddress, message: String) = {
        verify(aMock).createResponse(eqTo(ticketId), eqTo(email), eqTo(message), *, *, *)
      }
    }

    object GetTicketsForUser {

      def succeeds(tickets: List[DeskproTicket]) = {
        when(aMock.getTicketsForUser(*[LaxEmailAddress], *, *)).thenReturn(Future.successful(tickets))
      }

      def verifyCalledWith(email: LaxEmailAddress, status: Option[String]) = {
        verify(aMock).getTicketsForUser(eqTo(email), eqTo(status), *)
      }
    }
  }
}
