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

package uk.gov.hmrc.devhubsupportfrontend.controllers

import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.mvc.Result
import play.api.mvc.request.RequestTarget
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider
import uk.gov.hmrc.apiplatform.modules.tpd.test.builders.UserBuilder
import uk.gov.hmrc.apiplatform.modules.tpd.test.utils.LocalUserIdTracker
import uk.gov.hmrc.devhubsupportfrontend.config.ErrorHandler
import uk.gov.hmrc.devhubsupportfrontend.domain.models.upscan.UploadStatus.formatFailed
import uk.gov.hmrc.devhubsupportfrontend.domain.models.upscan.{S3UploadError, UploadStatus}
import uk.gov.hmrc.devhubsupportfrontend.mocks.connectors.{ThirdPartyDeveloperConnectorMockModule, UpscanInitiateConnectorMockModule}
import uk.gov.hmrc.devhubsupportfrontend.mocks.services.FileUploadServiceMockModule
import uk.gov.hmrc.devhubsupportfrontend.utils.WithCSRFAddToken
import uk.gov.hmrc.devhubsupportfrontend.utils.WithLoggedInSession._

class UpscanControllerSpec extends BaseControllerSpec with WithCSRFAddToken {

  trait Setup extends UpscanInitiateConnectorMockModule with ThirdPartyDeveloperConnectorMockModule with FileUploadServiceMockModule with UserBuilder with LocalUserIdTracker {

    val underTest = new UpscanController(
      mcc,
      cookieSigner,
      mock[ErrorHandler],
      ThirdPartyDeveloperConnectorMock.aMock,
      UpscanInitiateConnectorMock.aMock,
      FileUploadServiceMock.aMock
    )

    val sessionParams: Seq[(String, String)] = Seq("csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken)

    val upscanPostTarget      = "https://upscan.example.com/upload"
    val upscanKey             = "new-upscan-upload-key"
    val upscanSuccessRedirect = "http://localhost:9685/devhub-support/upscan/success"
    val upscanErrorRedirect   = "http://localhost:9685/devhub-support/upscan/success"
  }

  trait IsLoggedIn {
    self: Setup =>

    lazy val request = FakeRequest()
      .withUser(underTest)(sessionId)
      .withSession(sessionParams: _*)

    ThirdPartyDeveloperConnectorMock.FetchSession.succeeds()
  }

  trait NotLoggedIn {
    self: Setup =>

    lazy val request = FakeRequest()
      .withSession(sessionParams: _*)

    ThirdPartyDeveloperConnectorMock.FetchSession.fails()
  }

  "initiateUpscan" when {
    "user is logged in" should {
      "return upscan initiate response as JSON" in new Setup with IsLoggedIn {
        UpscanInitiateConnectorMock.Initiate.succeedsWith(
          upscanPostTarget,
          Map(
            "key"                     -> upscanKey,
            "success_action_redirect" -> upscanSuccessRedirect,
            "error_action_redirect"   -> upscanErrorRedirect
          )
        )

        val result = addToken(underTest.initiateUpscan())(request)

        status(result) shouldBe OK
        contentType(result) shouldBe Some("application/json")

        val json = contentAsJson(result)
        (json \ "postTarget").as[String] shouldBe upscanPostTarget
        (json \ "formFields" \ "key").as[String] shouldBe upscanKey
        (json \ "formFields" \ "success_action_redirect").as[String] shouldBe upscanSuccessRedirect
        (json \ "formFields" \ "error_action_redirect").as[String] shouldBe upscanErrorRedirect
      }

      "redirect to login page if not logged in" in new Setup with NotLoggedIn {
        val result = addToken(underTest.initiateUpscan())(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/developer/login")
      }
    }
  }

  "upscanSuccessRedirect" should {
    "return HTTP 200 with empty body and iframe headers" in new Setup {
      val result = underTest.upscanSuccessRedirect()(FakeRequest())

      status(result) shouldBe OK
      contentAsString(result) shouldBe ""
      header("X-Frame-Options", result) shouldBe Some("ALLOWALL")
      header("Content-Security-Policy", result) shouldBe Some("frame-ancestors *")
    }
  }

  "markFileUploadAsPosted" when {
    "user is logged in" should {
      "return Created status when file upload is marked as posted successfully" in new Setup with IsLoggedIn {
        val uploadPostedRequest = request.withMethod("GET")
          .withTarget(RequestTarget("", "", Map("key" -> Seq("test-key"))))

        FileUploadServiceMock.MarkFileAsPosted.succeeds()

        val result = underTest.markFileUploadAsPosted()(uploadPostedRequest)

        status(result) shouldBe CREATED
        FileUploadServiceMock.MarkFileAsPosted.verifyWasCalledWith("test-key")
      }

      "return BadRequest when form binding fails" in new Setup with IsLoggedIn {
        val result = underTest.markFileUploadAsPosted()(request)

        status(result) shouldBe BAD_REQUEST
      }
    }

    "user is not logged in" should {
      "redirect to login page" in new Setup with NotLoggedIn {
        val result = underTest.markFileUploadAsPosted()(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/developer/login")
      }
    }
  }

  "markFileUploadAsRejected" when {
    "user is logged in" should {
      "return Ok status when file upload is marked as rejected successfully" in new Setup with IsLoggedIn {
        val uploadRejectedRequest = request.withMethod("GET")
          .withTarget(RequestTarget("", "",
            Map("key" -> Seq("test-key"), "errorCode" -> Seq("EntityTooLarge"), "errorMessage" -> Seq("Entity+Too+Large"))))

        FileUploadServiceMock.MarkFileAsRejected.succeeds()

        val result: Future[Result] = underTest.markFileUploadAsRejected(uploadRejectedRequest)

        status(result) shouldBe OK
        val expectedError = S3UploadError("test-key", "EntityTooLarge", "Entity+Too+Large")
        FileUploadServiceMock.MarkFileAsRejected.verifyWasCalledWith(expectedError)
      }

      "return InternalServerError when form binding fails" in new Setup with IsLoggedIn {
        val result = underTest.markFileUploadAsRejected(request)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "user is not logged in" should {
      "redirect to login page" in new Setup with NotLoggedIn {
        val result = underTest.markFileUploadAsRejected(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/developer/login")
      }
    }
  }

  "checkFileUploadStatus" when {
    "user is logged in" should {
      "return Ok with file verification status when it exists if file was posted successfully" in new Setup with IsLoggedIn {
        FileUploadServiceMock.GetFileVerificationStatus.returns(Some(UploadStatus.UploadedSuccessfully))

        val result = underTest.checkFileUploadStatus("test-reference")(request)

        status(result) shouldBe OK
        contentType(result) shouldBe Some("application/json")
        contentAsJson(result) shouldBe Json.parse(s"""{"uploadStatus":"UploadedSuccessfully"}""")
        FileUploadServiceMock.GetFileVerificationStatus.verifyWasCalledWith("test-reference")
      }

      "return Ok with file verification status when it exists, if file failed to upload" in new Setup with IsLoggedIn {
        val errorCode = "EntityTooLarge"
        val message = "Entity+Too+Large"

        FileUploadServiceMock.GetFileVerificationStatus.returns(
          Some(UploadStatus.Failed(errorCode, message)))

        val result = underTest.checkFileUploadStatus("test-reference")(request)

        status(result) shouldBe OK
        contentType(result) shouldBe Some("application/json")
        contentAsJson(result) shouldBe Json.parse(s"""{"errorCode":"$errorCode","errorMessage":"$message","uploadStatus":"Failed"}""")
        FileUploadServiceMock.GetFileVerificationStatus.verifyWasCalledWith("test-reference")
      }

      "return NotFound when file verification status does not exist" in new Setup with IsLoggedIn {
        FileUploadServiceMock.GetFileVerificationStatus.returns(None)

        val result = underTest.checkFileUploadStatus("test-reference")(request)

        status(result) shouldBe NOT_FOUND
        FileUploadServiceMock.GetFileVerificationStatus.verifyWasCalledWith("test-reference")
      }
    }

    "user is not logged in" should {
      "redirect to login page" in new Setup with NotLoggedIn {
        FileUploadServiceMock.GetFileVerificationStatus.returns(Some(UploadStatus.UploadedSuccessfully))

        val result = underTest.checkFileUploadStatus("test-reference")(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/developer/login")
      }
    }
  }
}
