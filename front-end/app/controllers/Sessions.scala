package controllers

import models.SessionsJson

import play.api.mvc._
import play.api.libs.json._

import javax.inject.Inject

import play.api.mvc.Controller

import play.modules.reactivemongo.{ // ReactiveMongo Play2 plugin
MongoController,
ReactiveMongoApi,
ReactiveMongoComponents
}

import play.api.libs.concurrent.Execution.Implicits.defaultContext


class Sessions @Inject() (val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents with SessionsJson  {
  val store = models.SessionionStore

  def list = Action.async {
    store.list.collect[Seq]().map(
      stations => Ok(Json.toJson(stations))
    )
  }

  def details(poleid: String, startDate: Long, endDate: Long) = Action.async {
    store.get(poleid, startDate, endDate).collect[Seq]().map(
      stations => Ok(Json.toJson(stations))
    )
  }

}
