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
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

import play.api.data.Form
import play.api.libs.crypto.CookieSigner
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.HtmlFormat

import uk.gov.hmrc.devhubsupportfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.devhubsupportfrontend.connectors.ThirdPartyDeveloperConnector
import uk.gov.hmrc.devhubsupportfrontend.controllers.models.MaybeUserRequest
import uk.gov.hmrc.devhubsupportfrontend.controllers.security.SupportCookie
import uk.gov.hmrc.devhubsupportfrontend.domain.models.SupportFlow
import uk.gov.hmrc.devhubsupportfrontend.services.SupportService
import uk.gov.hmrc.devhubsupportfrontend.views.html.{GivingTeamMemberAccessView, HelpWithApplicationsView}

object HelpWithApplicationsController {

  def choose(form: HelpWithApplicationsForm)(flow: SupportFlow) =
    flow.copy(
      subSelection = Some(form.choice)
    )
}

@Singleton
class HelpWithApplicationsController @Inject() (
    mcc: MessagesControllerComponents,
    val cookieSigner: CookieSigner,
    val errorHandler: ErrorHandler,
    val thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
    supportService: SupportService,
    helpWithApplicationsView: HelpWithApplicationsView,
    givingTeamMemberAccessView: GivingTeamMemberAccessView
  )(implicit val ec: ExecutionContext,
    val appConfig: AppConfig
  ) extends AbstractSupportFlowController[HelpWithApplicationsForm, Unit](mcc, supportService) with SupportCookie {

  import HelpWithApplicationsController._

  def redirectBack(): Result = Redirect(routes.SupportEnquiryInitialChoiceController.page())

  def filterValidFlow(flow: SupportFlow): Boolean = flow match {
    case SupportFlow(_, SupportData.SettingUpApplication.id, _, _, _, _, _) => true
    case _                                                                  => false
  }

  def pageContents(flow: SupportFlow, form: Form[HelpWithApplicationsForm], extras: Unit)(implicit request: MaybeUserRequest[AnyContent]): HtmlFormat.Appendable =
    helpWithApplicationsView(
      fullyloggedInDeveloper,
      form,
      routes.SupportEnquiryInitialChoiceController.page().url
    )

  def onValidForm(flow: SupportFlow, form: HelpWithApplicationsForm)(implicit request: MaybeUserRequest[AnyContent]): Future[Result] = {
    form.choice match {
      case SupportData.GivingTeamMemberAccess.id                                                   => successful(Redirect(routes.HelpWithApplicationsController.givingTeamMembersAccess()))
      case SupportData.CompletingTermsOfUseAgreement.id | SupportData.GeneralApplicationDetails.id =>
        supportService.updateWithDelta(choose(form))(flow).map { newFlow =>
          Redirect(routes.SupportDetailsController.supportDetailsPage())
        }
      case _                                                                                       => throw new RuntimeException("Validation failed to eliminate bad data during Form processing")
    }
  }

  def form(): Form[HelpWithApplicationsForm] = HelpWithApplicationsForm.form

  // Typically can be successful(Unit) if nothing is needed (see HelpWithUsingAnApiController for use to get api list)
  def extraData()(implicit request: MaybeUserRequest[AnyContent]): Future[Unit] = successful(())

  def givingTeamMembersAccess(): Action[AnyContent] = maybeAtLeastPartLoggedInEnablingMfa { implicit request =>
    successful(Ok(
      givingTeamMemberAccessView(
        fullyloggedInDeveloper,
        routes.HelpWithApplicationsController.page().url,
        appConfig.logInUrl
      )
    ))
  }
}
