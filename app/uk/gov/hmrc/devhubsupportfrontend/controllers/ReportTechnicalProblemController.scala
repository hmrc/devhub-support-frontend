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
import play.api.data.Forms.{mapping, nonEmptyText, optional, text}
import play.api.libs.crypto.CookieSigner
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import uk.gov.hmrc.devhubsupportfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.devhubsupportfrontend.connectors.ThirdPartyDeveloperConnector
import uk.gov.hmrc.devhubsupportfrontend.services._
import uk.gov.hmrc.devhubsupportfrontend.views.html.{ReportTechnicalProblemConfirmationView, ReportTechnicalProblemView}

object ReportTechnicalProblemController {

  case class ReportTechnicalProblemForm(fullName: String, emailAddress: String, whatWereYouDoing: String, whatDoYouNeedHelpWith: String, referrerUrl: Option[String] = None)

  object ReportTechnicalProblemForm {

    def form: Form[ReportTechnicalProblemForm] = Form(
      mapping(
        "fullName"              -> nonEmptyText,
        "emailAddress"          -> FormValidation.emailValidator(),
        "whatWereYouDoing"      -> nonEmptyText,
        "whatDoYouNeedHelpWith" -> nonEmptyText,
        "referrerUrl"           -> optional(text)
      )(ReportTechnicalProblemForm.apply)(ReportTechnicalProblemForm.unapply)
    )
  }
}

@Singleton
class ReportTechnicalProblemController @Inject() (
    mcc: MessagesControllerComponents,
    val cookieSigner: CookieSigner,
    val errorHandler: ErrorHandler,
    val thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
    supportService: SupportService,
    reportTechnicalProblemView: ReportTechnicalProblemView,
    reportTechnicalProblemConfirmationView: ReportTechnicalProblemConfirmationView
  )(implicit val ec: ExecutionContext,
    val appConfig: AppConfig
  ) extends AbstractController(mcc) {

  import ReportTechnicalProblemController._
  val reportTechnicalProblemForm: Form[ReportTechnicalProblemForm] = ReportTechnicalProblemForm.form

  def page(referrerUrl: Option[String]): Action[AnyContent] = maybeAtLeastPartLoggedInEnablingMfa { implicit request =>
    Future.successful(Ok(reportTechnicalProblemView(fullyloggedInDeveloper, reportTechnicalProblemForm, referrerUrl)))
  }

  def action(): Action[AnyContent] = maybeAtLeastPartLoggedInEnablingMfa { implicit request =>
    reportTechnicalProblemForm.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(reportTechnicalProblemView(fullyloggedInDeveloper, formWithErrors, None)))
      },
      data => {
        val userAgent = request.headers.get("User-Agent")
        val sessionId = request.userSession match {
          case Some(session) => Some(session.sessionId.toString())
          case _             => None
        }
        supportService.reportTechnicalProblem(data.fullName, data.emailAddress, data.whatWereYouDoing, data.whatDoYouNeedHelpWith, data.referrerUrl, userAgent, sessionId).map(
          ref =>
            Redirect(routes.ReportTechnicalProblemController.confirmationPage(ref))
        )
      }
    )
  }

  def confirmationPage(ref: String): Action[AnyContent] = maybeAtLeastPartLoggedInEnablingMfa { implicit request =>
    Future.successful(Ok(reportTechnicalProblemConfirmationView(fullyloggedInDeveloper, ref)))
  }
}
