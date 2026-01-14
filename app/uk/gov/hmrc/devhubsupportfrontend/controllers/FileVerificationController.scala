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

import org.apache.pekko.actor.{ActorSystem, Scheduler}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileVerificationController @Inject() (
  components: BaseControllerComponents,
  waitingView: WaitingForFileVerificationView,
  actorSystem: ActorSystem,
  fileVerificationService: FileVerificationService,
  override val journeyContextService: JourneyContextService
)(implicit ec: ExecutionContext, appConfig: AppConfig)
    extends BaseController(components) with JourneyContextControllerHelper {

  implicit val scheduler: Scheduler = actorSystem.scheduler

  // GET /file-verification/:reference/status
  final def checkFileVerificationStatus(reference: String): Action[AnyContent] = Action.async { implicit request =>
    whenInSession { implicit journeyId =>
      whenAuthenticated {
        withJourneyContext { implicit journeyContext =>
          fileVerificationService.getFileVerificationStatus(reference).map {
            case Some(verificationStatus) =>
              Logger.info(
                s"[checkFileVerificationStatus] UpscanRef: '$reference', Status: ${verificationStatus.fileStatus}"
              )
              Ok(Json.toJson(verificationStatus))
            case None =>
              Logger.error(s"[checkFileVerificationStatus] No File exists for UpscanRef: '$reference'")
              NotFound
          }
        }
      }
    }
  }

  // GET /journey/:journeyId/file-verification?key
  final def asyncWaitingForFileVerification(journeyId: JourneyId, key: Option[String]): Action[AnyContent] =
    Action.async {
      implicit val journey: JourneyId = journeyId
      key match {
        case None => Future(BadRequest)
        case Some(upscanReference) =>
          val timeoutNanoTime: Long = System.nanoTime() + appConfig.upscanInitialWaitTime.toNanos
          fileVerificationService.waitForUpscanResponse(
            upscanReference,
            appConfig.upscanWaitInterval.toMillis,
            timeoutNanoTime
          )(
            _ => Future(Created),
            Future(Accepted)
          )
      }
    }
}
