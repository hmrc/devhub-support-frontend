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

import play.api.libs.json.{Json, OFormat}

import uk.gov.hmrc.devhubsupportfrontend.connectors.ApiPlatformDeskproConnector

sealed trait AuditAction {
  val auditType: String
}

private[services] case class CreateTicketDetails(
    fullName: String,
    email: String,
    subject: String,
    message: String,
    apiName: Option[String] = None,
    applicationId: Option[String] = None,
    organisation: Option[String] = None,
    supportReason: Option[String] = None,
    teamMemberEmail: Option[String] = None
  )

private[services] object CreateTicketDetails {

  def apply(createTicketRequest: ApiPlatformDeskproConnector.CreateTicketRequest): CreateTicketDetails = CreateTicketDetails(
    fullName = createTicketRequest.fullName,
    email = createTicketRequest.email,
    subject = createTicketRequest.subject,
    message = createTicketRequest.message,
    apiName = createTicketRequest.apiName,
    applicationId = createTicketRequest.applicationId,
    organisation = createTicketRequest.organisation,
    supportReason = createTicketRequest.supportReason,
    teamMemberEmail = createTicketRequest.teamMemberEmail
  )

  implicit val format: OFormat[CreateTicketDetails] = Json.format[CreateTicketDetails]
}

case class CreateTicketAuditAction(createTicketRequest: ApiPlatformDeskproConnector.CreateTicketRequest) extends AuditAction {
  val auditType = "TicketCreated"
}
