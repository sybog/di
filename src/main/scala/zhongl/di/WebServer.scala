package zhongl.di

import java.security.interfaces.RSAPrivateKey

import akka.actor.{ActorSystem, Props}
import akka.actor.Status.Status
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directives
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import spray.json.JsValue
import zhongl.di.github.AppActor.Event
import zhongl.di.github._
import zhongl.di.util.PEMFile

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object WebServer extends Directives with SprayJsonSupport {

  implicit val durationToFiniteDuration: java.time.Duration => FiniteDuration = d => FiniteDuration(d.toMillis, MILLISECONDS)

  val MAX_VALID: FiniteDuration = 10.minutes

  type Hook = (String, JsValue) => Future[String]

  def main(args: Array[String]): Unit = args match {
    case Array(githubApiId, privateKeyPath) => run(githubApiId, PEMFile(privateKeyPath).asRSAPrivateKey)
    case _                                  => throw new IllegalArgumentException("Missing args: <GitHub APP ID> <Private Key Path>")
  }

  def run(id: String, key: RSAPrivateKey): Unit = {
    implicit val system       = ActorSystem("di")
    implicit val materializer = ActorMaterializer()
    implicit val executor     = system.dispatcher
    implicit val timeout      = Timeout(5.seconds)

    val app        = system.actorOf(AppActor.props(installation(id, key)), "github-app")
    val hook: Hook = (event, json) => (app ? Event(event, json)).mapTo[Status].map(_.toString)
    val interface  = system.settings.config.getString("di.interface")
    val port       = system.settings.config.getInt("di.port")

    Http().bindAndHandle(route(hook), interface, port).onComplete {
      case Success(bound) =>
        println(s"Server online at ${bound.localAddress}/\nPress CTRL+C to stop...")
        sys.addShutdownHook {
          bound.unbind().onComplete(_ => system.terminate())
        }

      case Failure(error) =>
        error.printStackTrace()
        system.terminate()

    }

  }

  def route(h: Hook)(implicit materializer: ActorMaterializer) = {
    (path("webhook") & post & headerValueByName("X-GitHub-Event") & entity(as[JsValue])) { (event, json) =>
      onSuccess(h(event, json)) {
        complete(_)
      }
    }
  }

  private def installation(id: String, key: RSAPrivateKey)(implicit system: ActorSystem): Uri => Props = {
    val config = system.settings.config

    def min(a: FiniteDuration, b: FiniteDuration) = if (a > b) b else a
    def duration(path: String, defaultMax: FiniteDuration): FiniteDuration = {
      if (config.hasPath(path)) min(config.getDuration(path), defaultMax) else defaultMax
    }

    val timeout     = system.settings.config.getDuration("akka.http.client.idle-timeout").plus(1.second)
    val valid       = duration("di.github.app.jwt.valid-duration", MAX_VALID)
    val jwt: GetJWT = () => { signRSA256(id, valid, key) }

    (uri => InstallationActor.props(f => Api(uri, jwt, timeout, props => f.actorOf(props, "access-token"))))
  }

}
