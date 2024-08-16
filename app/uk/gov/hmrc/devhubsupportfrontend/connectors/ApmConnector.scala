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

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiDefinition
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.http.metrics.common.API

object ApmConnector {
  case class Config(serviceBaseUrl: String)
}

@Singleton
class ApmConnector @Inject() (http: HttpClient, config: ApmConnector.Config, metrics: ConnectorMetrics)(implicit ec: ExecutionContext)
    extends CommonResponseHandlers {

  val api = API("api-platform-microservice")

  def fetchApiDefinitionsVisibleToUser(userId: Option[UserId])(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    val queryParams: Seq[(String, String)] = userId.fold(Seq.empty[(String, String)])(id => Seq("developerId" -> id.toString()))

    http.GET[List[ApiDefinition]](s"${config.serviceBaseUrl}/combined-api-definitions", queryParams)
  }
}
