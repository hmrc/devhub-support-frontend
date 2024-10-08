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

import java.time.Period

import org.apache.pekko.stream.Materializer
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.crypto.CookieSigner
import play.api.mvc.MessagesControllerComponents

import uk.gov.hmrc.devhubsupportfrontend.config.AppConfig
import uk.gov.hmrc.devhubsupportfrontend.mocks.services.ErrorHandlerMock
import uk.gov.hmrc.devhubsupportfrontend.utils.AsyncHmrcSpec

class BaseControllerSpec
    extends AsyncHmrcSpec
    with GuiceOneAppPerSuite
    with ErrorHandlerMock {

  implicit val appConfig: AppConfig = mock[AppConfig]

  implicit val cookieSigner: CookieSigner = app.injector.instanceOf[CookieSigner]

  implicit lazy val materializer: Materializer = app.materializer

  lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val grantLength: Period               = Period.ofDays(547)

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(("metrics.jvm", false))
      .build()
}
