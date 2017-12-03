package zhongl.di.rest

import java.time.ZonedDateTime

import akka.actor.Status.Status
import akka.actor.{Actor, Props, Stash, Status}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.stream.ActorMaterializer

class ApiActor[T <: AccessToken](rt: RequestToken, dr: DecorateRequest)(implicit u: FromEntityUnmarshaller[T]) extends Actor with Stash {

  import ApiActor._
  import akka.pattern.pipe
  import context.dispatcher

  val http                = Http(context.system)
  var cached: AccessToken = AccessToken.Outdated

  implicit val mat = ActorMaterializer()(context)

  override def receive: Receive = autoRefresh {
    case GetAccessToken(ex) => if (cached.isOutdated(ex)) refresh() else sender() ! cached.token
    case r: HttpRequest     => http.singleRequest(dr(r, cached.token)).pipeTo(sender())
  }

  private def autoRefresh(receive: Receive): Receive = {
    if (cached.isOutdated()) { case _ => refresh() } else receive
  }

  private def receiveAccessToken: Receive = {
    case at: AccessToken                                        => cached = at; unstashAll(); context.become(receive)
    case Status.Failure(r: RedirectionException) if r.e.isRight => request(r.e.toOption)
    case s: Status                                              => unstashAll(); context.become(report(s))
    case _: HttpRequest | GetAccessToken                        => stash()
  }

  private def refresh(): Unit = {
    stash()
    request()
    context.become(receiveAccessToken)
  }

  private def request(maybeUri: Option[Uri] = None) = {
    http.singleRequest(rt(maybeUri)).flatMap(as[T]).pipeTo(self)
  }

  private def report(status: Status.Status): Receive = {
    case _ => sender() ! status; context.become(receive)
  }
}

object ApiActor {
  final case class GetAccessToken(expiresAfter: ZonedDateTime)

  def props[T <: AccessToken](rt: RequestToken, dr: DecorateRequest)(implicit u: FromEntityUnmarshaller[T]) =
    Props(new ApiActor[T](rt, dr))
}
