package au.id.reif.timekeeper.domain

trait TimerState {
  val json: String
}
case object Running extends TimerState { val json = "running"}
case object Stopped extends TimerState { val json = "stopped"}

