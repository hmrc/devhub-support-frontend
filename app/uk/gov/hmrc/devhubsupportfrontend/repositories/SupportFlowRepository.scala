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

package uk.gov.hmrc.devhubsupportfrontend.repositories

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions, UpdateOptions, Updates}

import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.devhubsupportfrontend.domain.models.SupportFlow
import uk.gov.hmrc.devhubsupportfrontend.domain.models.SupportSessionId
import uk.gov.hmrc.devhubsupportfrontend.config.AppConfig
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import java.time.Instant

object SupportFlowRepository {
  import play.api.libs.json._
  implicit val dateFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit val formatSupportFlow: Format[SupportFlow]                                                 = Json.format[SupportFlow]
}

@Singleton
class SupportFlowRepository @Inject() (mongo: MongoComponent, appConfig: AppConfig)(implicit val ec: ExecutionContext)
    extends PlayMongoRepository[SupportFlow](
      collectionName = "flows",
      mongoComponent = mongo,
      domainFormat = SupportFlowRepository.formatSupportFlow,
      indexes = Seq(
        IndexModel(
          ascending("sessionId"),
          IndexOptions().name("session_idx")
            .unique(true)
            .background(true)
        ),
        IndexModel(
          ascending("lastUpdated"),
          IndexOptions().name("last_updated_ttl_idx")
            .background(true)
            .expireAfter(appConfig.supportSessionTimeout.getSeconds(), TimeUnit.SECONDS)
        )
      )
    ) {

  def saveFlow[A <: SupportFlow](flow: A): Future[A] = {
    val query = equal("sessionId", flow.sessionId.toString)

    collection.find(query).headOption() flatMap {
      case Some(_: SupportFlow) =>
        for {
          updatedFlow <- collection.replaceOne(
                           filter = query,
                           replacement = flow
                         ).toFuture().map(_ => flow)

          _ <- updateLastUpdated(flow.sessionId)
        } yield updatedFlow

      case None =>
        for {
          newFlow <- collection.insertOne(flow).toFuture().map(_ => flow)
          _       <- updateLastUpdated(flow.sessionId)
        } yield newFlow
    }
  }

  def deleteBySessionId(sessionId: SupportSessionId): Future[Boolean] = {
    collection.deleteOne(equal("sessionId", sessionId.toString))
      .toFuture()
      .map(_.wasAcknowledged())
  }

  def fetchBySessionId(sessionId: SupportSessionId): Future[Option[SupportFlow]] = {
    collection.find(equal("sessionId", sessionId.toString)).headOption()
  }

  def updateLastUpdated(sessionId: SupportSessionId): Future[Unit] = {
    collection.updateMany(
      filter = equal("sessionId", sessionId.toString),
      update = Updates.currentDate("lastUpdated"),
      options = new UpdateOptions().upsert(false)
    ).toFuture()
      .map(_ => ())
  }
}
