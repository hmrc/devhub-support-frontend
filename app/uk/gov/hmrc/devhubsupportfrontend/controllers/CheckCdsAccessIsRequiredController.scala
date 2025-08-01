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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.HtmlFormat

import uk.gov.hmrc.devhubsupportfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.devhubsupportfrontend.connectors.ThirdPartyDeveloperConnector
import uk.gov.hmrc.devhubsupportfrontend.controllers.models.MaybeUserRequest
import uk.gov.hmrc.devhubsupportfrontend.controllers.security.SupportCookie
import uk.gov.hmrc.devhubsupportfrontend.domain.models._
import uk.gov.hmrc.devhubsupportfrontend.services.SupportService
import uk.gov.hmrc.devhubsupportfrontend.views.html.{CdsAccessIsNotRequiredView, CheckCdsAccessIsRequiredView}

@Singleton
class CheckCdsAccessIsRequiredController @Inject() (
    mcc: MessagesControllerComponents,
    supportService: SupportService,
    val cookieSigner: CookieSigner,
    val errorHandler: ErrorHandler,
    val thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
    checkCdsAccessIsRequiredView: CheckCdsAccessIsRequiredView,
    cdsAccessIsNotRequiredView: CdsAccessIsNotRequiredView
  )(implicit val ec: ExecutionContext,
    val appConfig: AppConfig
  ) extends AbstractSupportFlowController[CheckCdsAccessIsRequiredForm, Unit](mcc, supportService) with SupportCookie {

  def redirectBack(): Result = Redirect(routes.ChooseAPrivateApiController.page())

  def filterValidFlow(flow: SupportFlow): Boolean = flow match {
    case SupportFlow(_, SupportData.UsingAnApi.id, Some(SupportData.PrivateApiDocumentation.id), _, Some(SupportData.ChooseCDS.text), _, _) => true
    case _                                                                                                                                  => false
  }

  def pageContents(flow: SupportFlow, form: Form[CheckCdsAccessIsRequiredForm], extras: Unit)(implicit request: MaybeUserRequest[AnyContent]): HtmlFormat.Appendable =
    checkCdsAccessIsRequiredView(
      fullyloggedInDeveloper,
      form,
      routes.ChooseAPrivateApiController.page().url
    )

  def onValidForm(flow: SupportFlow, form: CheckCdsAccessIsRequiredForm)(implicit request: MaybeUserRequest[AnyContent]): Future[Result] = {
    if (form.confirmCdsIntegration == "yes")
      successful(Redirect(routes.ApplyForPrivateApiAccessController.page()))
    else
      successful(Redirect(routes.CheckCdsAccessIsRequiredController.cdsAccessIsNotRequiredPage()))
  }

  def form() = CheckCdsAccessIsRequiredForm.form

  def extraData()(implicit request: MaybeUserRequest[AnyContent]): Future[Unit] = successful(())

  def cdsAccessIsNotRequiredPage(): Action[AnyContent] = maybeAtLeastPartLoggedInEnablingMfa { implicit request =>
    successful(Ok(
      cdsAccessIsNotRequiredView(
        fullyloggedInDeveloper,
        routes.CheckCdsAccessIsRequiredController.page().url
      )
    ))
  }
}
