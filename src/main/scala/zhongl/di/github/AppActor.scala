package zhongl.di.github

import akka.actor.{Actor, ActorLogging, Props, Timers}
import spray.json._
import zhongl.di.github.AppActor._
import zhongl.di.github.Model._

class AppActor extends Actor with Timers with ActorLogging with JsonProtocol {

  override def receive = {
    case Event("pull_request", json) => sender() ! processPullRequestEvent(json)
    case Event("installation", json) => sender() ! processIntegrationEvent(json)
    case Event(name, _)              => sender() ! Ack(s"Ignored ${name}.")
  }

  private def processPullRequestEvent: PartialFunction[Payload, Ack] = {
    case Payload("opened" | "synchronize", Some(Installation(id, _)), Some(PullRequest(Commit(sha, Repository(ssh)), _, false, _))) =>
      // TODO Test
      log.info("Send Test({}, {}) to installation actor[{}]", ssh, sha, id)

      Ack(s"Try to test it.")
    case Payload("closed", Some(Installation(id, _)), Some(PullRequest(_, Commit(_, Repository(ssh)), true, Some(sha)))) =>
      // TODO Release
      log.info("Send Release({}, {}) to installation actor[{}]", ssh, sha, id)

      Ack(s"Try to release it.")
    case Payload(action, _, _) =>
      Ack(s"Ignored $action.")
  }

  private def processIntegrationEvent: PartialFunction[Payload, Ack] = {
    case Payload("created", Some(Installation(id, Some(accessTokensUrl))), _) =>
      // TODO Create installation actor
      Ack(s"Create installation $id.")
    case Payload("deleted", Some(Installation(id, Some(accessTokensUrl))), _) =>
      // TODO Remove installation actor
      Ack(s"Delete installation $id.")
    case Payload(action, _, _) =>
      Ack(s"Ignored ${action}.")
  }
}

object AppActor {

  final case class Event(name: String, json: JsValue)

  final case class Ack(message: String)

  def props = Props[AppActor]

}

