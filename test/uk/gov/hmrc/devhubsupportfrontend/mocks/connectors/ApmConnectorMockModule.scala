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

package uk.gov.hmrc.devhubsupportfrontend.mocks.connectors

import scala.concurrent.Future.{failed, successful}

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._

import uk.gov.hmrc.devhubsupportfrontend.connectors.ApmConnector

trait ApmConnectorMockModule extends MockitoSugar with ArgumentMatchersSugar {

  object ApmConnectorMock {
    val aMock = mock[ApmConnector]

    object FetchApiDefinitionsVisibleToUser {

      def willReturn(apis: List[ApiDefinition]) =
        when(aMock.fetchApiDefinitionsVisibleToUser(*)(*)).thenReturn(successful(apis))

      def willFailWith(exception: Exception) =
        when(aMock.fetchApiDefinitionsVisibleToUser(*)(*)).thenReturn(failed(exception))
    }
  }
}
