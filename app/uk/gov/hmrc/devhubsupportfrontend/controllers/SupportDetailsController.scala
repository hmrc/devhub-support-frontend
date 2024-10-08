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
import scala.concurrent.{ExecutionContext, Future}

import play.api.data.Form
import play.api.libs.crypto.CookieSigner
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}

import uk.gov.hmrc.devhubsupportfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.devhubsupportfrontend.connectors.ThirdPartyDeveloperConnector
import uk.gov.hmrc.devhubsupportfrontend.domain.models.{SupportFlow, SupportSessionId}
import uk.gov.hmrc.devhubsupportfrontend.services._
import uk.gov.hmrc.devhubsupportfrontend.views.html.{SupportPageConfirmationView, SupportPageDetailView}

@Singleton
class SupportDetailsController @Inject() (
    mcc: MessagesControllerComponents,
    val cookieSigner: CookieSigner,
    val errorHandler: ErrorHandler,
    val thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
    supportService: SupportService,
    supportPageDetailView: SupportPageDetailView,
    supportPageConfirmationView: SupportPageConfirmationView
  )(implicit val ec: ExecutionContext,
    val appConfig: AppConfig
  ) extends AbstractController(mcc) {

  def supportDetailsPage(): Action[AnyContent] = maybeAtLeastPartLoggedInEnablingMfa { implicit request =>
    def renderPage(flow: SupportFlow) =
      Ok(
        supportPageDetailView(
          fullyloggedInDeveloper,
          SupportDetailsForm.form,
          flow
        )
      )

    val sessionId = extractSupportSessionIdFromCookie(request).getOrElse(SupportSessionId.random)
    supportService.getSupportFlow(sessionId).map(renderPage)
  }

  def submitSupportDetails: Action[AnyContent] = maybeAtLeastPartLoggedInEnablingMfa { implicit request =>
    def renderSupportDetailsPageErrorView(flow: SupportFlow)(form: Form[SupportDetailsForm]) = {
      Future.successful(
        BadRequest(
          supportPageDetailView(
            fullyloggedInDeveloper,
            form,
            flow
          )
        )
      )
    }

    def handleValidForm(sessionId: SupportSessionId, flow: SupportFlow)(form: SupportDetailsForm): Future[Result] = {
      supportService.submitTicket(flow, form).map(_ =>
        withSupportCookie(Redirect(routes.SupportDetailsController.supportConfirmationPage()), sessionId)
      )
    }

    def handleInvalidForm(flow: SupportFlow)(formWithErrors: Form[SupportDetailsForm]): Future[Result] = {
      renderSupportDetailsPageErrorView(flow)(formWithErrors)
    }

    val sessionId = extractSupportSessionIdFromCookie(request).getOrElse(SupportSessionId.random)

    supportService.getSupportFlow(sessionId).flatMap { flow =>
      SupportDetailsForm.form.bindFromRequest().fold(handleInvalidForm(flow), handleValidForm(sessionId, flow))
    }
  }

  def supportConfirmationPage(): Action[AnyContent] = maybeAtLeastPartLoggedInEnablingMfa { implicit request =>
    def renderSupportConfirmationPage(flow: SupportFlow) =
      Ok(
        supportPageConfirmationView(
          fullyloggedInDeveloper,
          flow
        )
      )

    extractSupportSessionIdFromCookie(request).map(sessionId =>
      supportService.getSupportFlow(sessionId).map(renderSupportConfirmationPage)
    )
      .getOrElse(Future.successful(Redirect(routes.SupportEnquiryInitialChoiceController.page())))
  }
}
