package zhongl.di.rest

import java.time.ZonedDateTime

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.stream.Materializer

import scala.concurrent.Future

trait Api {

  def accessToken(expiresAfter: ZonedDateTime = ZonedDateTime.now): Future[Token]

  def post[R](uri: Uri)(implicit u: FromEntityUnmarshaller[R], mat: Materializer): Future[R]

  def post[E, R](uri: Uri, payload: E)(
      implicit
      u: FromEntityUnmarshaller[R],
      m: ToEntityMarshaller[E],
      mat: Materializer
  ): Future[R]

  def put[R](uri: Uri)(implicit u: FromEntityUnmarshaller[R], mat: Materializer): Future[R]

  def put[E, R](uri: Uri, payload: E)(
      implicit
      u: FromEntityUnmarshaller[R],
      m: ToEntityMarshaller[E],
      mat: Materializer
  ): Future[R]

  def get[R](uri: Uri)(implicit u: FromEntityUnmarshaller[R], mat: Materializer): Future[R]

  def delete[R](uri: Uri)(implicit u: FromEntityUnmarshaller[R], mat: Materializer): Future[R]

}
