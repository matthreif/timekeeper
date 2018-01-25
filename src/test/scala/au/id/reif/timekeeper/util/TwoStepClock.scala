package au.id.reif.timekeeper.util

import java.time.{Clock, Instant, ZoneId}

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