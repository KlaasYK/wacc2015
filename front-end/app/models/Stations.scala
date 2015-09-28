package models

import java.util.Date

import scala.collection.concurrent.TrieMap

case class Station(id: String, latitude: Double, longitude: Double, status:Int, lastHeartbeat:Long)

trait StationStore {
  def list(): Seq[Station]
  def create(id:String, latitude: Double, longitude: Double):Option[Station]
  def get(id:String): Option[Station]
  def heartbeat(id:String, latitude: Double, longitude: Double, status:Int): Option[Station]
}

object StationStore extends StationStore {

  private val stations = TrieMap.empty[String,Station]

  def list: Seq[Station] = {
    stations.values.to[Seq]
  }

  def get(id:String): Option[Station] = {
    // TODO: implement read from database
    stations.get(id)
  }

  def create(id: String, latitude: Double, longitude: Double): Option[Station] = {
    // TODO: implement database saving
    val station = Station(id, latitude, longitude, 0, new Date().getTime())
    stations.put(id, station)
    Some(station)
  }

  def heartbeat(id: String, latitude: Double, longitude: Double, status: Int): Option[Station] = {
    // TODO: implement database saving
    val station = Station(id, latitude, longitude, status, new Date().getTime())
    stations.replace(id,station)
    Some(station)
  }

}
