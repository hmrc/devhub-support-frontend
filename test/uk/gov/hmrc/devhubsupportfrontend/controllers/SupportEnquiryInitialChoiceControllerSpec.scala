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

package uk.gov.hmrc.devhubsupportfrontend.controllers

import scala.concurrent.ExecutionContext.Implicits.global

import org.jsoup.Jsoup

import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ServiceNameData
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiContextData
import uk.gov.hmrc.apiplatform.modules.tpd.test.utils.LocalUserIdTracker

import uk.gov.hmrc.devhubsupportfrontend.config.ErrorHandler
import uk.gov.hmrc.devhubsupportfrontend.domain.models.{ApiSummary, SupportFlow, SupportSessionId}
import uk.gov.hmrc.devhubsupportfrontend.mocks.connectors.ThirdPartyDeveloperConnectorMockModule
import uk.gov.hmrc.devhubsupportfrontend.mocks.services.SupportServiceMockModule
import uk.gov.hmrc.devhubsupportfrontend.utils.WithCSRFAddToken
import uk.gov.hmrc.devhubsupportfrontend.utils.WithLoggedInSession._
import uk.gov.hmrc.devhubsupportfrontend.views.html.SupportEnquiryInitialChoiceView

class SupportEnquiryInitialChoiceControllerSpec extends BaseControllerSpec with WithCSRFAddToken {

  trait Setup extends SupportServiceMockModule with ThirdPartyDeveloperConnectorMockModule with LocalUserIdTracker {
    val supportEnquiryInitialChoiceView = app.injector.instanceOf[SupportEnquiryInitialChoiceView]

    val supportSessionId = SupportSessionId.random
    val appropriateFlow  = SupportFlow(supportSessionId, SupportData.SigningIn.id)
    val apiName          = "Test API definition name"

    lazy val request = FakeRequest()
      .withSupport(underTest)(supportSessionId)
      .withUser(underTest)(sessionId)

    val underTest = new SupportEnquiryInitialChoiceController(
      mcc,
      cookieSigner,
      mock[ErrorHandler],
      ThirdPartyDeveloperConnectorMock.aMock,
      SupportServiceMock.aMock,
      supportEnquiryInitialChoiceView
    )

    ThirdPartyDeveloperConnectorMock.FetchSession.succeeds()

    val apiSummary = ApiSummary(
      ServiceNameData.serviceName,
      apiName,
      ApiContextData.one
    )

    SupportServiceMock.FetchAllApis.succeeds(List(apiSummary))

  }

  "SupportEnquiryController" when {
    "invoking landing page" should {
      "redirect to list of tickets for logged on user" in new Setup {
        val result = addToken(underTest.page())(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/devhub-support/tickets")
      }

      "redirect to start new ticket for not logged on user" in new Setup {
        val result = addToken(underTest.page())(FakeRequest())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/devhub-support/start")
      }
    }

    "invoking page for new support" should {
      "render the new support enquiry initial choice page when logged on" in new Setup {
        val result = addToken(underTest.startPage())(request)

        status(result) shouldBe OK
        val dom = Jsoup.parse(contentAsString(result))

        dom.getElementById(SupportData.FindingAnApi.id).attr("value") shouldEqual SupportData.FindingAnApi.id
        dom.getElementById(SupportData.UsingAnApi.id).attr("value") shouldEqual SupportData.UsingAnApi.id
        dom.getElementById(SupportData.SigningIn.id) shouldEqual null // Option not shown when logged on
        dom.getElementById(SupportData.SettingUpApplication.id).attr("value") shouldEqual SupportData.SettingUpApplication.id
      }

      "render the new support enquiry initial choice page when not logged on" in new Setup {
        val result = addToken(underTest.startPage())(FakeRequest())

        status(result) shouldBe OK
        val dom = Jsoup.parse(contentAsString(result))

        dom.getElementById(SupportData.FindingAnApi.id).attr("value") shouldEqual SupportData.FindingAnApi.id
        dom.getElementById(SupportData.UsingAnApi.id).attr("value") shouldEqual SupportData.UsingAnApi.id
        dom.getElementById(SupportData.SigningIn.id).attr("value") shouldEqual SupportData.SigningIn.id
        dom.getElementById(SupportData.SettingUpApplication.id).attr("value") shouldEqual SupportData.SettingUpApplication.id
      }
    }

    "invoking submit" should {
      "redirect to the new help with using an api page when the 'Using an API' option is selected" in new Setup {
        val formRequest = request
          .withFormUrlEncodedBody("initialChoice" -> SupportData.UsingAnApi.id)

        val result = addToken(underTest.submit())(formRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/devhub-support/api/choose-api")
      }

      "redirect to the generic support details page when any other option is selected" in new Setup {
        val formRequest = request
          .withFormUrlEncodedBody("initialChoice" -> SupportData.FindingAnApi.id)

        val result = addToken(underTest.submit())(formRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/devhub-support/details")
      }

      "submit invalid request returns BAD_REQUEST" in new Setup {
        val formRequest = request
          .withFormUrlEncodedBody(
            "xyz" -> "blah"
          )

        SupportServiceMock.GetSupportFlow.succeeds(appropriateFlow)

        val result = addToken(underTest.submit())(formRequest)

        redirectLocation(result) shouldBe None
        status(result) shouldBe BAD_REQUEST
      }

    }
  }
}
