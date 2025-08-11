/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.devhubsupportfrontend.domain.models

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiDefinition, ServiceName}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiContext

import uk.gov.hmrc.devhubsupportfrontend.connectors.XmlApi

case class ApiSummary(
    serviceName: ServiceName,
    name: String,
    context: ApiContext
  )

object ApiSummary {

  def fromApiDefinition(apiDefinition: ApiDefinition) = {
    ApiSummary(
      apiDefinition.serviceName,
      apiDefinition.name,
      apiDefinition.context
    )
  }

  def fromXmlApi(xmlApi: XmlApi) = {
    ApiSummary(
      xmlApi.serviceName,
      s"${xmlApi.name} - XML",
      xmlApi.context
    )
  }

  implicit val format: OFormat[ApiSummary] = Json.format[ApiSummary]
}
