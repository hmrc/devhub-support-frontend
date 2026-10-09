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

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider
import uk.gov.hmrc.apiplatform.modules.tpd.test.builders.UserBuilder
import uk.gov.hmrc.apiplatform.modules.tpd.test.utils.LocalUserIdTracker

import uk.gov.hmrc.devhubsupportfrontend.config.ErrorHandler
import uk.gov.hmrc.devhubsupportfrontend.domain.models.SupportSessionId
import uk.gov.hmrc.devhubsupportfrontend.mocks.connectors.ThirdPartyDeveloperConnectorMockModule
import uk.gov.hmrc.devhubsupportfrontend.utils.WithCSRFAddToken
import uk.gov.hmrc.devhubsupportfrontend.utils.WithLoggedInSession._
import uk.gov.hmrc.devhubsupportfrontend.utils.WithSupportSession._
import uk.gov.hmrc.devhubsupportfrontend.views.html.PageNotFoundView

class PageNotFoundControllerSpec extends BaseControllerSpec with WithCSRFAddToken {

  trait Setup extends ThirdPartyDeveloperConnectorMockModule with UserBuilder with LocalUserIdTracker {
    val pageNotFoundView = app.injector.instanceOf[PageNotFoundView]

    val underTest = new PageNotFoundController(
      mcc,
      cookieSigner,
      mock[ErrorHandler],
      ThirdPartyDeveloperConnectorMock.aMock,
      pageNotFoundView
    )

    val sessionParams: Seq[(String, String)] = Seq("csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken)
    val supportSessionId                     = SupportSessionId.random
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

  trait IsPartLoggedInEnablingMFA {
    self: Setup =>

    lazy val request = FakeRequest()
      .withUser(underTest)(sessionId)
      .withSession(sessionParams: _*)

    ThirdPartyDeveloperConnectorMock.FetchSession.succeedsPartLoggedInEnablingMfa()
  }

  "PageNotFoundController" when {
    "invoke page" should {
      "succeed when session exists" in new Setup with IsLoggedIn {
        val requestWithSupportCookie = request.withSupportSession(underTest)(supportSessionId)

        val result = addToken(underTest.page())(requestWithSupportCookie)

        status(result) shouldBe OK
        contentAsString(result) should include("This page can't be found")
      }

      "succeed when session does not exist" in new Setup with NotLoggedIn {
        val result = addToken(underTest.page())(request)

        status(result) shouldBe OK
        contentAsString(result) should include("This page can't be found")
      }
    }
  }
}
