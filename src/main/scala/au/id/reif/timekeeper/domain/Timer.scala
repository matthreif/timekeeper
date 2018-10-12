package au.id.reif.timekeeper.domain

import scala.concurrent.duration.FiniteDuration

case class Timer(id: TimerId, state: TimerState, elapsed: FiniteDuration)
