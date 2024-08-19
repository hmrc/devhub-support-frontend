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

  val platformFrontendHost: String = getConfigDefaulted("platform.frontend.host", "http://localhost:9695")

  val thirdPartyDeveloperUrl: String         = baseUrl("third-party-developer")
  val thirdPartyDeveloperFrontendUrl: String = baseUrl("third-party-developer-frontend")
  val apiDocumentationFrontendUrl: String    = baseUrl("api-documentation-frontend")

  lazy val keepAliveUrl: String = s"$thirdPartyDeveloperFrontendUrl/developer/keep-alive"
  lazy val logOutUrl: String    = s"$thirdPartyDeveloperFrontendUrl/developer/logout"
  lazy val logInUrl: String     = s"$thirdPartyDeveloperFrontendUrl/developer/login"

  lazy val reportProblemHost: String = config.underlying.getString("urls.report-a-problem.baseUrl") + config.underlying.getString("urls.report-a-problem.problem")

  val securedCookie: Boolean = getConfigDefaulted("cookie.secure", true)

  val sessionTimeout: Duration   = config.underlying.getDuration("sessiontimeout.timeout")
  val sessionCountdown: Duration = config.underlying.getDuration("sessiontimeout.countdown")

  val supportSessionTimeout: Duration = config.underlying.getDuration("supportsession.timeout")

  val deskproHorizonUrl: String             = config.get[String]("deskpro-horizon.uri")
  val deskproHorizonApiKey: String          = config.getOptional[String]("deskpro-horizon.api-key").map(key => s"key $key").getOrElse("")
  val deskproHorizonBrand: Int              = config.get[Int]("deskpro-horizon.brand")
  val deskproHorizonApiName: String         = config.get[String]("deskpro-horizon.api-name")
  val deskproHorizonSupportReason: String   = config.get[String]("deskpro-horizon.support-reason")
  val deskproHorizonOrganisation: String    = config.get[String]("deskpro-horizon.organisation")
  val deskproHorizonApplicationId: String   = config.get[String]("deskpro-horizon.application-id")
  val deskproHorizonTeamMemberEmail: String = config.get[String]("deskpro-horizon.team-member-email")

  private def getConfigDefaulted[A](key: String, default: => A)(implicit loader: ConfigLoader[A]): A = config.getOptional[A](key)(loader).getOrElse(default)
}
