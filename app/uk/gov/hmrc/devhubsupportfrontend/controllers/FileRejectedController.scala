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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.devhubsupportfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.devhubsupportfrontend.connectors.ThirdPartyDeveloperConnector
import uk.gov.hmrc.devhubsupportfrontend.controllers.models.Forms
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
    val appConfig: AppConfig
  ) extends AbstractController(mcc) with FileUploadsControllerHelper with JourneyContextControllerHelper {

  // GET /file-rejected
  final val markFileUploadAsRejected: Action[AnyContent] = Action.async { implicit request =>
    whenInSession { implicit journeyId =>
      withJourneyContext { implicit journeyContext =>
        Forms.UpscanUploadErrorForm
          .bindFromRequest()
          .fold(
            _ => {
              logger.error("[markFileUploadAsRejected] Query Parameters from Upscan could not be bound to form")
              logger.debug(s"[markFileUploadAsRejected] Query Params Received: ${request.queryString}")
              Future.successful(InternalServerError)
            },
            s3UploadError =>
              fileUploadService.markFileAsRejected(s3UploadError).map(_ => BadRequest)
          )
      }
    }
  }
}
