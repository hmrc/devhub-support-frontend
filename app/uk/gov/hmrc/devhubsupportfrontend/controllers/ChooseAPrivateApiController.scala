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

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

import play.api.data.Form
import play.api.libs.crypto.CookieSigner
import play.api.mvc.{AnyContent, Call, MessagesControllerComponents, Result}
import play.twirl.api.HtmlFormat

import uk.gov.hmrc.devhubsupportfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.devhubsupportfrontend.connectors.ThirdPartyDeveloperConnector
import uk.gov.hmrc.devhubsupportfrontend.controllers.models.MaybeUserRequest
import uk.gov.hmrc.devhubsupportfrontend.controllers.security.SupportCookie
import uk.gov.hmrc.devhubsupportfrontend.domain.models.SupportFlow
import uk.gov.hmrc.devhubsupportfrontend.services.SupportService
import uk.gov.hmrc.devhubsupportfrontend.views.html.ChooseAPrivateApiView

@Singleton
class ChooseAPrivateApiController @Inject() (
    mcc: MessagesControllerComponents,
    val cookieSigner: CookieSigner,
    val errorHandler: ErrorHandler,
    val thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
    supportService: SupportService,
    chooseAPrivateApiView: ChooseAPrivateApiView
  )(implicit val ec: ExecutionContext,
    val appConfig: AppConfig
  ) extends AbstractSupportFlowController[ChooseAPrivateApiForm, Unit](mcc, supportService) with SupportCookie {

  def redirectBack(): Result = Redirect(routes.HelpWithUsingAnApiController.page())

  def filterValidFlow(flow: SupportFlow): Boolean = flow match {
    case SupportFlow(_, SupportData.UsingAnApi.id, Some(SupportData.PrivateApiDocumentation.id), _, _, _, _) => true
    case _                                                                                                   => false
  }

  def pageContents(flow: SupportFlow, form: Form[ChooseAPrivateApiForm], extras: Unit)(implicit request: MaybeUserRequest[AnyContent]): HtmlFormat.Appendable =
    chooseAPrivateApiView(
      fullyloggedInDeveloper,
      form,
      routes.HelpWithUsingAnApiController.page().url
    )

  def choose(choice: Option[String])(flow: SupportFlow) =
    flow.copy(privateApi = choice)

  def updateFlowAndRedirect(flowFn: SupportFlow => SupportFlow)(redirectTo: Call)(flow: SupportFlow) = {
    supportService.updateWithDelta(flowFn)(flow).map { newFlow =>
      Redirect(redirectTo)
    }
  }

  def chooseADifferentAPI(flow: SupportFlow) = choose(None)(flow)
  def chooseCDS(flow: SupportFlow)           = choose(Some(SupportData.ChooseCDS.text))(flow)

  def onValidForm(flow: SupportFlow, form: ChooseAPrivateApiForm)(implicit request: MaybeUserRequest[AnyContent]): Future[Result] =
    form.chosenApiName match {
      case c @ SupportData.ChooseADifferentAPI.id => updateFlowAndRedirect(chooseADifferentAPI)(routes.ApplyForPrivateApiAccessController.page())(flow)
      case c @ SupportData.ChooseCDS.id           => updateFlowAndRedirect(chooseCDS)(routes.CheckCdsAccessIsRequiredController.page())(flow)
      case _                                      => throw new RuntimeException("Validation failed to eliminate bad data during Form processing")
    }

  def form(): Form[ChooseAPrivateApiForm] = ChooseAPrivateApiForm.form

  def extraData()(implicit request: MaybeUserRequest[AnyContent]): Future[Unit] = successful(())
}
