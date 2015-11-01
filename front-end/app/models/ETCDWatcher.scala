package models

import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.Json
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
   * @param channel websocket channel
   * @return whether adding was successful
   */
  def addListener(channel : Channel[String]) : Boolean = {
    feedConnections::=channel
    true
  }

  /**
   * Removes a listener to this server
   * @param channel websocket channel
   * @return whether removal was successful
   */
  def removeListener(channel : Channel[String]) : Boolean = {
    feedConnections = feedConnections.filterNot( _ == channel)
    true
  }

  /**
   * Starts infinite polling loop
   * @param uri to listen at
   */
  def start(uri : String, index : Int): Unit = {
    // TODO: add index to uri
    var newindex = index + 1
    WS.url(uri).get().map(response => {
      // TODO: check if index change of index is necessary in response headers
      val obj = response.json
      (obj \ "action").get.toString() match {
        case "\"set\"" =>
          val node = obj \ "node"
          val prevnode = obj \ "prevNode"
          val curval = (prevnode \ "value").getOrElse(Json.parse("-1")).toString().stripPrefix("\"").stripSuffix("\"")
          val nextval = (node \ "value").get.toString().stripPrefix("\"").stripSuffix("\"")
          if (!curval.equals(nextval)) {
            // Send update
            val polestring = (node \ "key").get.toString().stripPrefix("\"/poles/").stripSuffix("\"")
            val id = polestring.substring(polestring.indexOf("/") + 1)

            feedConnections.foreach(_.push("{\"id\":\"" + id + "\",\"status\":" + nextval + "}"))
          }

        case "\"expire\"" =>
          // Send expiration to each connected client
          val node = obj \ "node"
          val polestring = (node \ "key").get.toString().stripPrefix("\"/poles/").stripSuffix("\"")
          val id = polestring.substring(polestring.indexOf("/") + 1)
          feedConnections.foreach(_.push("{\"id\":\"" + id + "\",\"status\":0}"))

        case _ =>
          println("Unknown action")
      }

      // Start listening again
      start(uri, newindex)
    })
  }

}
