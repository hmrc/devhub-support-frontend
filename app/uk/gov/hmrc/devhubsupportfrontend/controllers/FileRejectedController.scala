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

import play.api.libs.crypto.CookieSigner
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.devhubsupportfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.devhubsupportfrontend.connectors.ThirdPartyDeveloperConnector
import uk.gov.hmrc.devhubsupportfrontend.services.{FileUploadService, JourneyContextService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileRejectedController @Inject() (
                                         mcc: MessagesControllerComponents,
                                         val cookieSigner: CookieSigner,
                                         val errorHandler: ErrorHandler,
                                         val thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
  override val fileUploadService: FileUploadService,
  override val journeyContextService: JourneyContextService
)(implicit val ec: ExecutionContext,
  val appConfig: AppConfig)
    extends AbstractController(mcc) with FileUploadsControllerHelper with JourneyContextControllerHelper {

  // GET /file-rejected
  final val markFileUploadAsRejected: Action[AnyContent] = Action.async { implicit request =>
    whenInSession { implicit journeyId =>
      whenAuthenticated {
        withJourneyContext { implicit journeyContext =>
          Forms.UpscanUploadErrorForm
            .bindFromRequest()
            .fold(
              _ => {
                Logger.error("[markFileUploadAsRejected] Query Parameters from Upscan could not be bound to form")
                Logger.debug(s"[markFileUploadAsRejected] Query Params Received: ${request.queryString}")
                Future.successful(InternalServerError)
              },
              s3UploadError =>
                fileUploadService.markFileAsRejected(s3UploadError).map { _ =>
                  Redirect(routes.ChooseSingleFileController.showChooseFile(None))
                }
            )
        }
      }
    }
  }

  // POST /file-rejected
  final val markFileUploadAsRejectedAsync: Action[AnyContent] = Action.async { implicit request =>
    whenInSession { implicit journeyId =>
      whenAuthenticated {
        rejectedAsyncLogicWithStatus(Created)
      }
    }
  }

  // GET /journey/:journeyId/file-rejected
  final def asyncMarkFileUploadAsRejected(implicit journeyId: JourneyId): Action[AnyContent] = Action.async {
    implicit request =>
      rejectedAsyncLogicWithStatus(NoContent)
  }

  private def rejectedAsyncLogicWithStatus(
    status: => Result
  )(implicit request: Request[AnyContent], journeyId: JourneyId): Future[Result] =
    withJourneyContext { implicit journeyContext =>
      Forms.UpscanUploadErrorForm
        .bindFromRequest()
        .fold(
          _ => {
            Logger.error("[rejectedAsyncLogicWithStatus] Query Parameters from Upscan could not be bound to form")
            Logger.debug(s"[rejectedAsyncLogicWithStatus] Query Params Received: ${request.queryString}")
            Future.successful(BadRequest)
          },
          s3UploadError => fileUploadService.markFileAsRejected(s3UploadError).map(_ => status)
        )
    }
}
