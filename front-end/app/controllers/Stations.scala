package controllers

import models.Station
import play.api.libs.iteratee.{Iteratee, Concurrent}
import play.api.libs.iteratee.Concurrent.Channel
import play.api.mvc._
import play.api.libs.functional.syntax._
import play.api.libs.json._

import javax.inject.Inject

import play.api.mvc.Controller

import play.modules.reactivemongo.{ // ReactiveMongo Play2 plugin
MongoController,
ReactiveMongoApi,
ReactiveMongoComponents
}

import play.api.libs.concurrent.Execution.Implicits.defaultContext

case class CreateStation(id: String, latitude: Double, longitude: Double, status: Int)

trait StationsJson {
  implicit val writesStation = Json.writes[Station]
  implicit val readsCreateStation = (
      (__ \ "id").read(Reads.minLength[String](1)) and
      (__ \ "latitude").read(Reads.min[Double](0)) and
      (__ \ "longitude").read(Reads.min[Double](0)) and
      (__ \ "status").read(Reads.min[Int](0))
    )(CreateStation.apply _)
}

class Stations @Inject() (val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents  with StationsJson  {
  val store = models.StationStore

  var feedConnections : List[Channel[String]] = Nil

  def feed =  WebSocket.using[String] { request =>

    // Concurrent.broadcast returns (Enumerator, Concurrent.Channel)
    val (out, channel) = Concurrent.broadcast[String]

    // log the message to stdout and send response back to client
    feedConnections ::= channel

    val in = Iteratee.foreach[String](s => {
      // push message to each connections
      feedConnections.foreach(_.push("{\"msg\":\"" + s + "\"}"))
    }).map(_ => {
      // unregister
      feedConnections = feedConnections.filterNot( _ == channel)
    })
    (in,out)
  }

  def list = Action.async {
    store.list.collect[Seq]().map(
      stations => Ok(Json.toJson(stations))
    )
  }

  def details(id: String) = Action.async {
    store.get(id).map {
      case Some(station) => Ok(Json.toJson(station))
      case None => NotFound(Json.parse("{}"))
    }
  }

  def heartbeat(id: String) = Action.async(parse.json[CreateStation]) { implicit request =>
   store.get(id).map {
      case Some(station) => {
        store.heartbeat(id,request.body.latitude, request.body.longitude, request.body.status) match {
          case Some(station) => {
            // Push to rabbitmq for other services
            feedConnections.foreach(_.push(Json.toJson(station).toString()))
            Ok(Json.parse("{\"success\":true}"))
          }
          case None => Ok(Json.parse("{\"success\":false,\"errormsg\":\"heartbeat failed\"}"))
        }
      }
      case None => {
        // Not listed yet
        store.create(id,request.body.latitude,request.body.longitude) match {
          case Some(station) => {
            store.heartbeat(id,request.body.latitude, request.body.longitude, request.body.status) match {
              case Some(station) => {
                // Push to rabbitmq for other services
                feedConnections.foreach(_.push(Json.toJson(station).toString()))
                Ok(Json.parse("{\"success\":true}"))
              }
              case None => Ok(Json.parse("{\"success\":false,\"errormsg\":\"heartbeat failed\"}"))
            }
          }
          case None => Ok(Json.parse("{\"success\":false,\"errormsg\":\"could not create new station\"}"))
        }
      } // None
    } // get(id)*/
  } // heartbeat

}
