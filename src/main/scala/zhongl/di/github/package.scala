package zhongl.di

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import spray.json.JsValue
import zhongl.di.github.AppActor.{ Ack, Event }
import akka.util.Timeout

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
}
