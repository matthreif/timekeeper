package au.id.reif.timekeeper.service

import java.util.concurrent.TimeUnit.SECONDS

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration.FiniteDuration

object TimekeeperServiceTest {
  val ZeroSeconds = FiniteDuration(0, SECONDS)
}

final class TimekeeperServiceTest extends FunSuite with Matchers with ScalatestRouteTest with TimekeeperServiceJsonSupport {

  import TimekeeperServiceTest.ZeroSeconds

  test("POST with timer state should create a new timer with the correct state") {
    val service = new TimekeeperService

    var runningTimer: Option[Timer] = None
    var stoppedTimer: Option[Timer] = None

    Post("/timers", TimerStateRequest(Running)) ~> service.route ~> check {
      handled shouldBe true
      status shouldBe OK
      runningTimer = Some(responseAs[Timer])
      runningTimer.get should have (
        'state (Running),
        'elapsed (ZeroSeconds)
      )
    }

    Post("/timers", TimerStateRequest(Stopped)) ~> service.route ~> check {
      handled shouldBe true
      status shouldBe OK
      stoppedTimer = Some(responseAs[Timer])
      stoppedTimer.get should have (
        'state (Stopped),
        'elapsed (ZeroSeconds)
      )
    }

    Get("/timers") ~> service.route ~> check {
      handled shouldBe true
      status shouldBe OK
      responseAs[Set[Timer]] shouldBe Set(runningTimer.get, stoppedTimer.get)
    }

  }

  test("Getting an existing timer should retrieve the correct timer") {
    val service = new TimekeeperService

    var timer: Option[Timer] = None

    Post("/timers", TimerStateRequest(Stopped)) ~> service.route ~> check {
      handled shouldBe true
      status shouldBe OK
      timer = Some(responseAs[Timer])
      timer.get should have (
        'state (Stopped),
        'elapsed (ZeroSeconds)
      )
    }

    Get(s"/timers/${timer.get.id.id}") ~> service.route ~> check {
      handled shouldBe true
      status shouldBe OK
      responseAs[Timer] should have (
        'id (timer.get.id),
        'state (Stopped),
        'elapsed (ZeroSeconds)
      )
    }
  }

  test("Getting an non-existent timer should return 404 Not Found") {
    val service = new TimekeeperService

    Get("/timers/non-existent") ~> Route.seal(service.route) ~> check {
      handled shouldBe true
      status shouldBe NotFound
    }
  }
}
