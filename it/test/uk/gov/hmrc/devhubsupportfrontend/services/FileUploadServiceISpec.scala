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

package uk.gov.hmrc.devhubsupportfrontend.services

import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.devhubsupportfrontend.config.AppConfig
import uk.gov.hmrc.devhubsupportfrontend.domain.models.upscan.UploadStatus.UploadedSuccessfully
import uk.gov.hmrc.devhubsupportfrontend.domain.models.upscan.{S3UploadError, UploadStatus}
import uk.gov.hmrc.devhubsupportfrontend.repositories.FileCacheRepository

class FileUploadServiceISpec extends AnyWordSpec
    with GuiceOneAppPerSuite
    with Matchers
    with OptionValues
    with DefaultAwaitTimeout
    with FutureAwaits
    with BeforeAndAfterEach {

  private val fileCacheRepository = app.injector.instanceOf[FileCacheRepository]
  private val appConfig           = app.injector.instanceOf[AppConfig]

  private val underTest           = new FileUploadService(
    fileCacheRepository,
    appConfig,
    app.injector.instanceOf[org.apache.pekko.actor.ActorSystem]
  )

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}"
      )
      .build()

  override protected def beforeEach(): Unit = {
    await(fileCacheRepository.collection.drop().toFuture())
    await(fileCacheRepository.ensureIndexes())
  }

  "markFileAsPosted" should {
    "save 'posted' status for a given key" in {
      val testKey = "test-key-123"

      await(underTest.markFileAsPosted(testKey))

      val result = await(fileCacheRepository.get(testKey)(uk.gov.hmrc.mongo.cache.DataKey[UploadStatus]("status")))
      result shouldBe Some(UploadedSuccessfully(""))
    }
  }

  "markFileAsRejected" should {
    "save rejected status with error details for a given S3UploadError" in {
      val testKey = "test-key-456"
      val s3Error = S3UploadError(
        key = testKey,
        errorCode = "NoSuchKey",
        errorMessage = "The resource you requested does not exist"
      )

      await(underTest.markFileAsRejected(s3Error))

      val result = await(fileCacheRepository.get(testKey)(uk.gov.hmrc.mongo.cache.DataKey[UploadStatus]("status")))
      result shouldBe Some(UploadStatus.Failed(s3Error.errorMessage, s3Error.errorCode))
    }
  }

  "getFileVerificationStatus" should {
    "retrieve the status for a given key" in {
      val testKey = "test-key-789"

      await(underTest.markFileAsPosted(testKey))

      val result = await(underTest.getFileVerificationStatus(testKey))

      result shouldBe Some(UploadedSuccessfully(""))
    }

    "return None when no status is found for a key" in {
      val result = await(underTest.getFileVerificationStatus("non-existent-key"))

      result shouldBe None
    }
  }
}
