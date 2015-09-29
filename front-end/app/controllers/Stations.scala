package controllers

import models.Station
import play.api.mvc._
import play.api.libs.functional.syntax._
import play.api.libs.json._

import javax.inject.Inject

import play.api.mvc.Controller

import scala.concurrent.ExecutionContext.Implicits.global

import play.modules.reactivemongo.{ // ReactiveMongo Play2 plugin
MongoController,
ReactiveMongoApi,
ReactiveMongoComponents
}

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

  def list = Action {
    Ok(Json.toJson(store.list))
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
          case Some(station) => Ok(Json.parse("{\"success\":true}"))
          case None => Ok(Json.parse("{\"success\":false,\"errormsg\":\"heartbeat failed\"}"))
        }
      }
      case None => {
        // Not listed yet
        store.create(id,request.body.latitude,request.body.longitude) match {
          case Some(station) => {
            store.heartbeat(id,request.body.latitude, request.body.longitude, request.body.status) match {
              case Some(station) => Ok(Json.parse("{\"success\":true}"))
              case None => Ok(Json.parse("{\"success\":false,\"errormsg\":\"heartbeat failed\"}"))
            }
          }
          case None => Ok(Json.parse("{\"success\":false,\"errormsg\":\"could not create new station\"}"))
        }
      } // None
    } // get(id)*/
  } // heartbeat

}
