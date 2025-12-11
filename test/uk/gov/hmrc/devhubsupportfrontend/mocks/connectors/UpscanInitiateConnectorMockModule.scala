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

package uk.gov.hmrc.devhubsupportfrontend.mocks.connectors

import scala.concurrent.Future

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.devhubsupportfrontend.connectors.UpscanInitiateConnector
import uk.gov.hmrc.devhubsupportfrontend.domain.models.upscan.services.{UpscanFileReference, UpscanInitiateResponse}

trait UpscanInitiateConnectorMockModule extends MockitoSugar with ArgumentMatchersSugar {

  trait AbstractUpscanInitiateConnectorMock {
    def aMock: UpscanInitiateConnector

    object Initiate {

      def succeeds(
          response: UpscanInitiateResponse = UpscanInitiateResponse(
            UpscanFileReference("test-reference"),
            "https://upscan.example.com/upload",
            Map("key" -> "value")
          )
        ) = {
        when(aMock.initiate()(*)).thenReturn(Future.successful(response))
      }

      def succeedsWith(postTarget: String, formFields: Map[String, String]) = {
        when(aMock.initiate()(*)).thenReturn(Future.successful(
          UpscanInitiateResponse(
            UpscanFileReference("fileReference"),
            postTarget,
            formFields
          )
        ))
      }

      def fails(exception: Throwable = new RuntimeException("Upscan initiate failed")) = {
        when(aMock.initiate()(*)).thenReturn(Future.failed(exception))
      }
    }
  }

  object UpscanInitiateConnectorMock extends AbstractUpscanInitiateConnectorMock {
    val aMock = mock[UpscanInitiateConnector]
  }
}
