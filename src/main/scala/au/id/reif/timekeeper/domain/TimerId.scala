package au.id.reif.timekeeper.domain

import java.util.UUID

object TimerId {
  def generate: TimerId = TimerId(UUID.randomUUID().toString)
}

case class TimerId(id: String)

