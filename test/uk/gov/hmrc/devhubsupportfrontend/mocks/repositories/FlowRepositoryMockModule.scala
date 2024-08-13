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

package uk.gov.hmrc.devhubsupportfrontend.mocks.repositories

import scala.concurrent.Future.successful

import org.mockito.quality.Strictness
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.devhubsupportfrontend.domain.models.{SupportFlow, SupportSessionId}
import uk.gov.hmrc.devhubsupportfrontend.repositories.FlowRepository

trait FlowRepositoryMockModule extends MockitoSugar with ArgumentMatchersSugar {

  protected trait BaseFlowRepositoryMock {
    def aMock: FlowRepository

    def verify = MockitoSugar.verify(aMock)

    def verifyZeroInteractions() = MockitoSugar.verifyZeroInteractions(aMock)

    object FetchBySessionId {
      def thenReturn(flow: SupportFlow) = when(aMock.fetchBySessionId(*)).thenReturn(successful(Some(flow)))

      def thenReturnNothing = when(aMock.fetchBySessionId(*)).thenReturn(successful(None))

      def verifyCalledWith(sessionId: SupportSessionId) = verify.fetchBySessionId(eqTo(sessionId))
    }

    object SaveFlow {
      def thenReturnSuccess = when(aMock.saveFlow(*)).thenAnswer((flow: SupportFlow) => successful(flow))

      def verifyCalledWith(flow: SupportFlow) = verify.saveFlow(eqTo(flow))
    }

  }

  object FlowRepositoryMock extends BaseFlowRepositoryMock {
    val aMock = mock[FlowRepository](withSettings.strictness(Strictness.LENIENT))
  }
}
