package controllers

import models.{CreateStation, StationsJson}
import play.api.libs.iteratee.{Iteratee, Concurrent}
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.ws.WS
import play.api.mvc._
import play.api.libs.json._

import javax.inject.Inject

import play.api.mvc.Controller

import play.modules.reactivemongo.{ // ReactiveMongo Play2 plugin
MongoController,
ReactiveMongoApi,
ReactiveMongoComponents
}

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class Stations @Inject() (val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents  with StationsJson  {
  val store = models.StationStore
  val watcher = models.ETCDWatcher

  // TODO: load this from somewhere
  val base = "http://heartbeat1:2379/v2/keys/poles/"

  var feedConnections : List[Channel[String]] = Nil

  def feed =  WebSocket.using[String] { request =>

    // Concurrent.broadcast returns (Enumerator, Concurrent.Channel)
    val (out, channel) = Concurrent.broadcast[String]

    watcher.addListener(channel)

    // log the message to stdout and send response back to client
    feedConnections ::= channel

    val in = Iteratee.foreach[String](s => {
      // push message to each connections
      feedConnections.foreach(_.push("{\"msg\":\"" + s + "\"}"))
    }).map(_ => {
      // unregister
      feedConnections = feedConnections.filterNot( _ == channel)
      watcher.removeListener(channel)
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

  // TODO: function to be called with actor from etcd

  def heartbeat(id: String) = Action.async(parse.json[CreateStation]) { implicit request =>
   store.get(id).map {
      case Some(station) => {
        store.heartbeat(id,request.body.latitude, request.body.longitude, request.body.status) match {
          case Some(station) => {
            // TODO: use this somewhere feedConnections.foreach(_.push(Json.toJson(station).toString()))
            // TODO: make 30 seconds somewhere variable
            WS.url(base + id).withHeaders("Content-Type"->"application/x-www-form-urlencoded").put("value=" + station.status + "&ttl=30").map {
              result =>
                // TODO: this is fire and forget, maybe check result...
                //println(result.json);
            }
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
                // TODO: make 30 seconds somewhere variable
                WS.url(base + id).withHeaders(("Content-Type","application/x-www-form-urlencoded")).put("value=" + station.status + "&ttl=30").map {
                  result =>
                    // TODO: this is fire and forget, maybe check result...
                    //println(result.json);
                }

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
