package zhongl.di.github

import akka.actor.{ Actor, ActorLogging, Props, Timers }
import spray.json.JsValue
import zhongl.di.github.AppActor.{ Ack, Event }

class AppActor extends Actor with Timers with ActorLogging {

  override def receive = {
    case Event("pull_request", json) =>
      log.info(json.prettyPrint)
      sender() ! Ack("handled")
    case Event(_, _) =>
      sender() ! Ack("Ignored")
  }

}

object AppActor {

  final case class Event(name: String, json: JsValue)

  final case class Ack(message: String)

  def props = Props[AppActor]

}

