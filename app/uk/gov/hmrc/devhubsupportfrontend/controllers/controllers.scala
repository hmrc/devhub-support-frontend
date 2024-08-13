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

import uk.gov.hmrc.devhubsupportfrontend.controllers.models.{FieldMessageKey, FieldNameKey, GlobalMessageKey}

package object controllers {

  object Conversions {
    implicit def fromFieldNameKeyToString(in: FieldNameKey): String         = in.value
    implicit def fromGlobalMessageKeyToString(in: GlobalMessageKey): String = in.value
    implicit def fromFieldMessageKeyToString(in: FieldMessageKey): String   = in.value
  }

  object FormKeys {
    val emailaddressRequiredKey = FieldMessageKey("emailaddress.error.required.field")
    val emailaddressNotValidKey = FieldMessageKey("emailaddress.error.not.valid.field")
    val emailMaxLengthKey       = FieldMessageKey("emailaddress.error.maxLength.field")

    val formKeysMap: Map[FieldMessageKey, GlobalMessageKey] = Map()

    def findFieldKeys(rawMessage: String): Option[(FieldMessageKey, GlobalMessageKey)] = {
      formKeysMap.find(_._1.value == rawMessage)
    }

    val globalKeys: Seq[GlobalMessageKey] = formKeysMap.values.toSeq

    val globalToField: Map[GlobalMessageKey, FieldNameKey] = Map()
  }
}
