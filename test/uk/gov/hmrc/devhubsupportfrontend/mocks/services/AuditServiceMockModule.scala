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

package uk.gov.hmrc.devhubsupportfrontend.mocks.services

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.devhubsupportfrontend.connectors.ApiPlatformDeskproConnector.CreateTicketRequest
import uk.gov.hmrc.devhubsupportfrontend.services.AuditService

trait AuditServiceMockModule extends MockitoSugar with ArgumentMatchersSugar {

  trait AbstractAuditServiceMock {
    def aMock: AuditService

    object ExplicitAudit {

      def succeeds() =
        doNothing.when(aMock).explicitAudit(*, *[CreateTicketRequest])(*)

      def verifyCalledWith(auditType: String, ticket: CreateTicketRequest) =
        verify(aMock).explicitAudit(eqTo(auditType), eqTo(ticket))(*)
    }
  }

  object AuditServiceMock extends AbstractAuditServiceMock {
    val aMock: AuditService = mock[AuditService]
  }
}
