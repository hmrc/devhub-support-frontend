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

package uk.gov.hmrc.devhubsupportfrontend.mocks.services

import scala.concurrent.Future

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.devhubsupportfrontend.domain.models.upscan.S3UploadError
import uk.gov.hmrc.devhubsupportfrontend.domain.models.upscan.UploadStatus
import uk.gov.hmrc.devhubsupportfrontend.services.FileUploadService

trait FileUploadServiceMockModule extends MockitoSugar with ArgumentMatchersSugar {

  trait AbstractFileUploadServiceMock {
    def aMock: FileUploadService

    object MarkFileAsPosted {
      def succeeds() = {
        when(aMock.markFileAsPosted(*)).thenReturn(Future.successful(()))
      }

      def fails(exception: Throwable = new RuntimeException("Mark file as posted failed")) = {
        when(aMock.markFileAsPosted(*)).thenReturn(Future.failed(exception))
      }
    }

    object MarkFileAsRejected {
      def succeeds() = {
        when(aMock.markFileAsRejected(*)).thenReturn(Future.successful(()))
      }

      def fails(exception: Throwable = new RuntimeException("Mark file as rejected failed")) = {
        when(aMock.markFileAsRejected(*)).thenReturn(Future.failed(exception))
      }
    }

    object GetFileVerificationStatus {
      def returns(status: Option[UploadStatus]) = {
        when(aMock.getFileVerificationStatus(*)).thenReturn(Future.successful(status))
      }

      def fails(exception: Throwable = new RuntimeException("Get file verification status failed")) = {
        when(aMock.getFileVerificationStatus(*)).thenReturn(Future.failed(exception))
      }
    }
  }

  object FileUploadServiceMock extends AbstractFileUploadServiceMock {
    val aMock = mock[FileUploadService]
  }
}