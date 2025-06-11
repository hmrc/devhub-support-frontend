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
import uk.gov.hmrc.devhubsupportfrontend.connectors.ApiPlatformDeskproConnector.{DeskproTicketCloseFailure, DeskproTicketCloseNotFound, DeskproTicketCloseSuccess}
import uk.gov.hmrc.devhubsupportfrontend.connectors.ThirdPartyDeveloperConnector
import uk.gov.hmrc.devhubsupportfrontend.services._
import uk.gov.hmrc.devhubsupportfrontend.views.html.{TicketListView, TicketView}

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
}

@Singleton
class TicketController @Inject() (
    mcc: MessagesControllerComponents,
    val cookieSigner: CookieSigner,
    val errorHandler: ErrorHandler,
    val thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
    ticketService: TicketService,
    ticketListView: TicketListView,
    ticketView: TicketView
  )(implicit val ec: ExecutionContext,
    val appConfig: AppConfig
  ) extends AbstractController(mcc) {

  import TicketController._

  def ticketListPage(): Action[AnyContent] = loggedInAction { implicit request =>
    def doSearch(form: FilterForm) = {
      val getResolvedTickets = (form.status == Some("resolved"))
      val queryForm          = filterForm.fill(form)

      ticketService.getTicketsForUser(request.userSession.developer.email, getResolvedTickets).map(tickets => Ok(ticketListView(queryForm, Some(request.userSession), tickets)))
    }

    def handleValidForm(form: FilterForm) = {
      doSearch(form)
    }

    def handleInvalidForm(form: Form[FilterForm]) = {
      val defaultForm = FilterForm()
      doSearch(defaultForm)
    }

    filterForm.bindFromRequest().fold(handleInvalidForm, handleValidForm)
  }

  def ticketPage(ticketId: Int): Action[AnyContent] = loggedInAction { implicit request =>
    ticketService.fetchTicket(ticketId).map {
      case Some(ticket) if ticket.personEmail == request.userSession.developer.email => Ok(ticketView(Some(request.userSession), ticket))
      case _                                                                         => NotFound
    }
  }

  def closeTicket(ticketId: Int): Action[AnyContent] = loggedInAction { implicit request =>
    ticketService.closeTicket(ticketId)
      .map {
        case DeskproTicketCloseSuccess  => Redirect(routes.TicketController.ticketListPage().url)
        case DeskproTicketCloseNotFound => InternalServerError
        case DeskproTicketCloseFailure  => InternalServerError
      }
  }
}
