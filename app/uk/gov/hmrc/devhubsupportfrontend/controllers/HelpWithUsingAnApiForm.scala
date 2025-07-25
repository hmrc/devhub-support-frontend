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

import uk.gov.voa.play.form.ConditionalMappings._

import play.api.data.Form
import play.api.data.Forms._

import uk.gov.hmrc.devhubsupportfrontend.controllers.SupportData.{GettingExamples, MakingAnApiCall, NoneOfTheAbove, PrivateApiDocumentation, ReportingDocumentation}

final case class HelpWithUsingAnApiForm(choice: String, apiNameForCall: Option[String], apiNameForExamples: Option[String], apiNameForReporting: Option[String])

object HelpWithUsingAnApiForm extends FormValidation {

  val form: Form[HelpWithUsingAnApiForm] = Form(
    mapping(
      "choice"                                -> oneOf(MakingAnApiCall.id, GettingExamples.id, ReportingDocumentation.id, PrivateApiDocumentation.id, NoneOfTheAbove.id),
      MakingAnApiCall.id + "-api-name"        -> mandatoryIf(isEqual("choice", MakingAnApiCall.id), nonEmptyText),
      GettingExamples.id + "-api-name"        -> mandatoryIf(isEqual("choice", GettingExamples.id), nonEmptyText),
      ReportingDocumentation.id + "-api-name" -> mandatoryIf(isEqual("choice", ReportingDocumentation.id), nonEmptyText)
    )(HelpWithUsingAnApiForm.apply)(HelpWithUsingAnApiForm.unapply)
  )
}
