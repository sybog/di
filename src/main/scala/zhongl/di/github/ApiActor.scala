package zhongl.di.github

import akka.actor.Actor
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpCharsets.`UTF-8`
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers.{ Accept, Authorization, OAuth2BearerToken }
import akka.http.scaladsl.model.{ HttpRequest, MediaType, Uri }
import akka.http.scaladsl.unmarshalling._
import akka.stream.ActorMaterializer
import zhongl.di.github.ApiActor._

class ApiActor(base: Uri) extends Actor with PredefinedFromEntityUnmarshallers {

  import akka.pattern.pipe
  import context.dispatcher

  private val http = Http(context.system)

  private implicit val materializer: ActorMaterializer = ActorMaterializer()(context)

  override def receive = {
    case m: Message => http.singleRequest(m.asRequestWith(base)).map { _ => ??? }.pipeTo(sender())
  }
}

object ApiActor {

  sealed trait Message {
    def asRequestWith(base: Uri): HttpRequest
  }

  final case class RequestAccessToken(installation: String, credential: String)
    extends Message {
    def asRequestWith(base: Uri): HttpRequest = {
      HttpRequest(
        POST, base.withPath(Path / "installation" / s"$installation" / "access_tokens"),
        List(
          Authorization(OAuth2BearerToken(credential)),
          Accept(`application/vnd.github.machine-main-preview+json`)))
    }
  }

  private val `application/vnd.github.machine-main-preview+json` =
    MediaType.customWithFixedCharset("application", "vnd.github.machine-man-preview+json", `UTF-8`)

}

