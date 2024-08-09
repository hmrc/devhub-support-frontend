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

package uk.gov.hmrc.devhubsupportfrontend.security

import scala.concurrent.{ExecutionContext, Future}

import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import uk.gov.hmrc.apiplatform.modules.tpd.session.domain.models.{UserSession, UserSessionId}
import uk.gov.hmrc.devhubsupportfrontend.controllers.BaseController
import uk.gov.hmrc.devhubsupportfrontend.config.AppConfig
import uk.gov.hmrc.devhubsupportfrontend.controllers.models.MaybeUserRequest
import uk.gov.hmrc.devhubsupportfrontend.connectors.ThirdPartyDeveloperConnector

trait DevHubAuthorization extends FrontendHeaderCarrierProvider with CookieEncoding {
  self: BaseController =>

  val thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector

  implicit val appConfig: AppConfig

  def maybeAtLeastPartLoggedInEnablingMfa(body: MaybeUserRequest[AnyContent] => Future[Result])(implicit ec: ExecutionContext): Action[AnyContent] = Action.async {
    implicit request: MessagesRequest[AnyContent] => loadSession.flatMap(maybeDeveloperSession => body(new MaybeUserRequest(maybeDeveloperSession, request)))
  }

  private[security] def loadSession[A](implicit request: Request[A]): Future[Option[UserSession]] = {
    (for {
      sessionId <- extractUserSessionIdFromCookie(request)
    } yield fetchDeveloperSession(sessionId))
      .getOrElse(Future.successful(None))
  }

  private def fetchDeveloperSession[A](sessionId: UserSessionId)(implicit hc: HeaderCarrier): Future[Option[UserSession]] = {
    thirdPartyDeveloperConnector.fetchSession(sessionId)
  }
}
