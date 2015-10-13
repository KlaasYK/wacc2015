package models

import play.api.libs.functional.syntax._

import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection

import play.api.Play.current

import reactivemongo.api.Cursor

import play.api.libs.concurrent.Execution.Implicits.defaultContext

case class CreateSession(startDate: Long, endDate: Long, kwh: Double, price: Double)

// TODO: implement card

trait SessionsJson {
  implicit val writesSesion = Json.writes[Session]
  implicit val readsStation = (
    (__ \ "startDate").read(Reads.min[Long](0)) and
      (__ \ "endDate").read(Reads.min[Long](0)) and
      (__ \ "kwh").read(Reads.min[Double](0)) and
      (__ \ "price").read(Reads.min[Double](0))
    )(CreateSession.apply _)
}

case class Session(poleid: String, startDate: Long, endDate: Long, kwh: Double, price: Double)

trait SessionStore {
  def list(): Cursor[Session]
  def create(poleid: String, startDate: Long, endDate: Long, kwh: Double, price: Double):Option[Session]
  def get(poleid: String, startDate: Long, endDate: Long): Cursor[Session]
}

object SessionionStore  extends SessionStore with SessionsJson{
  implicit val sessionReads = Json.reads[Session]

  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]

  def collection(name: String): JSONCollection =
    reactiveMongoApi.db.collection[JSONCollection](name)

  def list: Cursor[Session] = {
    // TODO: limit
    val query = Json.obj()
    collection("sessions").find(query).cursor[Session]()
  }

  def get(poleid:String, startDate: Long, endDate: Long): Cursor[Session] = {
    val query = Json.obj("poleid" -> poleid)
    // FIXME: add date filters
    collection("sessions").find(query).cursor[Session]()
  }

  def create(poleid: String, startDate: Long, endDate: Long, kwh: Double, price: Double): Option[Session] = {
    val session = Session(poleid, startDate, endDate, kwh, price)
    // TODO: check if insert worked (change return type to future)
    collection("sessions").insert(session).map(lastError => None)
    Some(session)
  }

}
