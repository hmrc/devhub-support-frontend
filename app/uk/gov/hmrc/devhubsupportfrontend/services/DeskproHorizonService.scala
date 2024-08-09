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

package uk.gov.hmrc.devhubsupportfrontend.service

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.devhubsupportfrontend.connectors.DeskproHorizonConnector
import uk.gov.hmrc.devhubsupportfrontend.config.AppConfig
import uk.gov.hmrc.devhubsupportfrontend.domain.models.connectors.DeskproHorizonTicketPerson
import uk.gov.hmrc.devhubsupportfrontend.domain.models.connectors.DeskproHorizonTicket
import uk.gov.hmrc.devhubsupportfrontend.domain.models.connectors.DeskproHorizonTicketMessage
import uk.gov.hmrc.devhubsupportfrontend.domain.models.connectors.HorizonTicketRef

@Singleton
class DeskproHorizonService @Inject() (
    val deskproHorizonConnector: DeskproHorizonConnector,
    val appConfig: AppConfig
  )(implicit val ec: ExecutionContext
  ) {

  // def submitSupportEnquiry(userId: Option[UserId], supportEnquiry: SupportEnquiryForm)(implicit hc: HeaderCarrier): Future[HorizonTicketRef] = {
  //   val ticket = createFromSupportEnquiry(supportEnquiry)

  //   deskproHorizonConnector.createTicket(ticket)
  // }

  // def createFromSupportEnquiry(supportEnquiry: SupportEnquiryForm) = {
  //   val message =
  //     s"""${supportEnquiry.email} has submitted the following support enquiry:
  //        |
  //        |${supportEnquiry.comments}
  //        |
  //        |Please send them a response within 2 working days.
  //        |HMRC Developer Hub""".stripMargin

  //   DeskproHorizonTicket(
  //     person = DeskproHorizonTicketPerson(supportEnquiry.fullname, supportEnquiry.email.toLaxEmail.text),
  //     subject = s"${appConfig.title}: Support Enquiry",
  //     brand = appConfig.deskproHorizonBrand,
  //     message = DeskproHorizonTicketMessage.fromRaw(message)
  //   )
  // }
}
