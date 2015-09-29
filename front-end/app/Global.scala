import play.api._
import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent.Future

import reactivemongo.api.{ DB, MongoConnection, MongoDriver }

trait ReactiveMongoApi {
  def driver: MongoDriver
  def connection: MongoConnection
  def db: DB
}

object Global extends GlobalSettings {
  override def onStart(app: Application): Unit = {

  }

  override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(
      NotFound(views.html.notfound("404 - Handler not found", s"Handler not found for: ${request.path}"))
    )
  }
}
