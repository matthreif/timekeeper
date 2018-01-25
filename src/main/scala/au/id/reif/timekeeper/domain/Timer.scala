package au.id.reif.timekeeper.domain

import au.id.reif.timekeeper.service.{TimerId, TimerState}

import scala.concurrent.duration.FiniteDuration

case class Timer(id: TimerId, state: TimerState, elapsed: FiniteDuration)
