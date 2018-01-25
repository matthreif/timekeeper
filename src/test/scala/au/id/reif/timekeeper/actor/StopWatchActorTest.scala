package au.id.reif.timekeeper.actor

import java.time.{Clock, Instant, ZoneId}
import java.util.concurrent.TimeUnit.SECONDS

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import au.id.reif.timekeeper.util.TwoStepClock
import org.scalatest.FunSuiteLike

import scala.concurrent.duration.FiniteDuration

class StopWatchActorTest extends TestKit(ActorSystem("stop-watch-test")) with ImplicitSender with FunSuiteLike {

  private def seconds(seconds: Long) = FiniteDuration(seconds, SECONDS)

  implicit val timeout: Timeout = Timeout(seconds(3))
  private val fixedInstant = Instant.ofEpochMilli(1234)
  private def fixedClock(instant: Instant) = Clock.fixed(fixedInstant, ZoneId.systemDefault())
  private val stepSeconds = 3
  private def fixedStepClock(seconds: Int) = new TwoStepClock(seconds)

  test("Stop watch should return zero duration when a fixed clock is used") {

    val stopWatch = TestActorRef(StopWatchActor.props(fixedClock(fixedInstant)))

    stopWatch ! GetElapsed

    expectMsg(seconds(0))
  }

  test("Stop watch should return a fixed duration when a fixed step clock is used") {

    val stopWatch = TestActorRef(StopWatchActor.props(fixedStepClock(stepSeconds)))

    stopWatch ! GetElapsed

    expectMsg(seconds(stepSeconds))
  }
}
