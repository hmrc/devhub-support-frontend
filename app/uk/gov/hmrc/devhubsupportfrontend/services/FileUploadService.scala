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

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.actor.{ActorSystem, Scheduler}

import uk.gov.hmrc.mongo.cache.DataKey

import uk.gov.hmrc.devhubsupportfrontend.config.AppConfig
import uk.gov.hmrc.devhubsupportfrontend.domain.models.upscan.UploadStatus.UploadedSuccessfully
import uk.gov.hmrc.devhubsupportfrontend.domain.models.upscan.{S3UploadError, UploadStatus}
import uk.gov.hmrc.devhubsupportfrontend.repositories.FileCacheRepository

class FileUploadService @Inject() (
    repo: FileCacheRepository,
    val appConfig: AppConfig,
    val actorSystem: ActorSystem
  )(implicit ec: ExecutionContext
  ) {

  implicit lazy val scheduler: Scheduler = actorSystem.scheduler

  def markFileAsPosted(key: String): Future[Unit] = {
    val dataKey = DataKey[UploadStatus]("status")
    repo.put(key)(dataKey, UploadedSuccessfully).map(_ => ())
  }

  def markFileAsRejected(s3UploadError: S3UploadError): Future[Unit] = {
    val dataKey = DataKey[UploadStatus]("status")
    repo.put(s3UploadError.key)(dataKey, UploadStatus.Failed(s3UploadError.errorMessage, s3UploadError.errorCode)).map(_ => ())
  }

  def getFileVerificationStatus(key: String): Future[Option[UploadStatus]] = {
    val dataKey = DataKey[UploadStatus]("status")
    repo.get(key)(dataKey)
  }
}
