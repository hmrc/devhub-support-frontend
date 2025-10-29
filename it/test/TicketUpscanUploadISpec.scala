/*
 * Copyright 2025 HM Revenue & Customs
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

import java.nio.file.{Files, Path}

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{ChromeFactory, OneBrowserPerSuite}
import org.scalatestplus.selenium.WebBrowser
import support.WireMockSupport

import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.tpd.test.builders.UserBuilder
import uk.gov.hmrc.apiplatform.modules.tpd.test.data.SampleUserSession
import uk.gov.hmrc.apiplatform.modules.tpd.test.utils.LocalUserIdTracker

import uk.gov.hmrc.devhubsupportfrontend.connectors.{ThirdPartyDeveloperConnector, UpscanInitiateConnector}
import uk.gov.hmrc.devhubsupportfrontend.controllers.security.CookieEncoding
import uk.gov.hmrc.devhubsupportfrontend.domain.models._
import uk.gov.hmrc.devhubsupportfrontend.domain.models.upscan.services._
import uk.gov.hmrc.devhubsupportfrontend.mocks.connectors.{ThirdPartyDeveloperConnectorMockModule, UpscanInitiateConnectorMockModule}
import uk.gov.hmrc.devhubsupportfrontend.mocks.services.TicketServiceMockModule
import uk.gov.hmrc.devhubsupportfrontend.services.TicketService

class TicketUpscanUploadISpec
    extends AnyWordSpec
    with Matchers
    with GuiceOneServerPerSuite
    with OneBrowserPerSuite
    with ChromeFactory
    with WebBrowser
    with WireMockSupport
    with SampleUserSession
    with UserBuilder
    with LocalUserIdTracker
    with ThirdPartyDeveloperConnectorMockModule
    with UpscanInitiateConnectorMockModule
    with TicketServiceMockModule
    with Eventually
    with FixedClock {

  private val attachments = List(
    DeskproAttachment("readme.txt", "https://example/file/readme.txt"),
    DeskproAttachment("error.png", "https://example/file/error.png")
  )

  private val messages = List(
    DeskproMessage(
      id = 1,
      ticketId = 123,
      person = 100,
      dateCreated = instant,
      isAgentNote = false,
      message = "Initial description",
      attachments = attachments
    ),
    DeskproMessage(
      id = 2,
      ticketId = 123,
      person = 200,
      dateCreated = instant.plusSeconds(3600),
      isAgentNote = false,
      message = "We are looking into it",
      attachments = Nil
    )
  )

  private val ticket = DeskproTicket(
    id = 123,
    subject = "Cannot upload file",
    ref = "SDST-12345",
    person = 100,
    personEmail = LaxEmailAddress("user@example.com"),
    status = "open",
    dateCreated = instant,
    dateLastUpdated = instant.plusSeconds(3600),
    dateResolved = None,
    messages = messages
  )

  private val upscan = UpscanInitiateResponse(
    fileReference = UpscanFileReference("ref-123"),
    postTarget = "https://dummy-upscan-target.example/",
    formFields = Map(
      "key"             -> "abc",
      "policy"          -> "XYZ",
      "x-amz-algorithm" -> "AWS4-HMAC-SHA256"
    )
  )

  override def fakeApplication(): Application = {
    ThirdPartyDeveloperConnectorMock.FetchSession.succeeds()
    TicketServiceMock.FetchTicket.succeeds(Some(ticket.copy(personEmail = userSession.developer.email)))
    TicketServiceMock.GetTicketsForUser.succeeds(List(ticket.copy(personEmail = userSession.developer.email)))
    TicketServiceMock.CreateResponse.succeeds()

    new GuiceApplicationBuilder()
      .configure(
        "cookie.secure"                      -> false,
        "play.http.router"                   -> "prod.Routes",
        "third-party-developer-frontend.url" -> "http://localhost:12345"
      )
      .overrides(
        bind[TicketService].toInstance(TicketServiceMock.aMock),
        bind[UpscanInitiateConnector].toInstance(UpscanInitiateConnectorMock.aMock),
        bind[ThirdPartyDeveloperConnector].toInstance(ThirdPartyDeveloperConnectorMock.aMock)
      )
      .build()
  }

  def cookieEncoding: CookieEncoding = {
    val cs                                                              = app.injector.instanceOf[play.api.libs.crypto.CookieSigner]
    implicit val ac: uk.gov.hmrc.devhubsupportfrontend.config.AppConfig = app.injector.instanceOf[uk.gov.hmrc.devhubsupportfrontend.config.AppConfig]
    new uk.gov.hmrc.devhubsupportfrontend.controllers.security.CookieEncoding {
      override val cookieSigner       = cs
      override implicit val appConfig = ac
    }
  }

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(150, Millis)
  )

  private def baseUrl = s"http://localhost:$port/devhub-support"

  "Upscan upload flow" should {
    "POST the multipart form to WireMock and return with ?key, populating fileReferences" in withWireMock {
      val ticketId = ticket.id

      // Given: the upscan postTarget is pointed to WireMock
      val upscanKey = "ref-123"
      val postPath  = "/upscan"
      val postUrl   = s"http://localhost:${wireMockServer.port()}$postPath"
      UpscanInitiateConnectorMock.Initiate.succeedsWith(upscanKey, postUrl, upscan.formFields)

      // Given: WireMock is stubbed to accept multipart and redirect back with ?key
      stubFor(post(urlEqualTo(postPath))
        .willReturn(
          aResponse()
            .withStatus(303)
            .withHeader("Location", s"http://localhost:$port/devhub-support/ticket/$ticketId/withAttachments?key=$upscanKey")
        ))

      // Given: a user visits the domain with a signed user cookie
      go to s"$baseUrl/tickets"
      webDriver.manage().addCookie(new org.openqa.selenium.Cookie(
        "PLAY2AUTH_SESS_ID",
        cookieEncoding.encodeCookie(sessionId.value.toString)
      ))

      // When: the user goes to the ticket with attachments page
      go to s"$baseUrl/ticket/$ticketId/withAttachments"

      eventually { find(cssSelector("form[enctype='multipart/form-data']")) must not be empty }

      // When: the user provides a real file path for the input (required by Selenium)
      val tmpFile: Path = Files.createTempFile("upload-test-", ".txt")
      Files.write(tmpFile, "hello from test".getBytes("UTF-8"))

      val fileInput = find(IdQuery("file-input")).get
      fileInput.underlying.sendKeys(tmpFile.toAbsolutePath.toString)

      // When: the user clicks the Upload button -> WireMock 303 redirects back with ?key
      clickOn(IdQuery("submit"))

      eventually {
        currentUrl must include(s"/ticket/$ticketId/withAttachments")
        currentUrl must include(s"key=$upscanKey")
      }

      // Then: the hidden fileReferences field should be populated from the query param
      val fileRefHidden = find(xpath("//input[@type='hidden' and @name='fileReferences']")).get
      fileRefHidden.attribute("value") mustBe Some(upscanKey)

      // When: the user submits a message to complete the flow
      clickOn(NameQuery("response"))
      enter("Here is my response after upload.")
      clickOn(IdQuery("continue"))

      eventually { currentUrl must endWith("/devhub-support/tickets") }

      // Then: the correct fileReferences was sent to the TicketService
      TicketServiceMock.CreateResponse.verifyCalledWith(ticketId, "Here is my response after upload.", "open", "awaiting_agent", List(upscanKey))

      // Then: WireMock should have seen a single POST
      com.github.tomakehurst.wiremock.client.WireMock.verify(1, postRequestedFor(urlEqualTo(postPath)))
    }
  }
}
