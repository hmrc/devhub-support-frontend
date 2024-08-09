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

package uk.gov.hmrc.devhubsupportfrontend.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.Logging
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, SessionId => _, _}
import uk.gov.hmrc.play.http.metrics.common.API

import uk.gov.hmrc.devhubsupportfrontend.config.AppConfig
import uk.gov.hmrc.apiplatform.modules.tpd.session.domain.models.UserSession
import uk.gov.hmrc.apiplatform.modules.tpd.session.domain.models.UserSessionId

@Singleton
class ThirdPartyDeveloperConnector @Inject() (
  http: HttpClient,
  config: AppConfig,
  metrics: ConnectorMetrics
)(
  implicit val ec: ExecutionContext
) extends CommonResponseHandlers with Logging {

  lazy val serviceBaseUrl: String = config.thirdPartyDeveloperUrl

  val api: API                    = API("third-party-developer")

  def fetchSession(sessionId: UserSessionId)(implicit hc: HeaderCarrier): Future[Option[UserSession]] = metrics.record(api) {
    http.GET[Option[UserSession]](s"$serviceBaseUrl/session/$sessionId")
  }
}
