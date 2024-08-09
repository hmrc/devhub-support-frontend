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

import javax.inject.{Inject, Singleton}

import play.api.{ConfigLoader, Configuration}
import java.time.Duration

@Singleton
class AppConfig @Inject() (config: Configuration) {
  val welshLanguageSupportEnabled: Boolean            = getConfigDefaulted("features.welsh-language-support", false)

  val title                                           = "HMRC Developer Hub"

  val platformFrontendHost: String                    = getConfigDefaulted("platform.frontend.host", "http://localhost:9695")

  val thirdPartyDeveloperUrl: String                  = baseUrl("third-party-developer")

  val thirdPartyDeveloperFrontendUrl: String          = buildUrl("platform.internal.frontend").getOrElse(baseUrl("third-party-developer-frontend"))
  val keepAliveUrl: String                            = s"$thirdPartyDeveloperFrontendUrl/developer/keep-alive"
  val logOutUrl: String                               = s"$thirdPartyDeveloperFrontendUrl/developer/logout"
  val logInUrl: String                                = s"$thirdPartyDeveloperFrontendUrl/developer/login"

  val apiDocumentationFrontendUrl: String             = buildUrl("platform.internal.frontend").getOrElse(baseUrl("api-documentation-frontend"))
  
  lazy val reportProblemHost: String                  = config.underlying.getString("urls.report-a-problem.baseUrl") + config.underlying.getString("urls.report-a-problem.problem")

  val securedCookie: Boolean                          = getConfigDefaulted("cookie.secure", true)

  val sessionTimeout: Duration                        = config.underlying.getDuration("sessiontimeout.timeout")
  val sessionCountdown: Duration                      = config.underlying.getDuration("sessiontimeout.countdown")

  val supportSessionTimeout: Duration                 = config.underlying.getDuration("supportsession.timeout")

  val deskproHorizonUrl: String                       = config.get[String]("deskpro-horizon.uri")
  val deskproHorizonApiKey: String                    = config.getOptional[String]("deskpro-horizon.api-key").map(key => s"key $key").getOrElse("")
  val deskproHorizonBrand: Int                        = config.get[Int]("deskpro-horizon.brand")
  val deskproHorizonApiName: String                   = config.get[String]("deskpro-horizon.api-name")
  val deskproHorizonSupportReason: String             = config.get[String]("deskpro-horizon.support-reason")
  val deskproHorizonOrganisation: String              = config.get[String]("deskpro-horizon.organisation")
  val deskproHorizonApplicationId: String             = config.get[String]("deskpro-horizon.application-id")
  val deskproHorizonTeamMemberEmail: String           = config.get[String]("deskpro-horizon.team-member-email")
  
  def getString(key: String) =
    config.getOptional[String](key).getOrElse(throwConfigNotFoundError(key))
    
  private def throwConfigNotFoundError(key: String) =
    throw new RuntimeException(s"Could not find config key '$key'")

  private val rootServices = "microservice.services"

  private lazy val defaultProtocol: String =
    config
      .getOptional[String](s"$rootServices.protocol")
      .getOrElse("http")

  private def buildUrl(key: String) = {
    (getConfigDefaulted(s"$key.protocol", ""), getConfigDefaulted(s"$key.host", "")) match {
      case (p, h) if !p.isEmpty && !h.isEmpty => Some(s"$p://$h")
      case (p, h) if p.isEmpty                => Some(s"https://$h")
      case _                                  => None
    }
  }
  
  private def baseUrl(serviceName: String) = {
    val protocol = config.getOptional[String](s"$serviceName.protocol").getOrElse(defaultProtocol)
    val host     = config.get[String](s"$serviceName.host")
    val port     = config.get[String](s"$serviceName.port")
    s"$protocol://$host:$port"
  }

  private def getConfigDefaulted[A](key: String, default: => A)(implicit loader: ConfigLoader[A]): A = config.getOptional[A](key)(loader).getOrElse(default)
}
