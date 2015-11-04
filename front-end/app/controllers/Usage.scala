package controllers

import javax.inject.Inject

import models.{CreateUsage, UsageJson}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.{Controller, _}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}


class Usage @Inject() (val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents with UsageJson  {
  val store = models.UsageStore

  def list = Action.async {
    store.list.collect[Seq]().map(
      usage => Ok(Json.toJson(usage))
    )
  }

  def details(id: String) = Action.async {
    store.get(id).map {
      case Some(usage) => Ok(Json.toJson(usage))
      case None => NotFound(Json.parse("{}"))
    }
  }
}
