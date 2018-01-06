package au.id.reif.timekeeper.actor

import java.time.{Clock, Instant, ZoneId}
import java.util.concurrent.TimeUnit.SECONDS

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.FunSuiteLike

import scala.concurrent.duration.FiniteDuration

class TwoStepClock(seconds: Long) extends Clock {
  private var counter = 0

  override def withZone(zone: ZoneId): Clock = ???
  override def getZone: ZoneId = ???
  override def instant(): Instant = {
    val result =
      if (counter == 0)
        Instant.ofEpochSecond(0)
      else
        Instant.ofEpochSecond(seconds)
      counter = counter + 1
    result
  }
}

class StopWatchActorTest extends TestKit(ActorSystem("stop-watch-test")) with ImplicitSender with FunSuiteLike {

  private def seconds(seconds: Long) = FiniteDuration(seconds, SECONDS)

  implicit val timeout: Timeout = Timeout(seconds(3))
  private val fixedInstant = Instant.ofEpochMilli(1234)
  private val fixedClock = Clock.fixed(fixedInstant, ZoneId.systemDefault())
  private val stepSeconds = 3
  private val fixedStepClock = new TwoStepClock(stepSeconds)

  test("Stop watch should return zero durations when a fixed clock is used") {

    val stopWatch = system.actorOf(StopWatchActor.props(fixedClock))

    stopWatch ! GetElapsed

    expectMsg(seconds(0))
  }

  test("Stop watch should return a fixed duration when a fixed step clock is used") {

    val stopWatch = system.actorOf(StopWatchActor.props(fixedStepClock))

    stopWatch ! GetElapsed

    expectMsg(seconds(stepSeconds))
  }
}
