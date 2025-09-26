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

package uk.gov.hmrc.devhubsupportfrontend.config

import java.time.Duration
import javax.inject.{Inject, Singleton}

import play.api.{ConfigLoader, Configuration}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject() (config: Configuration) extends ServicesConfig(config) {

  val welshLanguageSupportEnabled: Boolean = getConfigDefaulted("features.welsh-language-support", false)

  val serviceName = "HMRC Developer Hub"

  val feedbackSurveyUrl = getString("feedbackBanner.generic.surveyUrl")

  val thirdPartyDeveloperUrl: String = baseUrl("third-party-developer")

  val xmlServicesUrl: String = baseUrl("api-platform-xml-services")

  private val platformFrontendHost: Option[String] = config.getOptional[String]("platform.frontend.host")
  lazy val accessibilityStatementUrl               = platformFrontendHost.getOrElse("http://localhost:12346")

  private val internalPlatformHost: Option[String] = config.getOptional[String]("internal.platform.host")
  lazy val apiDocumentationFrontendUrl: String     = internalPlatformHost.getOrElse("http://localhost:9680")
  lazy val thirdPartyDeveloperFrontendUrl: String  = internalPlatformHost.getOrElse("http://localhost:9685")
  lazy val reportProblemHost: String               = internalPlatformHost.getOrElse("http://localhost:9250")

  lazy val keepAliveUrl: String = s"$thirdPartyDeveloperFrontendUrl/developer/keep-alive"
  lazy val logOutUrl: String    = s"$thirdPartyDeveloperFrontendUrl/developer/logout"
  lazy val logInUrl: String     = s"$thirdPartyDeveloperFrontendUrl/developer/login"

  val securedCookie: Boolean = getConfigDefaulted("cookie.secure", true)

  val sessionTimeout: Duration   = config.underlying.getDuration("session.timeout")
  val sessionCountdown: Duration = config.underlying.getDuration("session.countdown")

  val supportSessionTimeout: Duration = config.underlying.getDuration("supportsession.timeout")

  val mustBeLoggedIn: Boolean = getConfigDefaulted("toggle.must-be-logged-in", false)

  private def getConfigDefaulted[A](key: String, default: => A)(implicit loader: ConfigLoader[A]): A = config.getOptional[A](key)(loader).getOrElse(default)
}
