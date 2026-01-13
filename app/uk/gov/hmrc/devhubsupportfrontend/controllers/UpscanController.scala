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

import play.api.libs.crypto.CookieSigner
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.filters.headers.SecurityHeadersFilter

import uk.gov.hmrc.devhubsupportfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.devhubsupportfrontend.connectors.{ThirdPartyDeveloperConnector, UpscanInitiateConnector}
import uk.gov.hmrc.devhubsupportfrontend.controllers.models.Forms
import uk.gov.hmrc.devhubsupportfrontend.services.FileUploadService

@Singleton
class UpscanController @Inject() (
    mcc: MessagesControllerComponents,
    val cookieSigner: CookieSigner,
    val errorHandler: ErrorHandler,
    val thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
    upscanInitiateConnector: UpscanInitiateConnector,
    fileUploadService: FileUploadService
  )(implicit val ec: ExecutionContext,
    val appConfig: AppConfig
  ) extends AbstractController(mcc) {

  /** Returns fresh Upscan upload fields for multi-file uploads. Called by JavaScript after each successful file upload to refresh the Upscan form with new upscan fields for the
    * upload
    */
  def initiateUpscan(): Action[AnyContent] = loggedInAction { implicit request =>
    upscanInitiateConnector.initiate().map(upscanInitiateResponse =>
      Ok(Json.toJson(upscanInitiateResponse))
    )
  }

  // GET /upscan/file-posted
  final def markFileUploadAsPosted(): Action[AnyContent] = Action.async {
    implicit request =>
      Forms.UpscanUploadSuccessForm
        .bindFromRequest()
        .fold(
          _ => {
            logger.error("[markFileUploadAsPosted] Query Parameters from Upscan could not be bound to form")
            Future.successful(BadRequest)
          },
          {
            s3UploadSuccess => fileUploadService.markFileAsPosted(s3UploadSuccess.key).map(_ => Created)
          }
        )
  }

  // GET /upscan/file-rejected
  final val markFileUploadAsRejected: Action[AnyContent] = Action.async { implicit request =>
    Forms.UpscanUploadErrorForm
      .bindFromRequest()
      .fold(
        _ => {
          logger.error("[markFileUploadAsRejected] Query Parameters from Upscan could not be bound to form")
          Future.successful(InternalServerError)
        },
        s3UploadError => {
          fileUploadService.markFileAsRejected(s3UploadError).map { _ => Ok }
        }
      )
  }

  // GET /upscan/:reference/status
  final def checkFileUploadStatus(reference: String): Action[AnyContent] = Action.async { implicit request =>
    fileUploadService.getFileVerificationStatus(reference).map {
      case Some(verificationStatus) =>
        Ok(Json.toJson(verificationStatus))
      case None                     =>
        logger.error(s"[checkFileVerificationStatus] No File exists for UpscanRef: '$reference'")
        NotFound
    }
  }

  def upscanSuccessRedirect: Action[AnyContent] = Action { _ =>
    overrideIframeHeaders(Ok(""))
  }

  private def overrideIframeHeaders(result: Result) = {
    result.withHeaders(
      SecurityHeadersFilter.X_FRAME_OPTIONS_HEADER         -> "ALLOWALL",
      SecurityHeadersFilter.CONTENT_SECURITY_POLICY_HEADER -> "frame-ancestors *"
    )
  }
}
