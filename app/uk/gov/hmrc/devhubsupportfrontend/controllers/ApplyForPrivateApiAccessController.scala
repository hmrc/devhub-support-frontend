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
import play.api.mvc.{AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.HtmlFormat

import uk.gov.hmrc.devhubsupportfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.devhubsupportfrontend.connectors.ThirdPartyDeveloperConnector
import uk.gov.hmrc.devhubsupportfrontend.controllers.models.MaybeUserRequest
import uk.gov.hmrc.devhubsupportfrontend.controllers.security.SupportCookie
import uk.gov.hmrc.devhubsupportfrontend.domain.models.SupportFlow
import uk.gov.hmrc.devhubsupportfrontend.services.SupportService
import uk.gov.hmrc.devhubsupportfrontend.views.html.ApplyForPrivateApiAccessView

@Singleton
class ApplyForPrivateApiAccessController @Inject() (
    mcc: MessagesControllerComponents,
    supportService: SupportService,
    val cookieSigner: CookieSigner,
    val errorHandler: ErrorHandler,
    val thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
    applyForPrivateApiAccessView: ApplyForPrivateApiAccessView
  )(implicit val ec: ExecutionContext,
    val appConfig: AppConfig
  ) extends AbstractSupportFlowController[ApplyForPrivateApiAccessForm, Unit](mcc, supportService) with SupportCookie {

  def redirectBack(): Result = Redirect(routes.ChooseAPrivateApiController.page())

  def filterValidFlow(flow: SupportFlow): Boolean = flow match {
    case SupportFlow(_, SupportData.UsingAnApi.id, Some(SupportData.PrivateApiDocumentation.id), _, _, _, _) => true
    case _                                                                                                   => false
  }

  def form() = ApplyForPrivateApiAccessForm.form

  def extraData()(implicit request: MaybeUserRequest[AnyContent]): Future[Unit] = successful(())

  def pageContents(flow: SupportFlow, form: Form[ApplyForPrivateApiAccessForm], extras: Unit)(implicit request: MaybeUserRequest[AnyContent]): HtmlFormat.Appendable = {
    applyForPrivateApiAccessView(
      fullyloggedInDeveloper,
      flow.privateApi,
      form,
      routes.CheckCdsAccessIsRequiredController.page().url
    )
  }

  def onValidForm(flow: SupportFlow, form: ApplyForPrivateApiAccessForm)(implicit request: MaybeUserRequest[AnyContent]): Future[Result] = {
    supportService.submitTicket(flow, form).map { _ =>
      Redirect(routes.SupportDetailsController.supportConfirmationPage())
    }
  }
}
