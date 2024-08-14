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

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

import org.mongodb.scala.bson.{BsonValue, Document}
import org.mongodb.scala.model.Aggregates.{filter, project}
import org.mongodb.scala.model.Projections.fields
import org.mongodb.scala.model.{Filters, Projections}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Format, OFormat}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.mongo.play.json.Codecs
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import uk.gov.hmrc.devhubsupportfrontend.domain.models.{SupportFlow, SupportSessionId}

class FlowRepositoryISpec extends AnyWordSpec
    with GuiceOneAppPerSuite
    with Matchers
    with OptionValues
    with DefaultAwaitTimeout
    with FutureAwaits
    with BeforeAndAfterEach {

  private val currentSession        = SupportSessionId.random
  private val anotherSession        = SupportSessionId.random
  private val currentEntrySelection = "current"
  private val anotherEntrySelection = "another"

  private val flowRepository = app.injector.instanceOf[FlowRepository]

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}"
      )
      .build()

  override protected def beforeEach(): Unit = {
    await(flowRepository.collection.drop().toFuture())
    await(flowRepository.ensureIndexes())
  }

  trait PopulatedSetup {
    val currentFlow          = SupportFlow(currentSession, currentEntrySelection)
    val flowInAnotherSession = SupportFlow(anotherSession, anotherEntrySelection)

    await(flowRepository.saveFlow(currentFlow))
    await(flowRepository.saveFlow(flowInAnotherSession))
    await(flowRepository.collection.countDocuments().toFuture()) shouldBe 2

    def fetchLastUpdated(flow: SupportFlow): Instant = {
      val query = Document("sessionId" -> flow.sessionId.toString)

      await(flowRepository.collection.aggregate[BsonValue](
        Seq(
          filter(query),
          project(fields(Projections.excludeId(), Projections.include("lastUpdated")))
        )
      ).head()
        .map(Codecs.fromBson[ResultSet]))
        .lastUpdated
    }
  }

  "FlowRepository" when {

    "saveFlow" should {
      "save SupportFlow with entrySelection" in {
        val flow = SupportFlow(currentSession, currentEntrySelection)

        await(flowRepository.saveFlow(flow))

        val result = await(flowRepository.collection.find(Filters.equal("sessionId", currentSession.toString)).headOption())
        result match {
          case Some(savedFlow: SupportFlow) =>
            savedFlow.sessionId shouldBe currentSession
            savedFlow.entrySelection shouldBe currentEntrySelection
          case _                            => fail()
        }

      }

      "update the flow when it already exists" in new PopulatedSetup {
        val newEntrySelection        = "new selection"
        await(flowRepository.saveFlow(currentFlow))
        val updatedFlow: SupportFlow = currentFlow.copy(entrySelection = newEntrySelection)

        val result: SupportFlow = await(flowRepository.saveFlow(updatedFlow))

        result shouldBe updatedFlow
        val updatedDocument: SupportFlow = await(flowRepository.collection
          .find(Document("sessionId" -> currentSession.toString)).map(_.asInstanceOf[SupportFlow]).head())

        updatedDocument.entrySelection shouldBe newEntrySelection
      }
    }

    "fetchBySessionId" should {
      "fetch the flow for the specified session ID" in new PopulatedSetup {
        val result = await(flowRepository.fetchBySessionId(currentSession))

        result shouldBe Some(currentFlow)
      }

      "return None when the query does not match any data" in {
        val result = await(flowRepository.fetchBySessionId(currentSession))

        result shouldBe None
      }
    }

    "updateLastUpdated" should {
      "update lastUpdated for the specified session ID" in new PopulatedSetup {
        val lastUpdatedInCurrentFlow: Instant            = fetchLastUpdated(currentFlow)
        val lastUpdatedInFlowInDifferentSession: Instant = fetchLastUpdated(flowInAnotherSession)

        await(flowRepository.saveFlow(currentFlow))

        fetchLastUpdated(currentFlow).isAfter(lastUpdatedInCurrentFlow) shouldBe true
        fetchLastUpdated(flowInAnotherSession) shouldBe lastUpdatedInFlowInDifferentSession
      }
    }
  }
}

case class ResultSet(lastUpdated: Instant)

object ResultSet {
  import play.api.libs.json.Json
  implicit val dateFormat: Format[Instant]         = MongoJavatimeFormats.instantFormat
  implicit val resultSetFormat: OFormat[ResultSet] = Json.format[ResultSet]

  def apply(lastUpdated: Instant) = new ResultSet(lastUpdated)
}
