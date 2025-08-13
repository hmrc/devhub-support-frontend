/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiCategory, ServiceName}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiContext
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.http.metrics.common._

import uk.gov.hmrc.devhubsupportfrontend.config.AppConfig

case class XmlApi(name: String, serviceName: ServiceName, context: ApiContext, description: String, categories: Option[Seq[ApiCategory]] = None)

object XmlApi {
  implicit val formatXmlApi: OFormat[XmlApi] = Json.format[XmlApi]
}

@Singleton
class XmlServicesConnector @Inject() (
    http: HttpClientV2,
    config: AppConfig,
    metrics: ConnectorMetrics
  )(implicit val ec: ExecutionContext
  ) extends Logging {

  lazy val serviceBaseUrl: String = config.xmlServicesUrl

  val api = API("api-platform-xml-services")

  def fetchAllXmlApis()(implicit hc: HeaderCarrier): Future[Seq[XmlApi]] = metrics.record(api) {
    http.get(url"$serviceBaseUrl/api-platform-xml-services/xml/apis").execute[Seq[XmlApi]]
  }
}
