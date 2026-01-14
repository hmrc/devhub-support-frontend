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
import uk.gov.hmrc.devhubsupportfrontend.controllers.FilePostedController.UpscanUploadSuccessForm
import uk.gov.hmrc.devhubsupportfrontend.domain.models.upscan.{JourneyId, S3UploadSuccess}
import uk.gov.hmrc.devhubsupportfrontend.services.FileUploadService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

object FilePostedController {
  val UpscanUploadSuccessForm = Form[S3UploadSuccess](
    mapping(
      "key"    -> nonEmptyText,
      "bucket" -> optional(nonEmptyText)
    )(S3UploadSuccess.apply)(o => Some((o.key, o.bucket)))
  )
}

@Singleton
class FilePostedController @Inject() (
    mcc: MessagesControllerComponents,
    val cookieSigner: CookieSigner,
    val errorHandler: ErrorHandler,
    val thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
    fileUploadService: FileUploadService
  )(implicit val ec: ExecutionContext,
    val appConfig: AppConfig
  ) extends AbstractController(mcc) {


  // GET /journey/:journeyId/file-posted
  final def asyncMarkFileUploadAsPosted(implicit journeyId: JourneyId): Action[AnyContent] = Action.async {
    implicit request =>
      UpscanUploadSuccessForm
        .bindFromRequest()
        .fold(
          _ => {
            logger.error("[asyncMarkFileUploadAsPosted] Query Parameters from Upscan could not be bound to form")
            logger.debug(s"[asyncMarkFileUploadAsPosted] Query Params Received: ${request.queryString}")
            Future.successful(BadRequest)
          },
          s3UploadSuccess => fileUploadService.markFileAsPosted(s3UploadSuccess.key).map(_ => Created)
        )
  }
}
