package zhongl.di.github

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.marshalling.{Marshal, ToEntityMarshaller}
import akka.http.scaladsl.model.HttpCharsets.`UTF-8`
import akka.http.scaladsl.model.HttpMethods.{DELETE, GET, POST, PUT}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.stream.Materializer
import akka.util.Timeout
import zhongl.di.rest.{AccessToken => RestAccessToken, Api => RestApi, _}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

final class Api(proxy: ActorRef)(implicit timeout: Timeout) extends RestApi {
  import akka.pattern.ask

  override def accessToken(expiresAfter: ZonedDateTime): Future[Token] = {
    (proxy ? ApiActor.GetAccessToken(expiresAfter)).mapTo[Token]
  }

  override def post[R](uri: Uri)(implicit u: FromEntityUnmarshaller[R], mat: Materializer): Future[R] =
    request(POST, uri)(u, null, mat)

  override def post[E, R](uri: Uri, payload: E)(
      implicit
      u: FromEntityUnmarshaller[R],
      m: ToEntityMarshaller[E],
      mat: Materializer
  ): Future[R] = request(POST, uri, Some(payload))

  override def put[R](uri: Uri)(implicit u: FromEntityUnmarshaller[R], mat: Materializer): Future[R] =
    request(PUT, uri)(u, null, mat)

  override def put[E, R](uri: Uri, payload: E)(
      implicit
      u: FromEntityUnmarshaller[R],
      m: ToEntityMarshaller[E],
      mat: Materializer
  ): Future[R] = request(PUT, uri, Some(payload))

  override def get[R](uri: Uri)(implicit u: FromEntityUnmarshaller[R], mat: Materializer): Future[R] =
    request(uri = uri)(u, null, mat)

  override def delete[R](uri: Uri)(implicit u: FromEntityUnmarshaller[R], mat: Materializer): Future[R] =
    request(DELETE, uri)(u, null, mat)

  private def request[E, R](method: HttpMethod = GET, uri: Uri, payload: Option[E] = None)(
      implicit
      u: FromEntityUnmarshaller[R],
      m: ToEntityMarshaller[E],
      mat: Materializer
  ): Future[R] = {
    implicit val ec = mat.executionContext
    payload
      .map(v => Marshal(v).to[RequestEntity])
      .getOrElse(Future.successful(HttpEntity.Empty))
      .map(e => HttpRequest(method, uri, entity = e))
      .flatMap(proxy ? _)
      .mapTo[HttpResponse]
      .flatMap(as[R])
  }

}

object Api {
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat}
  import DefaultJsonProtocol._

  private val accept = Accept(MediaType.customWithFixedCharset("application", "vnd.github.machine-man-preview+json", `UTF-8`))

  private final case class AccessToken(token: Token, `expires_at`: ZonedDateTime) extends RestAccessToken {
    def isOutdated(time: ZonedDateTime = ZonedDateTime.now): Boolean = `expires_at`.compareTo(time) <= 0
  }

  private implicit object ZonedDateTimeJsonFormat extends JsonFormat[ZonedDateTime] {
    override def read(json: JsValue): ZonedDateTime = ZonedDateTime.parse(json.convertTo[String])

    override def write(obj: ZonedDateTime): JsValue = JsString(obj.format(DateTimeFormatter.ISO_INSTANT))
  }

  private implicit val accessToken = jsonFormat2(AccessToken)

  def apply(accessToken: Uri, jwt: GetJWT, timeout: FiniteDuration, actorOf: Props => ActorRef): Api = {
    def headers(c: HttpCredentials) = List(Authorization(c), accept)
    val rt: RequestToken            = o => HttpRequest(POST, o.getOrElse(accessToken), headers(OAuth2BearerToken(jwt())))
    val dr: DecorateRequest         = (r, t) => r.copy(headers = headers(GenericHttpCredentials("token", t.value)))
    new Api(actorOf(ApiActor.props[AccessToken](rt, dr)))(Timeout(timeout))
  }
}
