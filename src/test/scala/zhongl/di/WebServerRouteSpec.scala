package zhongl.di

import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.util.FastFuture
import org.scalatest.{Matchers, WordSpec}

import scala.util.Success

class WebServerRouteSpec extends WordSpec with Matchers with ScalatestRouteTest with Directives {

  val route = WebServer.route((_, _) => FastFuture.apply(Success("")))

  "Webhook" should {

    "leave requests without X-GitHub-Event header unhandled" in {
      Post("/webhook") ~> route ~> check {
        handled shouldBe false
      }
    }

    "response ok" in {
      val entity = HttpEntity(`application/json`, """{"key":"value"}""")
      Post("/webhook", entity) ~> RawHeader("X-GitHub-Event", "pull_request") ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }
}
