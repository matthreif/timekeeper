package au.id.reif.timekeeper.actor

import java.time.Clock
import java.util.concurrent.TimeUnit.SECONDS

import akka.actor.{Actor, Props}

import scala.concurrent.duration.FiniteDuration

case object GetElapsed

object StopWatchActor {
  def props(clock: Clock): Props = Props(StopWatchActor(clock))
}

case class StopWatchActor(clock: Clock) extends Actor {
  private val startSeconds = clock.instant().getEpochSecond
  override def receive: Receive = {
    case GetElapsed =>
      println(s"${clock.instant().getEpochSecond}")
      println(s"$startSeconds")
      val currentSeconds = clock.instant().getEpochSecond
      sender ! FiniteDuration(currentSeconds - startSeconds, SECONDS)
  }
}
