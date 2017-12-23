package au.id.reif.timekeeper.service

import akka.http.scaladsl.server.{Directive0, Route}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

trait TimerState {
  val json: String
}
case object Running extends TimerState { val json = "running"}
case object Stopped extends TimerState { val json = "stopped"}

case class TimerStateRequest(state: TimerState)

case class TimerId(id: String)

case class Timer(id: TimerId, state: TimerState)

// collect your json format instances into a support trait:


trait TimekeeperServiceJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit object timerStateFormat extends JsonFormat[TimerState] {
    override def read(json: JsValue): TimerState = json match {
      case JsString(Stopped.json) => Stopped
      case JsString(Running.json) => Running
      case unknown => throw DeserializationException(s"Failed to deserialise $unknown")
    }

    override def write(obj: TimerState): JsValue = JsString(obj.json)
  }

  implicit val timerStateRequestFormat: RootJsonFormat[TimerStateRequest] = jsonFormat1(TimerStateRequest.apply)
}

final class TimekeeperService extends TimekeeperServiceJsonSupport {

  def route: Route = getTimerRoute ~ deleteTimerRoute() ~ updateTimerRoute() ~ createTimerRoute

  def pathTimers: Directive0 = path("timers")

  // API

  // Create new timer in running state
  // POST /timers { "state": "running" }
  // Create new timer in stopped state
  // POST /timers { "state": "stopped" }
  def createTimerRoute: Route = post {
    pathTimers {
      entity(as[TimerStateRequest]) { request =>
        complete(s"Create timer with state ${request.state.json}" )
      }
    }
  }

  // Get timer
  // GET /timers/{id}
  // {
  //   "id": "...",
  //   "state": ".."
  // }
  def getTimerRoute: Route = get {
    pathTimers {
      complete("Get timer")
    }
  }

  // Start timer
  // PUT /timers/{id} { "state": "running" }
  // Stop timer
  // PUT /timers/{id} { "state": "stopped" }
  def updateTimerRoute(): Route = put {
    pathTimers {
      complete("Update timer")
    }
  }

  // Delete timer
  // DELETE /timers/{id}
  def deleteTimerRoute(): Route = delete {
    pathTimers {
      complete("Delete timer")
    }
  }
}
