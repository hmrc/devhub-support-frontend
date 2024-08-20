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

package uk.gov.hmrc.devhubsupportfrontend.domain.models

import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.apiplatform.modules.common.utils.BaseJsonFormattersSpec

class SupportSessionIdSpec extends BaseJsonFormattersSpec {
  val aSessionId = SupportSessionId.random

  "SupportSessionId" should {
    "toString" in {
      aSessionId.toString() shouldBe aSessionId.value.toString()
    }

    "apply raw text" in {
      val in = SupportSessionId.random

      SupportSessionId.apply(in.value.toString()) shouldBe Some(in)
    }

    "apply raw text fails when not valid" in {
      SupportSessionId.apply("not-a-uuid") shouldBe None
    }

    "unsafeApply text" in {
      val in = SupportSessionId.random

      SupportSessionId.unsafeApply(in.value.toString()) shouldBe in
    }

    "unsafeApply raw text throws when not valid" in {
      intercept[RuntimeException] {
        SupportSessionId.unsafeApply("not-a-uuid")
      }
    }

    "convert to json" in {
      Json.toJson(aSessionId) shouldBe JsString(aSessionId.value.toString())
    }

    "read from json" in {
      testFromJson[SupportSessionId](s""""${aSessionId.toString}"""")(aSessionId)
    }
  }
}
