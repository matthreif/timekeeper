package au.id.reif.timekeeper.actor

import java.time.Clock
import java.util.concurrent.TimeUnit.SECONDS

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.pattern.pipe
import akka.pattern.ask
import akka.util.Timeout
import au.id.reif.timekeeper.domain.Timer
import au.id.reif.timekeeper.service.{Running, Stopped}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object TimekeeperActor {
  def props(timer: Timer, clock: Clock): Props = Props(TimekeeperActor(timer, clock))

  implicit val timeout: Timeout = Timeout(3, SECONDS)

  case object Start
  case object Stop
  case object GetTimer
}

case class TimekeeperActor(initialTimer: Timer, clock: Clock) extends Actor {
  import TimekeeperActor._

  implicit val ec: ExecutionContext = context.dispatcher

  private var timer: Timer = initialTimer

  private var maybeStopWatch: Option[ActorRef] =
    if (initialTimer.state == Running)
      Some(context.actorOf(StopWatchActor.props(clock)))
    else
      None

  override def receive: Receive = {
    case GetTimer =>
      maybeStopWatch.fold(sender ! timer) { stopWatch =>
        stopWatch.ask(GetElapsed)
          .mapTo[FiniteDuration]
          .map(elapsed => timer.copy(elapsed = timer.elapsed.plus(elapsed))
          ) pipeTo sender
      }
    case Start =>
      maybeStopWatch = Some(context.actorOf(StopWatchActor.props(clock)))
      timer = timer.copy(state = Running)
    case Stop =>
      maybeStopWatch.fold() { stopWatch =>
        stopWatch.ask(GetElapsed)
          .mapTo[FiniteDuration]
          .map { elapsed => {
            stopWatch ! PoisonPill
            timer = timer.copy(state = Stopped, elapsed = timer.elapsed.plus(elapsed))
            maybeStopWatch = None
          }}
      }
  }
}
