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

package uk.gov.hmrc.devhubsupportfrontend

package object controllers {
  case class FieldNameKey(value: String)     extends AnyVal { override def toString(): String = value }
  case class GlobalMessageKey(value: String) extends AnyVal { override def toString(): String = value }
  case class FieldMessageKey(value: String)  extends AnyVal { override def toString(): String = value }

  object Conversions {
    implicit def fromFieldNameKeyToString(in: FieldNameKey): String         = in.value
    implicit def fromGlobalMessageKeyToString(in: GlobalMessageKey): String = in.value
    implicit def fromFieldMessageKeyToString(in: FieldMessageKey): String   = in.value
  }

  object FormKeys {
    val firstnameField               = FieldNameKey("firstname")
    val lastnameField                = FieldNameKey("lastname")
    val fullnameField                = FieldNameKey("fullname")
    val emailaddressField            = FieldNameKey("emailaddress")
    val passwordField                = FieldNameKey("password")
    val loginPasswordField           = FieldNameKey("loginpassword")
    val currentPasswordField         = FieldNameKey("currentpassword")
    val confirmapasswordField        = FieldNameKey("confirmpassword")
    val appNameField                 = FieldNameKey("applicationName")
    val appDescriptionField          = FieldNameKey("description")
    val deleteSelectField            = FieldNameKey("deleteSelect")
    val selectedApisNonSelectedField = FieldNameKey("errorSelectedApisNonselectedField")

    val firstnameRequiredKey                = FieldMessageKey("firstname.error.required.field")
    val firstnameMaxLengthKey               = FieldMessageKey("firstname.error.maxLength.field")
    val lastnameRequiredKey                 = FieldMessageKey("lastname.error.required.field")
    val lastnameMaxLengthKey                = FieldMessageKey("lastname.error.maxLength.field")
    val fullnameRequiredKey                 = FieldMessageKey("fullname.error.required.field")
    val fullnameMaxLengthKey                = FieldMessageKey("fullname.error.maxLength.field")
    val commentsRequiredKey                 = FieldMessageKey("comments.error.required.field")
    val commentsMaxLengthKey                = FieldMessageKey("comments.error.maxLength.field")
    val commentsSpamKey                     = FieldMessageKey("comments.error.spam.field")
    val ipAllowlistAddAnotherNoChoiceKey    = FieldMessageKey("ipAllowlist.addAnother.confirmation.no.choice.field")
    val ipAllowlistInvalidCidrBlockKey      = FieldMessageKey("ipAllowlist.cidrBlock.invalid")
    val ipAllowlistPrivateCidrBlockKey      = FieldMessageKey("ipAllowlist.cidrBlock.invalid.private")
    val ipAllowlistInvalidCidrBlockRangeKey = FieldMessageKey("ipAllowlist.cidrBlock.invalid.range")
    val telephoneRequiredKey                = FieldMessageKey("telephone.error.required.field")
    val emailaddressRequiredKey             = FieldMessageKey("emailaddress.error.required.field")
    val emailaddressNotValidKey             = FieldMessageKey("emailaddress.error.not.valid.field")
    val emailMaxLengthKey                   = FieldMessageKey("emailaddress.error.maxLength.field")
    val detailsRequiredKey                  = FieldMessageKey("details.error.required.field")
    val detailsMaxLengthKey                 = FieldMessageKey("details.error.maxLength.field")

    val termsOfUseAgreeKey       = FieldMessageKey("termsofuse.error.required.field")
    val termsOfUseAgreeGlobalKey = GlobalMessageKey("termsofuse.error.required.global")

    val passwordNotValidKey = FieldMessageKey("password.error.not.valid.field")
    val passwordRequiredKey = FieldMessageKey("password.error.required.field")
    val passwordNoMatchKey  = FieldMessageKey("password.error.no.match.field")

    val loginPasswordRequiredKey = FieldMessageKey("loginpassword.error.required.field")

    val emailalreadyInUseKey  = FieldMessageKey("emailaddress.already.registered.field")
    val emailalreadyInUse2Key = FieldMessageKey("emailaddress.already.registered.2.field")

    val accountUnverifiedKey        = FieldMessageKey("account.unverified.field")
    val invalidCredentialsKey       = FieldMessageKey("invalid.credentials.field")
    val invalidPasswordKey          = FieldMessageKey("invalid.password.field")
    val accountLockedKey            = FieldMessageKey("account.locked.field")
    val accountLocked2Key           = FieldMessageKey("account.locked.2.field")
    val currentPasswordRequiredKey  = FieldMessageKey("currentpassword.error.required.field")
    val currentPasswordInvalidKey   = FieldMessageKey("currentpassword.invalid.field")
    val redirectUriInvalidKey       = FieldMessageKey("redirect.uri.invalid.field")
    val privacyPolicyUrlRequiredKey = FieldMessageKey("privacy.policy.url.required.field")
    val privacyPolicyUrlInvalidKey  = FieldMessageKey("privacy.policy.url.invalid.field")
    val privacyPolicyUrlNoChoiceKey = FieldMessageKey("privacy.policy.url.no.choice.field")
    val tNcUrlInvalidKey            = FieldMessageKey("terms.conditions.url.invalid.field")
    val tNcUrlNoChoiceKey           = FieldMessageKey("terms.conditions.url.no.choice.field")
    val tNcUrlRequiredKey           = FieldMessageKey("terms.conditions.url.required.field")

    val applicationNameInvalidKeyLengthAndCharacters = "application.name.invalid.length.and.characters"

    val applicationNameInvalidKey       = FieldMessageKey("application.name.invalid.name")
    val applicationNameAlreadyExistsKey = FieldMessageKey("application.name.already.exists.field")

    val environmentInvalidKey = FieldMessageKey("environment.error.required.field")

    val teamMemberEmailRequired = FieldMessageKey("team.member.error.emailAddress.required.field")
    val teamMemberAlreadyExists = FieldMessageKey("team.member.error.emailAddress.already.exists.field")

    val teamMemberRoleRequired             = FieldMessageKey("roles.error.answer.required.field.content")
    val removeTeamMemberConfirmNoChoiceKey = FieldMessageKey("remove.team.member.confirmation.no.choice.field")

    val firstnameRequiredGlobalKey        = GlobalMessageKey("firstname.error.required.global")
    val firstnameMaxLengthGlobalKey       = GlobalMessageKey("firstname.error.maxLength.global")
    val lastnameRequiredGlobalKey         = GlobalMessageKey("lastname.error.required.global")
    val lastnameMaxLengthGlobalKey        = GlobalMessageKey("lastname.error.maxLength.global")
    val emailaddressRequiredGlobalKey     = GlobalMessageKey("emailaddress.error.required.global")
    val emailaddressNotValidGlobalKey     = GlobalMessageKey("emailaddress.error.not.valid.global")
    val emailMaxLengthGlobalKey           = GlobalMessageKey("emailaddress.error.maxLength.global")
    val passwordNotValidGlobalKey         = GlobalMessageKey("password.error.not.valid.global")
    val passwordRequiredGlobalKey         = GlobalMessageKey("password.error.required.global")
    val passwordNoMatchGlobalKey          = GlobalMessageKey("password.error.no.match.global")
    val emailaddressAlreadyInUseGlobalKey = GlobalMessageKey("emailaddress.already.registered.global")

    val accountUnverifiedGlobalKey             = GlobalMessageKey("account.unverified.global")
    val accountLockedGlobalKey                 = GlobalMessageKey("account.locked.global")
    val invalidCredentialsGlobalKey            = GlobalMessageKey("invalid.credentials.global")
    val invalidPasswordGlobalKey               = GlobalMessageKey("invalid.password.global")
    val currentPasswordRequiredGlobalKey       = GlobalMessageKey("currentpassword.error.required.global")
    val currentPasswordInvalidGlobalKey        = GlobalMessageKey("currentpassword.invalid.global")
    val redirectUriInvalidGlobalKey            = GlobalMessageKey("redirect.uri.invalid.global")
    val privacyPolicyUrlInvalidGlobalKey       = GlobalMessageKey("privacy.policy.url.invalid.global")
    val tNcUrlInvalidGlobalKey                 = GlobalMessageKey("terms.conditions.url.invalid.global")
    val clientSecretLimitExceeded              = GlobalMessageKey("client.secret.limit.exceeded")
    val productionCannotDeleteOnlyClientSecret = GlobalMessageKey("production.cannot.delete.only.client.secret")
    val sandboxCannotDeleteOnlyClientSecret    = GlobalMessageKey("sandbox.cannot.delete.only.client.secret")

    val deleteApplicationConfirmNoChoiceKey   = FieldMessageKey("delete.application.confirmation.no.choice.field")
    val deleteClientSecretsConfirmNoChoiceKey = FieldMessageKey("delete.client.secrets.confirmation.no.choice.field")
    val subscriptionConfirmationNoChoiceKey   = FieldMessageKey("subscription.confirmation.no.choice.field")
    val unsubscribeConfirmationNoChoiceKey    = FieldMessageKey("unsubscribe.confirmation.no.choice.field")
    val changeSubscriptionNoChoiceKey         = FieldMessageKey("subscription.change.no.choice.field")
    val accountDeleteConfirmationRequiredKey  = FieldMessageKey("developer.delete.error.required.field")
    val remove2SVConfirmNoChoiceKey           = FieldMessageKey("remove.2SV.confirmation.no.choice.field")

    val deleteRedirectConfirmationNoChoiceKey = FieldMessageKey("delete.redirect.confirmation.no.choice.field")

    val sellResellOrDistributeConfirmNoChoiceKey = FieldMessageKey("sell.resell.distribute.confirmation.no.choice.field")

    val verifyPasswordInvalidKey       = FieldMessageKey("verify.password.error.required.field")
    val verifyPasswordInvalidGlobalKey = GlobalMessageKey("verify.password.error.required.global")

    val selectAClientSecretKey      = FieldMessageKey("select.client.secret.field")
    val selectFewerClientSecretsKey = FieldMessageKey("select.fewer.client.secrets.field")

    val accessCodeInvalidKey       = FieldMessageKey("accessCode.invalid.number.field")
    val accessCodeInvalidGlobalKey = GlobalMessageKey("accessCode.invalid.number.global")

    val accessCodeErrorKey       = FieldMessageKey("accessCode.error.field")
    val accessCodeErrorGlobalKey = GlobalMessageKey("accessCode.error.global")

    val selectMfaInvalidKey       = FieldMessageKey("selectMfa.invalid.mfaType.field")
    val selectMfaInvalidGlobalKey = GlobalMessageKey("selectMfa.invalid.mfaType.global")

    val mfaNameChangeInvalidKey       = FieldMessageKey("mfaName.invalid.name.field")
    val mfaNameChangeInvalidGlobalKey = GlobalMessageKey("mfaName.invalid.name.global")

    val mobileNumberInvalidKey       = FieldMessageKey("mobileNumber.invalid.number.field")
    val mobileNumberInvalidGlobalKey = GlobalMessageKey("mobileNumber.invalid.number.global")

    val mobileNumberTooShortKey       = FieldMessageKey("mobileNumber.too.short.number.field")
    val mobileNumberTooShortGlobalKey = GlobalMessageKey("mobileNumber.too.short.number.global")

    val selectedCategoryNonSelectedKey       = FieldMessageKey("error.selectedcategories.nonselected.field")
    val selectedCategoryNonSelectedGlobalKey = GlobalMessageKey("error.selectedcategories.nonselected.global")

    val selectedApiRadioKey       = FieldMessageKey("error.select.apiradio.nonselected.field")
    val selectedApiRadioGlobalKey = GlobalMessageKey("error.select.apiradio.nonselected.global")

    val selectedApisNonSelectedKey       = FieldMessageKey("error.selectedapis.nonselected.field")
    val selectedApisNonSelectedGlobalKey = GlobalMessageKey("error.selectedapis.nonselected.global")

    val selectedTopicsNonSelectedKey       = FieldMessageKey("error.selectedtopics.nonselected.field")
    val selectedTopicsNonSelectedGlobalKey = GlobalMessageKey("error.selectedtopics.nonselected.global")

    val responsibleIndividualFullnameRequiredKey     = FieldMessageKey("responsible_individual_fullname.error.required.field")
    val responsibleIndividualEmailAddressRequiredKey = FieldMessageKey("responsible_individual_emailaddress.error.required.field")

    val noApplicationsChoiceRequiredKey = FieldMessageKey("no.applications.choice.error.required.field")

    val formKeysMap: Map[FieldMessageKey, GlobalMessageKey] = Map(
      firstnameRequiredKey           -> firstnameRequiredGlobalKey,
      firstnameMaxLengthKey          -> firstnameMaxLengthGlobalKey,
      lastnameRequiredKey            -> lastnameRequiredGlobalKey,
      lastnameMaxLengthKey           -> lastnameMaxLengthGlobalKey,
      emailaddressRequiredKey        -> emailaddressRequiredGlobalKey,
      emailaddressNotValidKey        -> emailaddressNotValidGlobalKey,
      emailMaxLengthKey              -> emailMaxLengthGlobalKey,
      emailalreadyInUseKey           -> emailaddressAlreadyInUseGlobalKey,
      passwordNotValidKey            -> passwordNotValidGlobalKey,
      passwordRequiredKey            -> passwordRequiredGlobalKey,
      passwordNoMatchKey             -> passwordNoMatchGlobalKey,
      accountUnverifiedKey           -> accountUnverifiedGlobalKey,
      invalidCredentialsKey          -> invalidCredentialsGlobalKey,
      invalidPasswordKey             -> invalidPasswordGlobalKey,
      accountLockedKey               -> accountLockedGlobalKey,
      currentPasswordRequiredKey     -> currentPasswordRequiredGlobalKey,
      currentPasswordInvalidKey      -> currentPasswordInvalidGlobalKey,
      redirectUriInvalidKey          -> redirectUriInvalidGlobalKey,
      privacyPolicyUrlInvalidKey     -> privacyPolicyUrlInvalidGlobalKey,
      tNcUrlInvalidKey               -> tNcUrlInvalidGlobalKey,
      termsOfUseAgreeKey             -> termsOfUseAgreeGlobalKey,
      accessCodeInvalidKey           -> accessCodeInvalidGlobalKey,
      accessCodeErrorKey             -> accessCodeErrorGlobalKey,
      selectedCategoryNonSelectedKey -> selectedCategoryNonSelectedGlobalKey,
      selectedApisNonSelectedKey     -> selectedApisNonSelectedGlobalKey,
      selectedApiRadioKey            -> selectedApiRadioGlobalKey,
      selectedTopicsNonSelectedKey   -> selectedTopicsNonSelectedGlobalKey,
      mobileNumberInvalidKey         -> mobileNumberInvalidGlobalKey,
      mobileNumberTooShortKey        -> mobileNumberTooShortGlobalKey
    )

    def findFieldKeys(rawMessage: String): Option[(FieldMessageKey, GlobalMessageKey)] = {
      formKeysMap.find(_._1.value == rawMessage)
    }

    val globalKeys: Seq[GlobalMessageKey] = formKeysMap.values.toSeq

    val globalToField: Map[GlobalMessageKey, FieldNameKey] = Map(
      firstnameRequiredGlobalKey        -> firstnameField,
      firstnameMaxLengthGlobalKey       -> firstnameField,
      lastnameRequiredGlobalKey         -> lastnameField,
      lastnameMaxLengthGlobalKey        -> lastnameField,
      emailaddressRequiredGlobalKey     -> emailaddressField,
      emailaddressNotValidGlobalKey     -> emailaddressField,
      emailaddressAlreadyInUseGlobalKey -> emailaddressField,
      passwordNotValidGlobalKey         -> passwordField,
      passwordRequiredGlobalKey         -> passwordField,
      passwordNoMatchGlobalKey          -> passwordField,
      accountLockedGlobalKey            -> currentPasswordField,
      emailaddressAlreadyInUseGlobalKey -> emailaddressField,
      accountUnverifiedGlobalKey        -> emailaddressField,
      invalidCredentialsGlobalKey       -> emailaddressField,
      invalidPasswordGlobalKey          -> passwordField,
      currentPasswordRequiredGlobalKey  -> currentPasswordField,
      currentPasswordInvalidGlobalKey   -> currentPasswordField,
      emailMaxLengthGlobalKey           -> emailaddressField,
      selectedApisNonSelectedGlobalKey  -> selectedApisNonSelectedField
    )
  }
}