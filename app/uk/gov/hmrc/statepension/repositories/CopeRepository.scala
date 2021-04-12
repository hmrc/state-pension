package uk.gov.hmrc.statepension.repositories

import com.google.inject.Inject
import com.mongodb.client.model.IndexOptions
import org.bson.conversions.Bson
import com.mongodb.client.model.Filters.{eq => mongoEq}
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.mongodb.scala.model.Indexes.ascending
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.statepension.controllers.{ErrorResponseCope, ErrorResponseCopeProcessing}

import scala.concurrent.{ExecutionContext, Future}

class CopeRepository @Inject()(mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[ErrorResponseCopeProcessing](
    collectionName = "cope",
    mongoComponent = mongoComponent,
    domainFormat = ErrorResponseCope.copeProcessingFormat,
    indexes = Seq(
      IndexModel(ascending("nino"),
      IndexOptions().name("indexed_id").unique(true))
    )
  ) {
  def put = ???
  def update = ???
  def find(nino: String): Future[ErrorResponseCopeProcessing] = collection.find().filter(_.nino == nino).headOption().toFuture()
  def delete = ???
}
