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

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

import play.api.http.HeaderNames.AUTHORIZATION
import play.api.http.Status.UNAUTHORIZED
import play.api.libs.json.Json
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.http.metrics.common.API

import uk.gov.hmrc.devhubsupportfrontend.config.AppConfig
import uk.gov.hmrc.devhubsupportfrontend.connectors.models._

class DeskproHorizonConnector @Inject() (http: HttpClientV2, config: AppConfig, metrics: ConnectorMetrics)(implicit val ec: ExecutionContext)
    extends ApplicationLogger {

  lazy val serviceBaseUrl: String = config.deskproHorizonUrl
  val api                         = API("deskpro-horizon")

  def createTicket(deskproHorizonTicket: DeskproHorizonTicketRequest)(implicit hc: HeaderCarrier): Future[DeskproHorizonTicketResponse] = metrics.record(api) {
    http.post(url"${requestUrl("/api/v2/tickets")}")
      .withProxy
      .withBody(Json.toJson(deskproHorizonTicket))
      .setHeader(AUTHORIZATION -> config.deskproHorizonApiKey)
      .execute[Either[UpstreamErrorResponse, DeskproHorizonTicketResponse]]
      .map { result =>
        (result match {
          case Right(response) => {
            logger.info(s"Deskpro horizon ticket '${deskproHorizonTicket.subject}' created successfully")
            response
          }

          case Left(err @ UpstreamErrorResponse(msg, UNAUTHORIZED, _, _)) =>
            logger.error(s"Deskpro horizon ticket creation failed due to unauthorized error for: ${deskproHorizonTicket.subject}")
            logger.error(msg)
            throw (err)

          case Left(err @ UpstreamErrorResponse(msg, _, _, _)) =>
            // TODO: ************************* FIX THE LOG MESSAGE
            logger.error(s"Deskpro horizon ticket creation failed due to ??? for: ${deskproHorizonTicket.subject}")
            logger.error(msg)
            throw (err)
        })
      }
  }

  override def toString = "DeskproHorizonConnector()"

  private def requestUrl[B, A](uri: String): String = s"$serviceBaseUrl$uri"
}
