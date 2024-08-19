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

package uk.gov.hmrc.devhubsupportfrontend.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.http.metrics.common.API

object ApiPlatformDeskproConnector {
  case class Config(serviceBaseUrl: String)

  case class Person(
      name: String,
      email: String
    )

  case class CreateTicketRequest(
      person: Person,
      subject: String,
      message: String,
      apiName: Option[String] = None,
      applicationId: Option[String] = None,
      organisation: Option[String] = None,
      supportReason: Option[String] = None,
      teamMemberEmailAddress: Option[String] = None
    )

  case class CreateTicketResponse(ref: String)

  implicit val personFormat: Format[Person]                             = Json.format[Person]
  implicit val createTicketRequestFormat: Format[CreateTicketRequest]   = Json.format[CreateTicketRequest]
  implicit val createTicketResponseFormat: Format[CreateTicketResponse] = Json.format[CreateTicketResponse]
}

@Singleton
class ApiPlatformDeskproConnector @Inject() (http: HttpClientV2, config: ApiPlatformDeskproConnector.Config, metrics: ConnectorMetrics)(implicit val ec: ExecutionContext)
    extends ApplicationLogger {

  import ApiPlatformDeskproConnector._

  val api = API("api-platform-deskpro")

  def createTicket(createRequest: CreateTicketRequest)(implicit hc: HeaderCarrier): Future[String] = metrics.record(api) {
    http.post(url"${config.serviceBaseUrl}/ticket")
      .withBody(Json.toJson(createRequest))
      .execute[CreateTicketResponse]
      .map(_.ref)
  }
}
