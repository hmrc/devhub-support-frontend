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
import scala.concurrent.Future

import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.tpd.test.builders.UserBuilder
import uk.gov.hmrc.apiplatform.modules.tpd.test.utils.LocalUserIdTracker

import uk.gov.hmrc.devhubsupportfrontend.config.ErrorHandler
import uk.gov.hmrc.devhubsupportfrontend.domain.models.{SupportFlow, SupportSessionId}
import uk.gov.hmrc.devhubsupportfrontend.mocks.connectors.ThirdPartyDeveloperConnectorMockModule
import uk.gov.hmrc.devhubsupportfrontend.mocks.services.SupportServiceMockModule
import uk.gov.hmrc.devhubsupportfrontend.utils.WithCSRFAddToken
import uk.gov.hmrc.devhubsupportfrontend.utils.WithLoggedInSession._
import uk.gov.hmrc.devhubsupportfrontend.views.html.support.{GivingTeamMemberAccessView, HelpWithApplicationsView}

class HelpWithApplicationControllerSpec extends BaseControllerSpec with WithCSRFAddToken {

  trait Setup extends SupportServiceMockModule with ThirdPartyDeveloperConnectorMockModule with UserBuilder with LocalUserIdTracker {
    val helpWithApplicationsView    = app.injector.instanceOf[HelpWithApplicationsView]
    val givingTeamMembersAccessView = app.injector.instanceOf[GivingTeamMemberAccessView]

    lazy val request = FakeRequest()
      .withSupport(underTest, cookieSigner)(supportSessionId)
      .withUser(underTest, cookieSigner)(sessionId)

    val underTest        = new HelpWithApplicationsController(
      mcc,
      cookieSigner,
      mock[ErrorHandler],
      ThirdPartyDeveloperConnectorMock.aMock,
      SupportServiceMock.aMock,
      helpWithApplicationsView,
      givingTeamMembersAccessView
    )
    val supportSessionId = SupportSessionId.random
    val basicFlow        = SupportFlow(supportSessionId, "?")
    val appropriateFlow  = basicFlow.copy(entrySelection = SupportData.SettingUpApplication.id)

    ThirdPartyDeveloperConnectorMock.FetchSession.succeeds()

    def shouldBeRedirectedToPreviousPage(result: Future[Result]) = {
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe "/devhub-support/new-support"
    }

    def shouldBeRedirectedToNextPage(result: Future[Result]) = {
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe "/devhub-support/new-support/details"
    }

    def shouldBeRedirectedToRemoveAccessCodesPage(result: Future[Result]) = {
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe "/devhub-support/new-support/signing-in/remove-access-codes"
    }
  }

  "HelpWithApplicationsController" when {
    "invoking givingTeamMembersAccess()" should {
      "render the giving team members access page" in new Setup() {
        val result = addToken(underTest.givingTeamMembersAccess())(request)

        status(result) shouldBe OK
      }
    }

    "invoking page()" should {
      "render the HelpWithApplicationsView" in new Setup() {
        SupportServiceMock.GetSupportFlow.succeeds(appropriateFlow)

        val result = addToken(underTest.page())(request)

        status(result) shouldBe OK
      }

      "render the previous page when flow is wrong" in new Setup {
        SupportServiceMock.GetSupportFlow.succeeds(basicFlow.copy(entrySelection = "Something else"))

        val result = addToken(underTest.page())(request)

        shouldBeRedirectedToPreviousPage(result)
      }

      "render the previous page when there is no flow" in new Setup {
        SupportServiceMock.GetSupportFlow.succeeds(basicFlow)

        val result = addToken(underTest.page())(request)

        shouldBeRedirectedToPreviousPage(result)
      }
    }

    "invoke submit" should {
      "redirect to the generic support details page when Completing Terms Of Use Agreement is selected" in new Setup {
        val formRequest = request
          .withFormUrlEncodedBody("choice" -> SupportData.CompletingTermsOfUseAgreement.id)

        SupportServiceMock.GetSupportFlow.succeeds(appropriateFlow)
        SupportServiceMock.UpdateWithDelta.succeeds()

        val result = addToken(underTest.submit())(formRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/devhub-support/new-support/details")
      }

      "redirect to the giving team member access page when Giving a Team Member Access  is selected" in new Setup {
        val formRequest = request
          .withFormUrlEncodedBody("choice" -> SupportData.GivingTeamMemberAccess.id)

        SupportServiceMock.GetSupportFlow.succeeds(appropriateFlow)
        SupportServiceMock.UpdateWithDelta.succeeds()

        val result = addToken(underTest.submit())(formRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/devhub-support/new-support/app/giving-team-member-access")
      }

      "redirect to the generic support details page when General Application Details is selected" in new Setup {
        val formRequest = request
          .withFormUrlEncodedBody("choice" -> SupportData.GeneralApplicationDetails.id)

        SupportServiceMock.GetSupportFlow.succeeds(appropriateFlow)
        SupportServiceMock.UpdateWithDelta.succeeds()

        val result = addToken(underTest.submit())(formRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/devhub-support/new-support/details")
      }
    }
  }
}
