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
import uk.gov.hmrc.devhubsupportfrontend.domain.models.SupportSessionId
import uk.gov.hmrc.devhubsupportfrontend.services._
import uk.gov.hmrc.devhubsupportfrontend.views.html.SupportEnquiryInitialChoiceView

@Singleton
class SupportEnquiryInitialChoiceController @Inject() (
    mcc: MessagesControllerComponents,
    val cookieSigner: CookieSigner,
    val errorHandler: ErrorHandler,
    val thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
    supportService: SupportService,
    supportEnquiryInitialChoiceView: SupportEnquiryInitialChoiceView
  )(implicit val ec: ExecutionContext,
    val appConfig: AppConfig
  ) extends AbstractController(mcc) {

  def page(): Action[AnyContent] = maybeAtLeastPartLoggedInEnablingMfa { implicit request =>
    if (fullyloggedInDeveloper(request).isDefined) {
      // If user is logged on then redirect to the list of tickets for that user
      Future.successful(Redirect(routes.TicketController.ticketListPage()))
    } else {
      // Else redirect to the create ticket page
      Future.successful(Redirect(routes.SupportEnquiryInitialChoiceController.startPage()))
    }
  }

  def startPage(): Action[AnyContent] = maybeAtLeastPartLoggedInEnablingMfa { implicit request =>
    Future.successful(Ok(supportEnquiryInitialChoiceView(fullyloggedInDeveloper, SupportEnquiryInitialChoiceForm.form)))
  }

  def submit(): Action[AnyContent] = maybeAtLeastPartLoggedInEnablingMfa { implicit request =>
    def handleValidForm(form: SupportEnquiryInitialChoiceForm): Future[Result] = {
      val sessionId = extractSupportSessionIdFromCookie(request).getOrElse(SupportSessionId.random)
      supportService.createFlow(sessionId, form.initialChoice)

      form.initialChoice match {
        case SupportData.UsingAnApi.id           => Future.successful(withSupportCookie(Redirect(routes.HelpWithUsingAnApiController.page()), sessionId))
        case SupportData.SigningIn.id            => Future.successful(withSupportCookie(Redirect(routes.HelpWithSigningInController.page()), sessionId))
        case SupportData.SettingUpApplication.id => Future.successful(withSupportCookie(Redirect(routes.HelpWithApplicationsController.page()), sessionId))
        case SupportData.NoneOfTheAbove.id       => Future.successful(withSupportCookie(Redirect(routes.SupportDetailsController.supportDetailsPage()), sessionId))
        case _                                   => Future.successful(withSupportCookie(Redirect(routes.SupportDetailsController.supportDetailsPage()), sessionId))
      }
    }

    def handleInvalidForm(formWithErrors: Form[SupportEnquiryInitialChoiceForm]): Future[Result] = {
      Future.successful(BadRequest(supportEnquiryInitialChoiceView(fullyloggedInDeveloper, formWithErrors)))
    }

    SupportEnquiryInitialChoiceForm.form.bindFromRequest().fold(handleInvalidForm, handleValidForm)
  }
}

object SupportEnquiryInitialChoiceController {}
