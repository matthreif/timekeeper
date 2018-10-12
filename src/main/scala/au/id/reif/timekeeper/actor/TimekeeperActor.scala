package au.id.reif.timekeeper.actor

import java.time.Clock
import java.util.concurrent.TimeUnit.SECONDS

import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}
import akka.event.LoggingReceive
import akka.pattern.pipe
import akka.pattern.ask
import akka.util.Timeout
import au.id.reif.timekeeper.domain.Timer
import au.id.reif.timekeeper.domain.{Running, Stopped}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object TimekeeperActor {
  def props(timer: Timer, clock: Clock): Props = Props(TimekeeperActor(timer, defaultStopWatchFactory(clock)))
  def props(timer: Timer, stopWatchFactory: ActorRefFactory => ActorRef) = Props(TimekeeperActor(timer, stopWatchFactory))

  implicit val timeout: Timeout = Timeout(3, SECONDS)

  def defaultStopWatchFactory(clock: Clock): ActorRefFactory => ActorRef = (f: ActorRefFactory) => f.actorOf(StopWatchActor.props(clock))

  case object Start
  case object Stop
  case object GetTimer
}

case class TimekeeperActor(initialTimer: Timer, stopWatchFactory: ActorRefFactory => ActorRef) extends Actor /* with Stash */ {
  import TimekeeperActor._

  implicit val ec: ExecutionContext = context.dispatcher

  private var timer: Timer = initialTimer

  private var maybeStopWatch: Option[ActorRef] =
    if (initialTimer.state == Running)
      Some(stopWatchFactory(context))
    else
      None

  override def receive: Receive = LoggingReceive {
    case GetTimer =>
      maybeStopWatch.fold(sender ! timer) { stopWatch =>
        stopWatch.ask(GetElapsed)
          .mapTo[FiniteDuration]
          .map(elapsed => timer.copy(elapsed = timer.elapsed.plus(elapsed))
          ) pipeTo sender
      }
    case Start =>
      maybeStopWatch = Some(stopWatchFactory(context))
      timer = timer.copy(state = Running)
      sender ! timer
    case Stop =>
      maybeStopWatch.fold() { stopWatch =>
        stopWatch.ask(GetElapsed)
          .mapTo[FiniteDuration]
          .map { elapsed => {
            context.stop(stopWatch)
            timer = timer.copy(state = Stopped, elapsed = timer.elapsed.plus(elapsed))
            maybeStopWatch = None
            sender ! timer
          }}
      }
  }
}
