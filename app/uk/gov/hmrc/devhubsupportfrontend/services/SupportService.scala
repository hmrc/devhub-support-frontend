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

package uk.gov.hmrc.devhubsupportfrontend.services

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.common.services.{ApplicationLogger, EitherTHelper}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.devhubsupportfrontend.config.AppConfig
import uk.gov.hmrc.devhubsupportfrontend.connectors.{ApiPlatformDeskproConnector, ApmConnector, XmlServicesConnector}
import uk.gov.hmrc.devhubsupportfrontend.controllers._
import uk.gov.hmrc.devhubsupportfrontend.domain.models.{SupportFlow, _}
import uk.gov.hmrc.devhubsupportfrontend.repositories.SupportFlowRepository

@Singleton
class SupportService @Inject() (
    val apmConnector: ApmConnector,
    deskproConnector: ApiPlatformDeskproConnector,
    xmlServicesConnector: XmlServicesConnector,
    flowRepository: SupportFlowRepository,
    config: AppConfig,
    auditService: AuditService
  )(implicit val ec: ExecutionContext
  ) extends ApplicationLogger {

  val ET = EitherTHelper.make[Throwable]
  val ES = EitherTHelper.make[UpstreamErrorResponse]

  private def fetchSupportFlow(sessionId: SupportSessionId): Future[SupportFlow] = {
    flowRepository.fetchBySessionId(sessionId) map {
      case Some(flow) => flow
      case None       => SupportFlow(sessionId, "unknown", None)
    }
  }

  def getSupportFlow(sessionId: SupportSessionId): Future[SupportFlow] = {
    for {
      flow      <- fetchSupportFlow(sessionId)
      savedFlow <- flowRepository.saveFlow(flow)
    } yield savedFlow
  }

  def updateWithDelta(fn: SupportFlow => SupportFlow)(flow: SupportFlow): Future[SupportFlow] = {
    flowRepository.saveFlow(fn(flow))
  }

  def createFlow(sessionId: SupportSessionId, entrypoint: String): Future[SupportFlow] = {
    flowRepository.saveFlow(SupportFlow(sessionId, entrypoint, None))
  }

  def fetchAllApis(userId: Option[UserId])(implicit hc: HeaderCarrier): Future[List[ApiSummary]] = {
    for {
      restfulApiDefs     <- apmConnector.fetchApiDefinitionsVisibleToUser(userId)
      restfulApiSummaries = restfulApiDefs.map(api => ApiSummary.fromApiDefinition(api))
      xmlApis            <- xmlServicesConnector.fetchAllXmlApis()
      xmlApiSummaries     = xmlApis.map(api => ApiSummary.fromXmlApi(api)).toList
    } yield restfulApiSummaries ++ xmlApiSummaries
  }

  def submitTicket(supportFlow: SupportFlow, form: SupportDetailsForm)(implicit hc: HeaderCarrier): Future[SupportFlow] = {
    val baseDeskproTicket = buildTicket(
      supportFlow,
      form.fullName,
      form.emailAddress,
      form.details
    )

    submitTicket(
      supportFlow,
      baseDeskproTicket.copy(
        organisation = form.organisation.filterNot(_.isBlank),
        teamMemberEmail = form.teamMemberEmailAddress.filterNot(_.isBlank)
      )
    )
  }

  def submitTicket(supportFlow: SupportFlow, form: ApplyForPrivateApiAccessForm)(implicit hc: HeaderCarrier): Future[SupportFlow] = {
    val baseDeskproTicket = buildTicket(
      supportFlow,
      form.fullName,
      form.emailAddress,
      s"Private API documentation access request for Application Id[${form.applicationId}] to ${form.privateApi} API."
    )

    submitTicket(
      supportFlow,
      baseDeskproTicket.copy(
        organisation = Some(form.organisation).filterNot(_.isBlank),
        applicationId = Some(form.applicationId).filterNot(_.isBlank)
      )
    )
  }

  private def buildTicket(supportFlow: SupportFlow, fullName: String, email: String, messageContents: String): ApiPlatformDeskproConnector.CreateTicketRequest = {
    // Entry point is currently the value of the text on the radio button but may not always be so.
    def deriveSupportReason(): String = {
      (supportFlow.entrySelection, supportFlow.subSelection) match {
        case (SupportData.FindingAnApi.id, _)                                                          => SupportData.FindingAnApi.text
        case (SupportData.UsingAnApi.id, Some(SupportData.MakingAnApiCall.id))                         => SupportData.MakingAnApiCall.text
        case (SupportData.UsingAnApi.id, Some(SupportData.GettingExamples.id))                         => SupportData.GettingExamples.text
        case (SupportData.UsingAnApi.id, Some(SupportData.ReportingDocumentation.id))                  => SupportData.ReportingDocumentation.text
        case (SupportData.UsingAnApi.id, Some(SupportData.PrivateApiDocumentation.id))                 => SupportData.PrivateApiDocumentation.text
        case (SupportData.UsingAnApi.id, _)                                                            => SupportData.UsingAnApi.text
        case (SupportData.PrivateApiDocumentation.id, _)                                               => SupportData.PrivateApiDocumentation.text // TODO - fix
        case (SupportData.SigningIn.id, _)                                                             => SupportData.SigningIn.text
        case (SupportData.SettingUpApplication.id, Some(SupportData.CompletingTermsOfUseAgreement.id)) => SupportData.CompletingTermsOfUseAgreement.text
        case (SupportData.SettingUpApplication.id, Some(SupportData.GivingTeamMemberAccess.id))        => SupportData.GivingTeamMemberAccess.text
        case (SupportData.SettingUpApplication.id, _)                                                  => SupportData.SettingUpApplication.text
        case (SupportData.ReportingDocumentation.id, _)                                                => SupportData.ReportingDocumentation.text
        case (SupportData.NoneOfTheAbove.id, _)                                                        => "General Issue"
        case _                                                                                         => throw new RuntimeException("SupportFlow state cannot support ticket creation")
      }
    }

    val supportReason = deriveSupportReason()
    ApiPlatformDeskproConnector.CreateTicketRequest(
      fullName = fullName,
      email = email,
      subject = supportReason,
      message = messageContents,
      supportReason = Some(supportReason),
      apiName = supportFlow.api
    )
  }

  private def submitTicket(supportFlow: SupportFlow, ticket: ApiPlatformDeskproConnector.CreateTicketRequest)(implicit hc: HeaderCarrier): Future[SupportFlow] = {
    for {
      ticketReference <- deskproConnector.createTicket(ticket, hc)
      _                = auditService.explicitAudit(CreateTicketAuditAction(ticket))
      flow            <- flowRepository.saveFlow(supportFlow.copy(referenceNumber = Some(ticketReference), emailAddress = Some(ticket.email)))
    } yield flow
  }
}
