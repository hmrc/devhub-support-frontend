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

  sealed trait PrimaryChoice {
    def id: String
    def text: String
  }

  case object FindingAnApi extends PrimaryChoice {
    val id   = "finding-an-api"
    val text = "Finding an API to build my software"
  }

  case object UsingAnApi extends PrimaryChoice {
    val id   = "using-an-api"
    val text = "Using an API"
  }

  case object SigningIn extends PrimaryChoice {
    val id   = "signing-into-account"
    val text = "Signing in to my account"
  }

  case object SettingUpApplication extends PrimaryChoice {
    val id   = "setting-up-application"
    val text = "Setting up or managing a software application"
  }

  case object NoneOfTheAbove extends PrimaryChoice {
    val id   = "none-of-the-above"
    val text = "None of these"
  }

  sealed trait ApiSecondaryChoice {
    def id: String
    def text: String
  }

  case object MakingAnApiCall extends ApiSecondaryChoice {
    val id   = "making-an-api-call"
    val text = "Making an API call"
  }

  case object GettingExamples extends ApiSecondaryChoice {
    val id   = "getting-examples"
    val text = "Viewing examples of errors, payloads or schemas"
  }

  case object ReportingDocumentation extends ApiSecondaryChoice {
    val id   = "reporting-documentation"
    val text = "Reporting inaccurate or missing API information"
  }

  case object PrivateApiDocumentation extends ApiSecondaryChoice {
    val id   = "private-api-documentation"
    val text = "Accessing private API documentation"
  }

  case object ForgottenPassword extends ApiSecondaryChoice {
    val id   = "forgotten-password"
    val text = "I've forgotten my password"
  }

  case object AccessCodes extends ApiSecondaryChoice {
    val id   = "access-codes"
    val text = "I cannot receive access codes for 2FA"
  }

  case object CompletingTermsOfUseAgreement extends ApiSecondaryChoice {
    val id   = "completing-terms-of-use"
    val text = "Completing the terms of use agreement"
  }

  case object GivingTeamMemberAccess extends ApiSecondaryChoice {
    val id   = "giving-team-member-access"
    val text = "Giving a team member access to an application"
  }

  case object GeneralApplicationDetails extends ApiSecondaryChoice {
    val id   = "general-application-details"
    val text = "General application details"
  }

  case object ChooseBusinessRates {
    val id   = "business-rates"
    val text = "Business Rates 2.0"
  }

  case object ChooseCDS {
    val id   = "customs-declarations"
    val text = "Customs Declarations"
  }
}
