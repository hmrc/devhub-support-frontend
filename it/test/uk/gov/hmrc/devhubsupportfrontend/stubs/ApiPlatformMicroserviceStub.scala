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

package uk.gov.hmrc.devhubsupportfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping

import play.api.http.Status._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId

object ApiPlatformMicroserviceStub {

  def stubFetchApiDefinitionsVisibleToUserFailure(userId: UserId): StubMapping = {
    stubFor(
      get(urlEqualTo(s"/combined-api-definitions?developerId=${userId.toString()}"))
        .willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
        )
    )
  }

  def stubFetchApiDefinitionsVisibleToUser(userId: UserId, body: String): StubMapping = {
    stubFor(
      get(urlEqualTo(s"/combined-api-definitions?developerId=${userId.toString()}"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(body)
            .withHeader("content-type", "application/json")
        )
    )
  }
}
