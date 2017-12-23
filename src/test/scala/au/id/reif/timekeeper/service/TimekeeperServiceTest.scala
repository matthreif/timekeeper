package au.id.reif.timekeeper.service

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{FunSuite, Matchers}

final class TimekeeperServiceTest extends FunSuite with Matchers with ScalatestRouteTest with TimekeeperServiceJsonSupport {

  test("POST with timer state should report the correct state") {
    val service = new TimekeeperService

    Post("/timers", TimerStateRequest(Running)) ~> service.route ~> check {
      handled shouldBe true
      responseAs[String] shouldBe "Create timer with state running"
    }

    Post("/timers", TimerStateRequest(Stopped)) ~> service.route ~> check {
      handled shouldBe true
      responseAs[String] shouldBe "Create timer with state stopped"
    }
  }
}
