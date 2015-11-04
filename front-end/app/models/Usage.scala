package models

import java.util.Date

import play.api.libs.functional.syntax._

import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection

import play.api.Play.current

import reactivemongo.api.Cursor

import scala.concurrent.Future


import play.api.libs.concurrent.Execution.Implicits.defaultContext

case class CreateUsage(_id: String, kwh: Double, price: Double)

trait UsageJson {
  implicit val writesUsage = Json.writes[Usage]
  implicit val readsUsage = (
      (__ \ "_id").read(Reads.minLength[String](1)) and
      (__ \ "kwh").read(Reads.min[Double](0)) and
      (__ \ "price").read(Reads.min[Double](0))
    )(CreateUsage.apply _)
}

case class Usage(_id: String, kwh: Double, price: Double)

trait UsageStore {
  def list(): Cursor[Usage]
  def get(id:String): Future[Option[Usage]]
}

object UsageStore extends UsageStore with UsageJson {
  implicit val usageReads = Json.reads[Usage]

  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]

  def collection(name: String): JSONCollection =
    reactiveMongoApi.db.collection[JSONCollection](name)

  def list: Cursor[Usage] = {
    // TODO: limit
    val query = Json.obj()
    collection("sessionsout").find(query).cursor[Usage]()
  }

  def get(id:String): Future[Option[Usage]] = {
    val query = Json.obj("_id" -> id)
    collection("sessionsout").find(query).one[Usage]
  }

}
