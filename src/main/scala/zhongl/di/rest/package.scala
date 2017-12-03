package zhongl.di

import akka.http.scaladsl.model.StatusCodes.{ClientError, ServerError}
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshal}
import akka.stream.Materializer
import spray.json.{JsString, JsValue, JsonFormat}
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future
import scala.util.control.NoStackTrace

package object rest {

  final case class Token(value: String) extends AnyVal

  type RequestToken    = Option[Uri] => HttpRequest
  type DecorateRequest = (HttpRequest, Token) => HttpRequest

  final class RedirectionException(val e: Either[String, Uri]) extends RuntimeException(e.map(_.toString).merge) with NoStackTrace
  final class BadRequestException(message: String)             extends RuntimeException(message) with NoStackTrace
  final class ServerInternalException(message: String)         extends RuntimeException(message) with NoStackTrace
  final class UnexpectedResponseException(message: String)     extends RuntimeException(message) with NoStackTrace

  def as[R](implicit u: FromEntityUnmarshaller[R], mat: Materializer): PartialFunction[HttpResponse, Future[R]] = {
    case HttpResponse(_: StatusCodes.Success, _, entity, _)    => Unmarshal(entity).to[R]
    case r @ HttpResponse(_: StatusCodes.Redirection, _, _, _) => redirect(r)
    case r @ HttpResponse(_: ClientError, _, _, _)             => Future.failed(new BadRequestException(r.toString))
    case r @ HttpResponse(_: ServerError, _, _, _)             => Future.failed(new ServerInternalException(r.toString))
    case r                                                     => Future.failed(new UnexpectedResponseException(r.toString))
  }

  private def redirect[R](r: HttpResponse): Future[R] = {
    Future.failed(new RedirectionException(r.headers.collectFirst { case Location(uri) => uri }.toRight(r.toString)))
  }

  implicit object TokenJsonFormat extends JsonFormat[Token] {
    override def write(obj: Token): JsValue = JsString(obj.value)

    override def read(json: JsValue): Token = Token(json.convertTo[String])
  }

}
