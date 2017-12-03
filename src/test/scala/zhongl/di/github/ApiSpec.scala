package zhongl.di.github

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_INSTANT
import java.time.temporal.ChronoUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.testkit.{ImplicitSender, TestActor, TestKit, TestProbe}
import com.github.dreamhead.moco.{Moco, Runnable, Runner}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import spray.json._
import zhongl.di.rest.Token

import scala.concurrent.duration._

class ApiSpec
    extends TestKit(ActorSystem("RestApiV3Spec"))
    with WordSpecLike
    with Matchers
    with ImplicitSender
    with DefaultJsonProtocol
    with SprayJsonSupport
    with BeforeAndAfterAll {

  import akka.pattern.pipe
  import system.dispatcher

  implicit val mat    = ActorMaterializer()
  implicit val result = jsonFormat1(Result)

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Api" should {

    import Moco._

    def authorization(value: String) = Moco.eq(header("Authorization"), value)

    val accept      = Moco.eq(header("Accept"), "application/vnd.github.machine-man-preview+json")
    val json        = header("Content-Type", "application/json")
    val datetime    = ZonedDateTime.now.plus(1, ChronoUnit.HOURS).format(ISO_INSTANT)
    val accessToken = s"""{ "token": "v1.1f699f1069f60xxx", "expires_at": "${datetime}" }"""

    "get access token" in {
      val server = httpServer(log())

      server.request(and(by(uri("/at")), authorization("Bearer JWT"), accept)).response(json, `with`(accessToken))

      val run = new Runnable {
        override def run(): Unit = {
          Api(s"http://localhost:${server.port}/at", () => "JWT", 1.second, p => system.actorOf(p)).accessToken().pipeTo(self)
          expectMsgPF(1.second) {
            case Token("v1.1f699f1069f60xxx") =>
          }
        }
      }

      Runner.running(server, run)
    }

    "get result" in {
      val server = httpServer(log())

      server.request(and(by(uri("/at")), authorization("Bearer JWT"), accept)).response(json, `with`(accessToken))
      server.request(and(by(uri("/result")), authorization("token v1.1f699f1069f60xxx"), accept)).response(json, `with`("""{"value": "got"}"""))

      val run = new Runnable {
        override def run(): Unit = {
          val port = server.port
          Api(s"http://localhost:${port}/at", () => "JWT", 1.second, p => system.actorOf(p))
            .get[Result](s"http://localhost:${port}/result")
            .pipeTo(self)

          expectMsgPF(1.second) {
            case Result("got") =>
          }
        }
      }

      Runner.running(server, run)
    }

    "post and receive the result" in {
      val probe = {
        val p    = TestProbe()
        p.setAutoPilot(new TestActor.AutoPilot {
          override def run(sender: ActorRef, msg: Any): TestActor.AutoPilot = msg match {
            case r: HttpRequest => sender ! HttpResponse(entity = r.entity); TestActor.NoAutoPilot

          }
        })
        p
      }

      Api("http://token", () => "", 1.second, _ => probe.ref).post[Result, Result]("http://post", Result("post")).pipeTo(self)
      expectMsgPF(1.second) {
        case Result("post") =>
      }
    }

  }
}

final case class Result(value: String)
