package zhongl.di.rest

import java.time.ZonedDateTime

import akka.actor.{ActorSystem, Status}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse, StatusCodes}
import akka.stream.{ActorMaterializer, StreamTcpException}
import akka.testkit.{ImplicitSender, TestKit}
import com.github.dreamhead.moco.Moco._
import com.github.dreamhead.moco.Runner.running
import com.github.dreamhead.moco.{Moco, Runnable}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import spray.json.DefaultJsonProtocol
import zhongl.di.rest.ApiActor.GetAccessToken

import scala.concurrent.duration._

class ApiActorSpec
    extends TestKit(ActorSystem("AccessTokenSpec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender
    with DefaultJsonProtocol
    with SprayJsonSupport {

  implicit val mat = ActorMaterializer()
  implicit val at  = jsonFormat1(TestAccessToken)

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "ApiActor" should {

    "send back the token after refresh" in {
      val server = httpServer(log())
      val json   = header("Content-Type", "application/json")

      server.request(by(uri("/at"))).response(json, `with`("""{"token": "v1.1f699f1069f60xxx"}"""))

      val run = new Runnable {

        override def run(): Unit = {
          val port                = server.port
          val rt: RequestToken    = _ => HttpRequest(uri = s"http://localhost:${port}/at")
          val dr: DecorateRequest = (r, _) => r

          system.actorOf(ApiActor.props[TestAccessToken](rt, dr)) ! GetAccessToken(ZonedDateTime.now)

          expectMsgPF(1.seconds) {
            case Token("v1.1f699f1069f60xxx") =>
          }
        }
      }

      running(server, run)
    }

    "send back http response" in {
      val server = httpServer(log())
      val json   = header("Content-Type", "application/json")

      server.request(by(uri("/at"))).response(json, `with`("""{"token": "v1.1f699f1069f60xxx"}"""))
      server.request(and(by(uri("/array")), Moco.eq(header("Authorization"), "Bearer v1.1f699f1069f60xxx"))).response(status(200))

      val run = new Runnable {

        override def run(): Unit = {
          val port                = server.port
          val rt: RequestToken    = _ => HttpRequest(uri = s"http://localhost:${port}/at")
          val dr: DecorateRequest = (r, t) => r.copy(headers = Authorization(OAuth2BearerToken(t.value)) :: Nil)

          system.actorOf(ApiActor.props[TestAccessToken](rt, dr)) ! HttpRequest(uri = s"http://localhost:${port}/array")

          expectMsgPF(1.seconds) {
            case HttpResponse(StatusCodes.OK, Nil, HttpEntity.Empty, _) =>
          }
        }
      }

      running(server, run)
    }

    "redirect automatically" in {
      val port   = 20001
      val server = httpServer(port, log())
      val json   = header("Content-Type", "application/json")

      server.request(by(uri("/at"))).response(status(302), header("Location", s"http://localhost:${port}/nat"))
      server.request(by(uri("/nat"))).response(json, `with`("""{"token": "v1.1f699f1069f60xxx"}"""))

      val run = new Runnable {

        override def run(): Unit = {
          val rt: RequestToken    = o => HttpRequest(uri = { o.getOrElse(s"http://localhost:${port}/at") })
          val dr: DecorateRequest = (r, _) => r

          system.actorOf(ApiActor.props[TestAccessToken](rt, dr)) ! GetAccessToken(ZonedDateTime.now)

          expectMsgPF(1.seconds) {
            case Token("v1.1f699f1069f60xxx") =>
          }
        }
      }

      running(server, run)
    }

    "send back failure status" in {
      val rt: RequestToken    = o => HttpRequest(uri = { o.getOrElse(s"http://localhost:10086/at") })
      val dr: DecorateRequest = (r, _) => r

      system.actorOf(ApiActor.props[TestAccessToken](rt, dr)) ! GetAccessToken(ZonedDateTime.now)

      expectMsgPF(1.seconds) {
        case Status.Failure(_: StreamTcpException) =>
      }

    }

  }

}

final case class TestAccessToken(token: Token) extends AccessToken {
  override def isOutdated(expiration: ZonedDateTime): Boolean = false
}
