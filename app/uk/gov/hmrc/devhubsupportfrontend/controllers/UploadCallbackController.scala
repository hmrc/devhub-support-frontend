/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.crypto.CookieSigner
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.devhubsupportfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.devhubsupportfrontend.connectors.{Reference, ThirdPartyDeveloperConnector}
import uk.gov.hmrc.devhubsupportfrontend.controllers.AbstractController
import uk.gov.hmrc.devhubsupportfrontend.services.UpscanCallbackDispatcher
import uk.gov.hmrc.devhubsupportfrontend.utils.HttpUrlFormat

import java.net.URL
import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

sealed trait CallbackBody {
  def reference: Reference
}

case class ReadyCallbackBody(
    reference: Reference,
    downloadUrl: URL,
    uploadDetails: UploadDetails
  ) extends CallbackBody

case class FailedCallbackBody(
    reference: Reference,
    failureDetails: ErrorDetails
  ) extends CallbackBody

object CallbackBody {

  implicit val UploadDetailsFormat: Reads[UploadDetails] = Json.reads[UploadDetails]
  implicit val ErrorDetailsFormat: Reads[ErrorDetails]   = Json.reads[ErrorDetails]

  implicit val ReadyCallbackBodyFormat: Reads[ReadyCallbackBody] = {
    implicit val urlFormat: Format[URL] = HttpUrlFormat.format
    Json.reads[ReadyCallbackBody]
  }

  implicit val failedCallbackBodyFormat: Reads[FailedCallbackBody] = Json.reads[FailedCallbackBody]

  implicit val callbackBodyFormat: Reads[CallbackBody] =
    (json: JsValue) =>
      json \ "fileStatus" match {
        case JsDefined(JsString("READY"))  => json.validate[ReadyCallbackBody]
        case JsDefined(JsString("FAILED")) => json.validate[FailedCallbackBody]
        case JsDefined(value)              => JsError(s"Invalid type discriminator: $value")
        case _                             => JsError(s"Missing type discriminator")
      }
}

case class UploadDetails(
    uploadTimestamp: Instant,
    checksum: String,
    fileMimeType: String,
    fileName: String,
    size: Long
  )

case class ErrorDetails(
    failureReason: String,
    message: String
  )

@Singleton
class UploadCallbackController @Inject() (
    val cookieSigner: CookieSigner,
    val errorHandler: ErrorHandler,
    val thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
    upscanCallbackDispatcher: UpscanCallbackDispatcher,
    mcc: MessagesControllerComponents
  )(implicit val ec: ExecutionContext,
    val appConfig: AppConfig
  ) extends AbstractController(mcc) {

  //  def closeTicket(ticketId: Int): Action[AnyContent] = loggedInAction { implicit request =>
  //    ticketService.closeTicket(ticketId)
  //      .map {
  //        case DeskproTicketCloseSuccess  => Redirect(routes.TicketController.ticketListPage().url)
  //        case DeskproTicketCloseNotFound => InternalServerError
  //        case DeskproTicketCloseFailure  => InternalServerError
  //      }
  //  }

  def callback: Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"Received callback notification [${request.body}]")
    withJsonBodyFromAnyContent[CallbackBody] { feedback =>
      upscanCallbackDispatcher.handleCallback(feedback).map(_ => Ok)
    }
  }

  def withJsonBodyFromAnyContent[T](f: T => Future[Result])(implicit request: Request[AnyContent], reads: Reads[T], d: DummyImplicit): Future[Result] = {
    request.body.asJson match {
      case Some(json) => withJson(json)(f)
      case _          => Future.successful(BadRequest("Invalid payload"))
    }
  }

  private def withJson[T](json: JsValue)(f: T => Future[Result])(implicit reads: Reads[T]): Future[Result] = {
    Try(json.validate[T]) match {
      case Success(JsSuccess(payload, _)) => f(payload)
      case Success(JsError(errs))         => Future.successful(BadRequest("Invalid payload: " + JsError.toJson(errs)))
      // $COVERAGE-OFF$ We wont be able to reach here as play json parser will scoop up any non json errors
      case Failure(e)                     => Future.successful(BadRequest("Invalid payload: " + e.getMessage))
      // $COVERAGE-ON$
    }
  }
}
