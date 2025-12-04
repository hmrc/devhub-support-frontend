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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.devhubsupportfrontend.connectors.ApiPlatformDeskproConnector
import uk.gov.hmrc.devhubsupportfrontend.connectors.ApiPlatformDeskproConnector.{DeskproTicketResponseResult, Attachment}
import uk.gov.hmrc.devhubsupportfrontend.domain.models._

@Singleton
class TicketService @Inject() (
    deskproConnector: ApiPlatformDeskproConnector
  )(implicit val ec: ExecutionContext
  ) extends ApplicationLogger {

  def getTicketsForUser(email: LaxEmailAddress, getResolvedTickets: Boolean)(implicit hc: HeaderCarrier): Future[List[DeskproTicket]] = {
    val status = if (getResolvedTickets) Some("resolved") else None
    deskproConnector.getTicketsForUser(email, status, hc)
  }

  def fetchTicket(ticketId: Int)(implicit hc: HeaderCarrier): Future[Option[DeskproTicket]] = {
    deskproConnector.fetchTicket(ticketId, hc)
  }

  def createResponse(
      ticketId: Int,
      userEmail: LaxEmailAddress,
      message: String,
      status: String,
      userName: String,
      newStatus: String,
      attachments: List[Attachment] = List.empty
    )(implicit hc: HeaderCarrier
    ): Future[DeskproTicketResponseResult] = {
    def getMessage() = {
      if (status == "resolved") {
        s"$message<p><strong>$userName reopened this support request</strong></p>"
      } else if (newStatus == "resolved") {
        s"$message<p><strong>$userName marked this support request as resolved</strong></p>"
      } else {
        message
      }
    }
    deskproConnector.createResponse(ticketId, userEmail, getMessage(), newStatus, attachments, hc)
  }
}
