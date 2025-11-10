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

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.crypto.CookieSigner
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import uk.gov.hmrc.devhubsupportfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.devhubsupportfrontend.connectors.ApiPlatformDeskproConnector._
import uk.gov.hmrc.devhubsupportfrontend.connectors.{ThirdPartyDeveloperConnector, UpscanInitiateConnector}
import uk.gov.hmrc.devhubsupportfrontend.domain.models.upscan.services.{UpscanFileReference, UpscanInitiateResponse}
import uk.gov.hmrc.devhubsupportfrontend.services._
import uk.gov.hmrc.devhubsupportfrontend.views.html._

object TicketController {

  case class FilterForm(
      status: Option[String] = Some("open")
    )

  val filterForm: Form[FilterForm] = Form(
    mapping(
      "status" -> optional(text)
        .verifying("ticketlist.status.no.choice.field", _.isDefined)
    )(FilterForm.apply)(FilterForm.unapply)
  )

  case class TicketResponseForm(
      response: Option[String],
      status: String,
      action: String,
      fileReferences: List[String]
    )

  val ticketResponseForm: Form[TicketResponseForm] = Form(
    mapping(
      "response"       -> optional(text).verifying("ticketdetails.response.required", _.isDefined),
      "status"         -> nonEmptyText,
      "action"         -> nonEmptyText,
      "fileReferences" -> list(text).transform[List[String]](_.filter(_.nonEmpty), identity)
    )(TicketResponseForm.apply)(TicketResponseForm.unapply)
  )
}

@Singleton
class TicketController @Inject() (
    mcc: MessagesControllerComponents,
    val cookieSigner: CookieSigner,
    val errorHandler: ErrorHandler,
    val thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
    upscanInitiateConnector: UpscanInitiateConnector,
    ticketService: TicketService,
    ticketListView: TicketListView,
    ticketView: TicketView,
    ticketViewWithAttachments: TicketViewWithAttachments
  )(implicit val ec: ExecutionContext,
    val appConfig: AppConfig
  ) extends AbstractController(mcc) {

  import TicketController._

  def ticketListPage(resolved: Boolean): Action[AnyContent] = loggedInAction { implicit request =>
    ticketService.getTicketsForUser(request.userSession.developer.email, resolved)
      .map(tickets => Ok(ticketListView(resolved, Some(request.userSession), tickets)))
  }

  def ticketPage(ticketId: Int): Action[AnyContent] = loggedInAction { implicit request =>
    ticketService.fetchTicket(ticketId).map {
      case Some(ticket) if ticket.personEmail == request.userSession.developer.email => Ok(ticketView(ticketResponseForm, Some(request.userSession), ticket))
      case _                                                                         => NotFound
    }
  }

  def ticketPageWithAttachments(ticketId: Int, key: Option[String] = None): Action[AnyContent] = loggedInAction { implicit request =>
    val successRedirectUrl = appConfig.devhubSupportFrontendUrl + routes.TicketController.ticketPageWithAttachments(ticketId, None).url
    val errorRedirectUrl   = appConfig.devhubSupportFrontendUrl + routes.TicketController.ticketPageWithAttachments(ticketId, None).url

    val ticketResponseFormWithFileRef = ticketResponseForm.fill(TicketResponseForm(None, "open", "", key.map(List(_)).getOrElse(List.empty)))
    val userEmail                     = request.userSession.developer.email

    for {
      maybeTicket            <- ticketService.fetchTicket(ticketId)
      upscanInitiateResponse <- upscanInitiateConnector.initiate(Some(successRedirectUrl), Some(errorRedirectUrl))
    } yield (maybeTicket, upscanInitiateResponse) match {
      case (Some(ticket), upscan) if ticket.personEmail == userEmail =>
        Ok(ticketViewWithAttachments(ticketResponseFormWithFileRef, Some(request.userSession), ticket, upscan))
      case _                                                         =>
        NotFound
    }
  }

  def submitTicketResponse(ticketId: Int): Action[AnyContent] = loggedInAction { implicit request =>
    val requestForm: Form[TicketResponseForm] = ticketResponseForm.bindFromRequest()

    def errors(errors: Form[TicketResponseForm]) =
      ticketService.fetchTicket(ticketId).map {
        case Some(ticket) if ticket.personEmail == request.userSession.developer.email => Ok(ticketView(errors, Some(request.userSession), ticket))
        case _                                                                         => NotFound
      }

    def handleValidForm(validForm: TicketResponseForm) = {

      val newStatus = validForm.action match {
        case "send"  => "awaiting_agent"
        case "close" => "resolved"
      }

      ticketService.createResponse(ticketId, request.email, validForm.response.get, validForm.status, request.displayedName, newStatus, validForm.fileReferences).map {
        case DeskproTicketResponseSuccess  => Redirect(routes.TicketController.ticketListPage().url)
        case DeskproTicketResponseNotFound => InternalServerError
        case DeskproTicketResponseFailure  => InternalServerError
      }
    }

    requestForm.fold(errors, handleValidForm)
  }

  def submitTicketResponseWithAttachments(ticketId: Int): Action[AnyContent] = loggedInAction { implicit request =>
    val requestForm: Form[TicketResponseForm] = ticketResponseForm.bindFromRequest()

    def errors(errors: Form[TicketResponseForm]) =
      ticketService.fetchTicket(ticketId).map {
        case Some(ticket) if ticket.personEmail == request.userSession.developer.email =>
          Ok(ticketViewWithAttachments(errors, Some(request.userSession), ticket, UpscanInitiateResponse(UpscanFileReference(""), "", Map.empty)))
        case _                                                                         => NotFound
      }

    def handleValidForm(validForm: TicketResponseForm) = {
      val newStatus = validForm.action match {
        case "send"  => "awaiting_agent"
        case "close" => "resolved"
      }

      ticketService.createResponse(
        ticketId,
        request.email,
        validForm.response.get,
        validForm.status,
        request.displayedName,
        newStatus,
        validForm.fileReferences
      ).map {
        case DeskproTicketResponseSuccess  => Redirect(routes.TicketController.ticketListPage().url)
        case DeskproTicketResponseNotFound => InternalServerError
        case DeskproTicketResponseFailure  => InternalServerError
      }
    }

    requestForm.fold(errors, handleValidForm)
  }

}
