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

package uk.gov.hmrc.devhubsupportfrontend.mocks.services

import scala.concurrent.Future.successful

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

import uk.gov.hmrc.devhubsupportfrontend.domain.models.DeskproTicket
import uk.gov.hmrc.devhubsupportfrontend.services.TicketService

trait TicketServiceMockModule extends MockitoSugar with ArgumentMatchersSugar {

  trait AbstractTicketServiceMock {
    def aMock: TicketService

    object FetchTicket {

      def succeeds(ticket: Option[DeskproTicket]) =
        when(aMock.fetchTicket(*)(*)).thenReturn(successful(ticket))
    }

    object GetTicketsForUser {

      def succeeds(tickets: List[DeskproTicket]) =
        when(aMock.getTicketsForUser(*[LaxEmailAddress], *)(*)).thenReturn(successful(tickets))
    }
  }

  object TicketServiceMock extends AbstractTicketServiceMock {
    val aMock: TicketService = mock[TicketService]
  }
}
