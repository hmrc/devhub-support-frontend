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

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.devhubsupportfrontend.config.AppConfig
import uk.gov.hmrc.devhubsupportfrontend.connectors.ApiPlatformDeskproConnector.CreateTicketRequest
import uk.gov.hmrc.devhubsupportfrontend.controllers.{ApplyForPrivateApiAccessForm, SupportData, SupportDetailsForm}
import uk.gov.hmrc.devhubsupportfrontend.domain.models.{SupportFlow, SupportSessionId}
import uk.gov.hmrc.devhubsupportfrontend.mocks.connectors.{ApiPlatformDeskproConnectorMockModule, ApmConnectorMockModule}
import uk.gov.hmrc.devhubsupportfrontend.mocks.repositories.SupportFlowRepositoryMockModule
import uk.gov.hmrc.devhubsupportfrontend.mocks.services.AuditServiceMockModule
import uk.gov.hmrc.devhubsupportfrontend.utils.AsyncHmrcSpec

class SupportServiceSpec extends AsyncHmrcSpec {

  val sessionId     = SupportSessionId.random
  val entryPoint    = SupportData.UsingAnApi.id
  val savedFlow     = SupportFlow(sessionId, entryPoint)
  val defaultFlow   = SupportFlow(sessionId, "unknown")
  val mockAppConfig = mock[AppConfig]
  val auditType     = "CreateTicket"

  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup extends ApmConnectorMockModule with SupportFlowRepositoryMockModule with ApiPlatformDeskproConnectorMockModule with AuditServiceMockModule {
    val underTest = new SupportService(ApmConnectorMock.aMock, ApiPlatformDeskproConnectorMock.aMock, SupportFlowRepositoryMock.aMock, mockAppConfig, AuditServiceMock.aMock)
    SupportFlowRepositoryMock.SaveFlow.thenReturnSuccess
  }

  "getSupportFlow" should {
    "default to a fixed SupportFlow if not found" in new Setup {
      SupportFlowRepositoryMock.FetchBySessionId.thenReturnNothing

      underTest.getSupportFlow(sessionId)
      SupportFlowRepositoryMock.SaveFlow.verifyCalledWith(defaultFlow)
    }

    "get stored SupportFlow if found" in new Setup {
      SupportFlowRepositoryMock.FetchBySessionId.thenReturn(savedFlow)
      val result = await(underTest.getSupportFlow(sessionId))
      result shouldBe savedFlow
      SupportFlowRepositoryMock.SaveFlow.verifyCalledWith(savedFlow)
    }
  }

  "createFlow" should {
    "save a newly created flow in the flow repository" in new Setup {
      underTest.createFlow(sessionId, entryPoint)
      SupportFlowRepositoryMock.SaveFlow.verifyCalledWith(savedFlow)
    }
  }

  "fetchAllPublicApis" should {
    "fetch all apis visible to the user when the user is NOT logged in" in new Setup {
      val apiList: List[ApiDefinition] = List(ApiDefinitionData.apiDefinition)
      ApmConnectorMock.FetchApiDefinitionsVisibleToUser.willReturn(apiList)
      val result                       = await(underTest.fetchAllPublicApis(None))
      result shouldBe apiList
      verify(ApmConnectorMock.aMock).fetchApiDefinitionsVisibleToUser(None)
    }

    "fetch all apis visible to the user when the user is logged in" in new Setup {
      val apiList: List[ApiDefinition]   = List(ApiDefinitionData.apiDefinition)
      ApmConnectorMock.FetchApiDefinitionsVisibleToUser.willReturn(apiList)
      private val loggedInUserId: UserId = UserId(UUID.randomUUID())
      val result                         = await(underTest.fetchAllPublicApis(Some(loggedInUserId)))
      result shouldBe apiList
      verify(ApmConnectorMock.aMock).fetchApiDefinitionsVisibleToUser(Some(loggedInUserId))
    }
  }

  "updateWithDelta" should {
    "update the SupportFlow with the new support session ID" in new Setup {
      val oldValue       = SupportSessionId.random
      val newValue       = SupportSessionId.random
      val entrySelection = "?"

      val result = await(underTest.updateWithDelta(f => f.copy(sessionId = newValue))(SupportFlow(oldValue, entrySelection)))

      result shouldBe SupportFlow(newValue, entrySelection)
    }
  }

  "submitTicket for Support Details" should {
    "send no API when one is NOT provided" in new Setup {
      val details  = "This is some\ndescription"
      val fullName = "test name"
      val email    = "email@test.com"
      ApiPlatformDeskproConnectorMock.CreateTicket.succeeds()
      AuditServiceMock.ExplicitAudit.succeeds()

      await(
        underTest.submitTicket(
          SupportFlow(
            SupportSessionId.random,
            SupportData.FindingAnApi.id
          ),
          SupportDetailsForm(
            details,
            fullName,
            email,
            organisation = None,
            teamMemberEmailAddress = None
          )
        )
      )

      val createTicketRequest: CreateTicketRequest = CreateTicketRequest(
        fullName = fullName,
        email = email,
        subject = "HMRC Developer Hub: Support Enquiry",
        message = details,
        supportReason = Some(SupportData.FindingAnApi.text)
      )

      ApiPlatformDeskproConnectorMock.CreateTicket.verifyCalledWith(createTicketRequest)
      AuditServiceMock.ExplicitAudit.verifyCalledWith(auditType, createTicketRequest)
    }

    "send the API when one is provided" in new Setup {
      val apiName  = "Hello world"
      val details  = "This is some\ndescription"
      val fullName = "test name"
      val email    = "email@test.com"
      ApiPlatformDeskproConnectorMock.CreateTicket.succeeds()
      AuditServiceMock.ExplicitAudit.succeeds()

      await(
        underTest.submitTicket(
          SupportFlow(
            SupportSessionId.random,
            SupportData.UsingAnApi.id,
            Some(SupportData.MakingAnApiCall.id),
            Some(apiName)
          ),
          SupportDetailsForm(
            details,
            fullName,
            email,
            organisation = None,
            teamMemberEmailAddress = None
          )
        )
      )

      val createTicketRequest = CreateTicketRequest(
        fullName = fullName,
        email = email,
        subject = "HMRC Developer Hub: Support Enquiry",
        message = details,
        apiName = Some(apiName),
        supportReason = Some(SupportData.MakingAnApiCall.text)
      )

      ApiPlatformDeskproConnectorMock.CreateTicket.verifyCalledWith(createTicketRequest)
      AuditServiceMock.ExplicitAudit.verifyCalledWith(auditType, createTicketRequest)
    }
  }

  "submitTicket for Applying for Private API Access" should {
    "send the API name, organisation and application ID" in new Setup {
      val details       = "Private API documentation access request for Application Id[12345] to my API API."
      val fullName      = "test name"
      val email         = "email@test.com"
      val organisation  = "anOrg"
      val applicationId = "12345"

      ApiPlatformDeskproConnectorMock.CreateTicket.succeeds()
      AuditServiceMock.ExplicitAudit.succeeds()

      await(
        underTest.submitTicket(
          SupportFlow(
            SupportSessionId.random,
            SupportData.UsingAnApi.id,
            Some(SupportData.PrivateApiDocumentation.id),
            privateApi = Some("my API")
          ),
          ApplyForPrivateApiAccessForm(
            fullName,
            email,
            organisation,
            applicationId
          )
        )
      )

      val createTicketRequest = CreateTicketRequest(
        fullName = fullName,
        email = email,
        subject = "HMRC Developer Hub: Support Enquiry",
        message = details,
        supportReason = Some(SupportData.PrivateApiDocumentation.text),
        organisation = Some(organisation),
        applicationId = Some(applicationId)
      )

      ApiPlatformDeskproConnectorMock.CreateTicket.verifyCalledWith(createTicketRequest)
      AuditServiceMock.ExplicitAudit.verifyCalledWith(auditType, createTicketRequest)
    }
  }
}
