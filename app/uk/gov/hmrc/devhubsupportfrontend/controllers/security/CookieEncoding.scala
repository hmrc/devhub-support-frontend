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

package uk.gov.hmrc.devhubsupportfrontend.security

import java.security.MessageDigest

import play.api.libs.crypto.CookieSigner
import play.api.mvc.{Cookie, RequestHeader}
import uk.gov.hmrc.apiplatform.modules.tpd.session.domain.models.UserSessionId

import uk.gov.hmrc.devhubsupportfrontend.config.AppConfig
import uk.gov.hmrc.devhubsupportfrontend.domain.models.SupportSessionId

trait CookieEncoding {
  implicit val appConfig: AppConfig

  private[security] lazy val cookieSecureOption: Boolean        = appConfig.securedCookie
  private[security] lazy val cookieHttpOnlyOption: Boolean      = true
  private[security] lazy val cookieDomainOption: Option[String] = None
  private[security] lazy val cookiePathOption: String           = "/"
  private[security] lazy val cookieName                         = "SUPPORT_SESS_ID"
  private[security] lazy val cookieMaxAge                       = Some(3600) // Hardcoded to 1 Hour

  val cookieSigner: CookieSigner

  def createUserCookie(sessionId: UserSessionId): Cookie = {
    Cookie(
      cookieName,
      encodeCookie(sessionId.toString()),
      cookieMaxAge,
      cookiePathOption,
      cookieDomainOption,
      cookieSecureOption,
      cookieHttpOnlyOption
    )
  }

  def createSupportCookie(sessionId: SupportSessionId): Cookie = {
    Cookie(
      cookieName,
      encodeCookie(sessionId.toString()),
      cookieMaxAge,
      cookiePathOption,
      cookieDomainOption,
      cookieSecureOption,
      cookieHttpOnlyOption
    )
  }

  def encodeCookie(token: String): String = {
    cookieSigner.sign(token) + token
  }

  def extractSupportSessionIdFromCookie(request: RequestHeader): Option[SupportSessionId] = decodeCookie(request, cookieName).flatMap(SupportSessionId.apply)

  def extractUserSessionIdFromCookie(request: RequestHeader): Option[UserSessionId] = decodeCookie(request, cookieName).flatMap(UserSessionId.apply)

  private def decodeCookie(request: RequestHeader, theCookieName: String): Option[String] = {
    for {
      cookie       <- request.cookies.get(theCookieName)
      decodedValue <- decodeCookieValue(cookie.value)
    } yield decodedValue
  }

  private def decodeCookieValue(token: String): Option[String] = {
    val (hmac, value) = token.splitAt(40)

    val signedValue = cookieSigner.sign(value)

    if (MessageDigest.isEqual(signedValue.getBytes, hmac.getBytes)) {
      Some(value)
    } else {
      None
    }
  }
}