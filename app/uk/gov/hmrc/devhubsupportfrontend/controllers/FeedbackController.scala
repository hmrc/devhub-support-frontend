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
import play.api.data.Forms.{mapping, optional, text}
import play.api.libs.crypto.CookieSigner
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import uk.gov.hmrc.devhubsupportfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.devhubsupportfrontend.connectors.ThirdPartyDeveloperConnector
import uk.gov.hmrc.devhubsupportfrontend.domain.models.SupportSessionId
import uk.gov.hmrc.devhubsupportfrontend.services._
import uk.gov.hmrc.devhubsupportfrontend.views.html.{FeedbackConfirmationView, FeedbackView}

object FeedbackController {

  case class FeedbackForm(
      whatWereYouDoing: String,
      feedback: String,
      url: Option[String]
    )

  object FeedbackForm {

    def form: Form[FeedbackForm] = Form(
      mapping(
        "whatWereYouDoing" -> text
          .verifying(
            "reportproblem.whatwereyoudoing.error.required",
            whatwereyoudoing => whatwereyoudoing.nonEmpty
          )
          .verifying("feedback.whatwereyoudoing.error.length", whatwereyoudoing => whatwereyoudoing.length <= 1000),
        "feedback"         -> text
          .verifying(
            "feedback.feedback.error.required",
            feedback => feedback.nonEmpty
          )
          .verifying("reportproblem.feedback.error.length", feedback => feedback.length <= 1000),
        "url"              -> optional(text)
      )(FeedbackForm.apply)(FeedbackForm.unapply)
    )
  }
}

@Singleton
class FeedbackController @Inject() (
    mcc: MessagesControllerComponents,
    val cookieSigner: CookieSigner,
    val errorHandler: ErrorHandler,
    val thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
    supportService: SupportService,
    feedbackView: FeedbackView,
    feedbackConfirmationView: FeedbackConfirmationView
  )(implicit val ec: ExecutionContext,
    val appConfig: AppConfig
  ) extends AbstractController(mcc) {

  import FeedbackController._
  val feedbackForm: Form[FeedbackForm] = FeedbackForm.form

  def page(): Action[AnyContent] = maybeAtLeastPartLoggedInEnablingMfa { implicit request =>
    Future.successful(Ok(feedbackView(fullyloggedInDeveloper, feedbackForm)))
  }

  def action(): Action[AnyContent] = maybeAtLeastPartLoggedInEnablingMfa { implicit request =>
    feedbackForm.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(feedbackView(fullyloggedInDeveloper, formWithErrors)))
      },
      data => {
        if (fullyloggedInDeveloper.isEmpty && data.url.isDefined) {
          logger.warn("Honeypot field triggered via 'Feedback' support form")
          val sessionId = extractSupportSessionIdFromCookie(request).getOrElse(SupportSessionId.random)
          Future.successful(withSupportCookie(Ok(feedbackConfirmationView(fullyloggedInDeveloper)), sessionId))
        } else {
          val userAgent = request.headers.get("User-Agent")
          val sessionId = request.userSession match {
            case Some(session) => Some(session.sessionId.toString())
            case _             => None
          }
          val fullName  = request.userSession match {
            case Some(session) => s"${session.developer.firstName} ${session.developer.lastName}"
            case _             => "feedback"
          }
          val email     = request.userSession match {
            case Some(session) => session.developer.email.text
            case _             => "feedback@developerhub.gov.uk"
          }

          supportService.giveFeedback(
            fullName,
            email,
            data.whatWereYouDoing,
            data.feedback,
            userAgent,
            sessionId
          ).map(ref =>
            Redirect(routes.FeedbackController.confirmationPage())
          )
        }
      }
    )
  }

  def confirmationPage(): Action[AnyContent] = maybeAtLeastPartLoggedInEnablingMfa { implicit request =>
    Future.successful(Ok(feedbackConfirmationView(fullyloggedInDeveloper)))
  }
}
