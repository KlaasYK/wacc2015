import controllers.ETCDWatcher
import play.api._
import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent.Future

object Global extends GlobalSettings {
  override def onStart(app: Application): Unit = {
    val watcher = ETCDWatcher
    // TODO: load this from file or settings or something similar
    watcher.start("http://heartbeat1:4001/v2/keys/poles?wait=true&recursive=true",0);
  }

  override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(
      NotFound(views.html.notfound("404 - Handler not found", s"Handler not found for: ${request.path}"))
    )
  }
}
