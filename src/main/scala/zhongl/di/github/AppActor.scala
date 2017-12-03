package zhongl.di.github

import akka.actor.Status.Status
import akka.actor.{Actor, ActorLogging, PoisonPill, Props, Status, Timers}
import akka.http.scaladsl.model.Uri
import spray.json._

class AppActor(installation: Uri => Props) extends Actor with Timers with ActorLogging with DefaultJsonProtocol {
  import AppActor._

  implicit val jsValueToPayload: JsValue => Payload = _.convertTo[Payload]
  implicit val repository: JsonFormat[Repository]   = jsonFormat1(Repository)
  implicit val commit: JsonFormat[Commit]           = jsonFormat2(Commit)
  implicit val pullRequest: JsonFormat[PullRequest] = jsonFormat4(PullRequest)
  implicit val inst: JsonFormat[Installation]       = jsonFormat2(Installation)
  implicit val payload: JsonFormat[Payload]         = jsonFormat3(Payload)

  override def receive = {
    case Event("pull_request", payload) => sender() ! processPullRequestEvent(payload)
    case Event("installation", payload) => sender() ! processInstallationEvent(payload)
    case Event(name, _)                 => sender() ! Status.Success(s"Ignored ${name}.")
  }

  private def processPullRequestEvent: PartialFunction[Payload, Status] = {
    case Payload("opened" | "synchronize", Installation(id, _), Some(PullRequest(Commit(sha, Repository(ssh)), _, false, _))) =>
      Status.Success(s"Try to test it.")
    case Payload("closed", Installation(id, _), Some(PullRequest(_, Commit(_, Repository(ssh)), true, Some(sha)))) =>
      // TODO Release
      log.info("Send Release({}, {}) to installation actor[{}]", ssh, sha, id)

      Status.Success(s"Try to release it.")
    case Payload(action, _, _) =>
      Status.Success(s"Ignored $action.")
  }

  private def processInstallationEvent: PartialFunction[Payload, Status.Success] = {
    case Payload("created", Installation(id, Some(uri)), _) =>
      context.actorOf(installation(uri), id)
      Status.Success(s"Create installation $id.")
    case Payload("deleted", Installation(id, _), _) =>
      context.child(id).foreach(_ ! PoisonPill)
      Status.Success(s"Delete installation $id.")
    case Payload(action, _, _) =>
      Status.Success(s"Ignored ${action}.")
  }

}

object AppActor extends DefaultJsonProtocol {

  final case class Repository(`ssh_url`: String)

  final case class Commit(sha: String, repo: Repository)

  final case class PullRequest(head: Commit, base: Commit, merged: Boolean, `merge_commit_sha`: Option[String])

  final case class Installation(id: String, `access_tokens_url`: Option[String])

  final case class Payload(action: String, installation: Installation, `pull_request`: Option[PullRequest])

  final case class Event(name: String, payload: JsValue)

  def props(installation: Uri => Props): Props = Props(new AppActor(installation))

}
