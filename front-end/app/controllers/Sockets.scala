package controllers

import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.mvc._
import play.api.libs.iteratee.Concurrent.Channel
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor._

object MyWebSocketActor {
  def props(out: ActorRef) = Props(new MyWebSocketActor(out))
}

class MyWebSocketActor(out: ActorRef) extends Actor {
  def receive = {
    case msg: String =>
      out ! ("I received your message: " + msg)
  }
}

class Sockets extends Controller {
  /* old thingy
   def socket = WebSocket.acceptWithActor[String, String] { request => out =>
  MyWebSocketActor.props(out)
}*/
  var roomConnections : List[Channel[String]] = Nil

  def socket =  WebSocket.using[String] { request =>

    // Concurrent.broadcast returns (Enumerator, Concurrent.Channel)
    val (out, channel) = Concurrent.broadcast[String]

    // log the message to stdout and send response back to client
    roomConnections ::= channel

    val in = Iteratee.foreach[String](s => {
      println(s)
      // push message to each connections
      roomConnections.foreach(_.push("Received message" + s))
    }).map(_ => {
      // unregister
      roomConnections = roomConnections.filterNot( _ == channel)
      println("Disconnected")
    })
    (in,out)
  }

  def stuff = Action {
    // Do other stuff
    roomConnections.foreach(_.push("This is an anouncement"))
    Ok("test");
  }

}

