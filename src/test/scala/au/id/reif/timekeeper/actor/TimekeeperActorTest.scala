package au.id.reif.timekeeper.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import au.id.reif.timekeeper.actor.TimekeeperActor.{GetTimer, Start, Stop}
import au.id.reif.timekeeper.domain.Timer
import au.id.reif.timekeeper.service.{Running, Stopped, TimerId}
import au.id.reif.timekeeper.util.TwoStepClock
import org.scalatest.FunSuiteLike

import scala.concurrent.duration._

class TimekeeperActorTest extends TestKit(ActorSystem("timekeeper-test")) with ImplicitSender with FunSuiteLike {

  implicit val timeout: Timeout = Timeout(3 seconds)
  private val stepSeconds = 3
  private def fixedStepClock(seconds: Int) = new TwoStepClock(seconds)

  test("Create running timekeeper and get elapsed time") {
    val timer = Timer(TimerId("TIMER-01"), Running, FiniteDuration(0, SECONDS))
    val timekeeper = TestActorRef(TimekeeperActor.props(timer, fixedStepClock(stepSeconds)))

    timekeeper ! GetTimer

    expectMsg(timer.copy(elapsed = FiniteDuration(stepSeconds, SECONDS)))
  }

  test("Create a running time, stop it and verify state and elapsed time") {
    val timer = Timer(TimerId("TIMER-01"), Running, FiniteDuration(1, SECONDS))
    val timekeeper = TestActorRef(TimekeeperActor.props(timer, fixedStepClock(stepSeconds)))

    timekeeper ! Stop
    timekeeper ! GetTimer

    expectMsg(timer.copy(state = Stopped, elapsed = FiniteDuration(1 + stepSeconds, SECONDS)))
  }

  test("Create stopped timer and verify zero elapsed time") {
    val timer = Timer(TimerId("TIMER-01"), Stopped, FiniteDuration(2, SECONDS))
    val timekeeper = TestActorRef(TimekeeperActor.props(timer, fixedStepClock(stepSeconds)))

    timekeeper ! GetTimer

    expectMsg(timer)
  }

  test("Create stopped timer, start it and get elapsed time") {
    val timer = Timer(TimerId("TIMER-01"), Stopped, FiniteDuration(2, SECONDS))
    val timekeeper = TestActorRef(TimekeeperActor.props(timer, fixedStepClock(stepSeconds)))

    timekeeper ! Start
    timekeeper ! GetTimer

    expectMsg(timer.copy(state = Running, elapsed = FiniteDuration(2 + stepSeconds, SECONDS)))
  }

  test("Create stopped timer, start it, stop it and get elapsed time") {
    val timer = Timer(TimerId("TIMER-01"), Stopped, FiniteDuration(2, SECONDS))
    val timekeeper = TestActorRef(TimekeeperActor.props(timer, fixedStepClock(stepSeconds)))

    timekeeper ! Start
    timekeeper ! Stop
    timekeeper ! GetTimer

    expectMsg(timer.copy(elapsed = FiniteDuration(2 + stepSeconds, SECONDS)))
  }

  test("Create stopped timer, stop it and verify it is still stopped") {
    val timer = Timer(TimerId("TIMER-01"), Stopped, FiniteDuration(2, SECONDS))
    val timekeeper = TestActorRef(TimekeeperActor.props(timer, fixedStepClock(stepSeconds)))

    timekeeper ! Stop
    timekeeper ! GetTimer

    expectMsg(timer)
  }
}
