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

package uk.gov.hmrc.devhubsupportfrontend.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.http.metrics.common.API

import uk.gov.hmrc.devhubsupportfrontend.domain.models.DeskproTicket

object ApiPlatformDeskproConnector {

  case class Config(
      serviceBaseUrl: String,
      authToken: String
    )

  case class CreateTicketRequest(
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

  case class CreateTicketResponse(ref: String)

  sealed trait DeskproTicketCloseResult
  object DeskproTicketCloseSuccess  extends DeskproTicketCloseResult
  object DeskproTicketCloseNotFound extends DeskproTicketCloseResult
  object DeskproTicketCloseFailure  extends DeskproTicketCloseResult

  case class GetTicketsByEmailRequest(email: LaxEmailAddress, status: Option[String] = None)

  implicit val createTicketRequestFormat: Format[CreateTicketRequest]     = Json.format[CreateTicketRequest]
  implicit val createTicketResponseFormat: Format[CreateTicketResponse]   = Json.format[CreateTicketResponse]
  implicit val getTicketsByEmailRequest: Format[GetTicketsByEmailRequest] = Json.format[GetTicketsByEmailRequest]
}

@Singleton
class ApiPlatformDeskproConnector @Inject() (http: HttpClientV2, config: ApiPlatformDeskproConnector.Config, metrics: ConnectorMetrics)(implicit val ec: ExecutionContext)
    extends ApplicationLogger {

  import ApiPlatformDeskproConnector._

  val api = API("api-platform-deskpro")

  def createTicket(createRequest: CreateTicketRequest, hc: HeaderCarrier): Future[String] = metrics.record(api) {
    implicit val headerCarrier: HeaderCarrier = hc.copy(authorization = Some(Authorization(config.authToken)))
    http.post(url"${config.serviceBaseUrl}/ticket")
      .withBody(Json.toJson(createRequest))
      .execute[CreateTicketResponse]
      .map(_.ref)
  }

  def getTicketsForUser(email: LaxEmailAddress, status: Option[String], hc: HeaderCarrier): Future[List[DeskproTicket]] = metrics.record(api) {
    implicit val headerCarrier: HeaderCarrier = hc.copy(authorization = Some(Authorization(config.authToken)))
    http.post(url"${config.serviceBaseUrl}/ticket/query")
      .withBody(Json.toJson(GetTicketsByEmailRequest(email, status)))
      .execute[List[DeskproTicket]]
  }

  def fetchTicket(ticketId: Int, hc: HeaderCarrier): Future[Option[DeskproTicket]] = metrics.record(api) {
    implicit val headerCarrier: HeaderCarrier = hc.copy(authorization = Some(Authorization(config.authToken)))
    http.get(url"${config.serviceBaseUrl}/ticket/$ticketId")
      .execute[Option[DeskproTicket]]
  }

  def closeTicket(ticketId: Int, hc: HeaderCarrier): Future[DeskproTicketCloseResult] = metrics.record(api) {
    implicit val headerCarrier: HeaderCarrier = hc.copy(authorization = Some(Authorization(config.authToken)))
    http.post(url"${config.serviceBaseUrl}/ticket/$ticketId/close")
      .execute[HttpResponse]
      .map(response =>
        response.status match {
          case OK        =>
            logger.info(s"Deskpro close ticket '$ticketId' success")
            DeskproTicketCloseSuccess
          case NOT_FOUND =>
            logger.warn(s"Deskpro close ticket '$ticketId' failed Not found")
            DeskproTicketCloseNotFound
          case _         =>
            logger.error(s"Deskpro close ticket '$ticketId' failed status: ${response.status}")
            DeskproTicketCloseFailure
        }
      )
  }
}
