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
import scala.annotation.nowarn
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

import play.api.data.Form
import play.api.libs.crypto.CookieSigner
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.HtmlFormat

import uk.gov.hmrc.devhubsupportfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.devhubsupportfrontend.connectors.ThirdPartyDeveloperConnector
import uk.gov.hmrc.devhubsupportfrontend.controllers.models.MaybeUserRequest
import uk.gov.hmrc.devhubsupportfrontend.domain.models.SupportFlow
import uk.gov.hmrc.devhubsupportfrontend.services._
import uk.gov.hmrc.devhubsupportfrontend.views.html.{HelpWithSigningInView, RemoveAccessCodesView}

object HelpWithSigningInController {
  SupportFlow

  def choose(form: HelpWithSigningInForm)(flow: SupportFlow) =
    flow.copy(
      subSelection = Some(form.choice)
    )
}

@Singleton
class HelpWithSigningInController @Inject() (
    mcc: MessagesControllerComponents,
    val cookieSigner: CookieSigner,
    val errorHandler: ErrorHandler,
    val thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
    supportService: SupportService,
    helpWithSigningInView: HelpWithSigningInView,
    removeAccessCodesView: RemoveAccessCodesView
  )(implicit val ec: ExecutionContext,
    val appConfig: AppConfig
  ) extends AbstractSupportFlowController[HelpWithSigningInForm, Unit](mcc, supportService) {

  import HelpWithSigningInController.choose

  def redirectBack(): Result = Redirect(routes.SupportEnquiryInitialChoiceController.page())

  def filterValidFlow(flow: SupportFlow): Boolean = flow match {
    case SupportFlow(_, SupportData.SigningIn.id, _, _, _, _, _) => true
    case _                                                       => false
  }

  def pageContents(flow: SupportFlow, form: Form[HelpWithSigningInForm], extras: Unit)(implicit request: MaybeUserRequest[AnyContent]): HtmlFormat.Appendable =
    helpWithSigningInView(
      fullyloggedInDeveloper,
      form,
      routes.SupportEnquiryInitialChoiceController.page().url
    )

  def onValidForm(flow: SupportFlow, form: HelpWithSigningInForm)(implicit request: MaybeUserRequest[AnyContent]): Future[Result] = {
    form.choice match {
      case SupportData.AccessCodes.id       => successful(Redirect(routes.HelpWithSigningInController.removeAccessCodesPage()))
      case SupportData.ForgottenPassword.id => successful(Redirect(s"${appConfig.thirdPartyDeveloperFrontendUrl}/developer/forgot-password"))
      case _                                =>
        supportService.updateWithDelta(choose(form))(flow).map { newFlow =>
          Redirect(routes.SupportDetailsController.supportDetailsPage())
        }

    }
  }

  def form(): Form[HelpWithSigningInForm] = HelpWithSigningInForm.form

  // Typically can be successful(Unit) if nothing is needed (see HelpWithUsingAnApiController for use to get api list)
  def extraData()(implicit @nowarn request: MaybeUserRequest[AnyContent]): Future[Unit] = successful(())

  def removeAccessCodesPage(): Action[AnyContent] = maybeAtLeastPartLoggedInEnablingMfa { implicit request =>
    successful(Ok(
      removeAccessCodesView(
        fullyloggedInDeveloper,
        routes.HelpWithSigningInController.page().url,
        appConfig.logInUrl
      )
    ))
  }
}
