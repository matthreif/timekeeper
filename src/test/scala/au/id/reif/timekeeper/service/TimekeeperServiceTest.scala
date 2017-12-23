package au.id.reif.timekeeper.service

import java.util.concurrent.TimeUnit.SECONDS

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration.FiniteDuration

final class TimekeeperServiceTest extends FunSuite with Matchers with ScalatestRouteTest with TimekeeperServiceJsonSupport {

  test("POST with timer state should report the correct state") {
    val service = new TimekeeperService
    val zeroSeconds = FiniteDuration(0, SECONDS)

    Post("/timers", TimerStateRequest(Running)) ~> service.route ~> check {
      handled shouldBe true
      responseAs[Timer] should have (
        'state (Running),
        'elapsed (zeroSeconds)
      )
    }

    Post("/timers", TimerStateRequest(Stopped)) ~> service.route ~> check {
      handled shouldBe true
      responseAs[Timer] should have (
        'state (Stopped),
        'elapsed (zeroSeconds)
      )
    }
  }
}
