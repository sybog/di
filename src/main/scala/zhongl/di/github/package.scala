package zhongl.di

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import spray.json.{DefaultJsonProtocol, JsValue}
import zhongl.di.github.AppActor.{Ack, Event}

import scala.concurrent.duration._

package object github extends SprayJsonSupport {
  implicit val timeout = Timeout(5.seconds)

  private[github] val webhook = path("webhook") & post & headerValueByName("X-GitHub-Event") & entity(as[JsValue])

  def routes(implicit system: ActorSystem, materialize: ActorMaterializer) = {
    import system.dispatcher

    val app = system.actorOf(AppActor.props, "github-app")

    webhook { (event, json) =>
      onSuccess((app ? Event(event, json)).mapTo[Ack].map(_.message)) {
        complete(_)
      }
    }

  }

  private[github] trait JsonProtocol extends DefaultJsonProtocol {

    implicit val repository = jsonFormat1(Model.Repository)

    implicit val head = jsonFormat2(Model.Commit)

    implicit val pullRequest = jsonFormat4(Model.PullRequest)

    implicit val installation = jsonFormat2(Model.Installation)

    implicit val payload = jsonFormat3(Model.Payload)

    implicit val jsValueToPayload: JsValue => Model.Payload = _.convertTo[Model.Payload]
  }

  private[github] object Model {

    final case class Repository(`ssh_url`: String)

    final case class Commit(sha: String, repo: Repository)

    final case class PullRequest(head: Commit, base: Commit, merged: Boolean, `merge_commit_sha`: Option[String])

    final case class Installation(id: Int, `access_tokens_url`: Option[String])

    final case class Payload(action: String, installation: Option[Installation], `pull_request`: Option[PullRequest])

  }

}
