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

object SupportData {

  abstract sealed trait SupportDataChoice {
    def id: String
    def text: String
  }

  case object GeneralIssue extends SupportDataChoice {
    val id   = "general-issue"
    val text = "General Issue"
  }

  enum PrimaryChoice(val id: String, val text: String) extends SupportDataChoice {
    case FindingAnApi         extends PrimaryChoice("finding-an-api", "Finding an API to build my software")
    case UsingAnApi           extends PrimaryChoice("using-an-api", "Using an API")
    case SigningIn            extends PrimaryChoice("signing-into-account", "Signing in to my account")
    case SettingUpApplication extends PrimaryChoice("setting-up-application", "Setting up or managing a software application")
    case NoneOfTheAbove       extends PrimaryChoice("none-of-the-above", "None of these")
  }

  export PrimaryChoice.*

  enum ApiSecondaryChoice(val id: String, val text: String) extends SupportDataChoice {
    case MakingAnApiCall               extends ApiSecondaryChoice("making-an-api-call", "Making an API call")
    case GettingExamples               extends ApiSecondaryChoice("getting-examples", "Viewing examples of errors, payloads or schemas")
    case ReportingDocumentation        extends ApiSecondaryChoice("reporting-documentation", "Reporting inaccurate or missing API information")
    case PrivateApiDocumentation       extends ApiSecondaryChoice("private-api-documentation", "Accessing private API documentation")
    case ForgottenPassword             extends ApiSecondaryChoice("forgotten-password", "I've forgotten my password")
    case AccessCodes                   extends ApiSecondaryChoice("access-codes", "I cannot receive access codes for 2FA")
    case CompletingTermsOfUseAgreement extends ApiSecondaryChoice("completing-terms-of-use", "Completing the terms of use agreement")
    case GivingTeamMemberAccess        extends ApiSecondaryChoice("giving-team-member-access", "Giving a team member access to a software application")
  }

  export ApiSecondaryChoice.*

  case object ChooseADifferentAPI {
    val id   = "different-api"
    val text = "A different API"
  }

  case object ChooseCDS {
    val id   = "customs-declarations"
    val text = "Customs Declarations Service (CDS)"
  }
}
