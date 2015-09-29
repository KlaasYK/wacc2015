package models

import java.util.Date

import play.api.libs.functional.syntax._

import play.api.libs.json._
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.{JSONQueryBuilder, JSONCollection}

import scala.collection.concurrent.TrieMap

import play.api.Play.current
import play.api.inject.ApplicationLifecycle

import reactivemongo.api.{Cursor, MongoConnection, MongoDriver}

import scala.concurrent.ExecutionContext.Implicits.global


import scala.concurrent.Future

case class CreateStation(id: String, latitude: Double, longitude: Double, status: Int)

trait StationsJson {
  implicit val writesStation = Json.writes[Station]
  implicit val readsStation = (
    (__ \ "id").read(Reads.minLength[String](1)) and
      (__ \ "latitude").read(Reads.min[Double](0)) and
      (__ \ "longitude").read(Reads.min[Double](0)) and
      (__ \ "status").read(Reads.min[Int](0))
    )(CreateStation.apply _)
}

case class Station(id: String, latitude: Double, longitude: Double, status:Int, lastHeartbeat:Long)

trait StationStore {
  def list(): Seq[Station]
  def create(id:String, latitude: Double, longitude: Double):Option[Station]
  def get(id:String): Future[Option[Station]]
  def heartbeat(id:String, latitude: Double, longitude: Double, status:Int): Option[Station]
}

object StationStore extends StationStore with StationsJson{
  implicit val stationReads = Json.reads[Station]

  // TODO: make these somewhere global!
  def driver = registerDriverShutdownHook(MongoDriver()) // first pool
  def connection = driver.connection(List("localhost:27017/test"))
  def collection = connection.db("wacc").collection("stations") : JSONCollection

  private val stations = TrieMap.empty[String,Station]

  def list: Seq[Station] = {
    // TODO: implement read from database
    stations.values.to[Seq]
  }

  def get(id:String): Future[Option[Station]] = {
    // TODO: implement read from database
    //stations.get(id)
    val query = Json.obj("id" -> id)
    collection.find(query).one[Station]
    /*val fut = collection.find(query).one[Station]
    fut.map( station => {
      System.out.println(station)
    })
    None*/
  }

  def create(id: String, latitude: Double, longitude: Double): Option[Station] = {
    val station = Station(id, latitude, longitude, 0, new Date().getTime())
    stations.put(id, station)
    collection.insert(station).map(lastError => None)
    Some(station)
  }

  def heartbeat(id: String, latitude: Double, longitude: Double, status: Int): Option[Station] = {
    // TODO: implement database saving
    val station = Station(id, latitude, longitude, status, new Date().getTime())
    stations.replace(id,station)
    Some(station)
  }

  def registerDriverShutdownHook(mongoDriver: MongoDriver): MongoDriver = {
    current.injector.instanceOf[ApplicationLifecycle].
      addStopHook { () => Future(mongoDriver.close()) }
    mongoDriver
  }

}
