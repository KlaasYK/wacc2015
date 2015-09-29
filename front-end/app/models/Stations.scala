package models

import java.util.Date

import controllers.{StationsJson, CreateStation}
import play.api.libs.json._
import play.modules.reactivemongo.json.collection.JSONCollection

import scala.collection.concurrent.TrieMap

import play.api.Play.current
import play.api.inject.ApplicationLifecycle

import reactivemongo.api.{Cursor, MongoConnection, MongoDriver}

import scala.concurrent.ExecutionContext.Implicits.global


import scala.concurrent.Future

case class Station(id: String, latitude: Double, longitude: Double, status:Int, lastHeartbeat:Long)

trait StationStore {
  def list(): Seq[Station]
  def create(id:String, latitude: Double, longitude: Double):Option[Station]
  def get(id:String): Option[Station]
  def heartbeat(id:String, latitude: Double, longitude: Double, status:Int): Option[Station]
}

object StationStore extends StationStore with StationsJson{
  private def driver = registerDriverShutdownHook(MongoDriver()) // first pool
  private def connection = driver.connection(List("localhost:27017"))
  private def collection = connection.db("wacc").collection("stations") : JSONCollection

  private val stations = TrieMap.empty[String,Station]

  def list: Seq[Station] = {
    stations.values.to[Seq]
  }

  def get(id:String): Option[Station] = {
    // TODO: implement read from database
    //stations.get(id)
    val futurestation = collection.find(Json.obj("id" -> id)).one[Station]
    futurestation.map(
      station => Some(station)
    )
    None
  }

  def create(id: String, latitude: Double, longitude: Double): Option[Station] = {
    // TODO: implement database saving
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
