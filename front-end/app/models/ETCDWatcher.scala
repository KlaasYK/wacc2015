package models

import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.ws.WS

import play.api.Play.current

trait ETCDWatcher {
  def start(uri : String, index : Int)
  def addListener(channel : Channel[String]) : Boolean
  def removeListener(channel : Channel[String]) : Boolean
}

object ETCDWatcher extends  ETCDWatcher {

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  var feedConnections : List[Channel[String]] = Nil

  /**
   * Add a listener to this server
   * @param channel
   * @return whether adding was successful
   */
  def addListener(channel : Channel[String]) : Boolean = {
    feedConnections::=channel
    return true
  }

  /**
   * Removes a listener to this server
   * @param channel
   * @return whether removal was successful
   */
  def removeListener(channel : Channel[String]) : Boolean = {
    feedConnections = feedConnections.filterNot( _ == channel)
    return true
  }

  /**
   * Starts infinite polling loop
   * @param uri to listen at
   */
  def start(uri : String, index : Int): Unit = {
    // TODO: mark
    WS.url(uri).get().map(response => {
      // TODO: handle response
      println(response.json)
      // FIXME: will this cause a stackoverflow????? Probably not, however, it might cause a memory leak. Which seems to be fixed
      start(uri, index+1)
    })
  }

}
