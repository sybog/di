package zhongl.di.github

import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

class GithubRoutesSpec extends WordSpec with Matchers with ScalatestRouteTest with Directives {

  val route = webhook { (event, json) =>
    complete(event)
  }

  "webhook" should {

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
