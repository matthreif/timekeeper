package au.id.reif.timekeeper.service

import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS

import akka.http.scaladsl.server.{Directive0, Route}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

trait TimerState {
  val json: String
}
case object Running extends TimerState { val json = "running"}
case object Stopped extends TimerState { val json = "stopped"}

case class TimerStateRequest(state: TimerState)

object TimerId {
  def generate: TimerId = TimerId(UUID.randomUUID().toString)
}

case class TimerId(id: String)

case class Timer(id: TimerId, state: TimerState, elapsed: FiniteDuration)

// collect your json format instances into a support trait:
trait TimekeeperServiceJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val timerIdFormat: JsonFormat[TimerId] = jsonFormat1(TimerId.apply)
  implicit object timerStateFormat extends JsonFormat[TimerState] {
    override def read(json: JsValue): TimerState = json match {
      case JsString(Stopped.json) => Stopped
      case JsString(Running.json) => Running
      case unknown => deserializationError(s"Failed to deserialise $unknown")
    }

    override def write(obj: TimerState): JsValue = JsString(obj.json)
  }

  implicit object FiniteDurationJsonFormat extends RootJsonFormat[FiniteDuration]{
    def write(dur: FiniteDuration) = JsObject(
      "length" -> JsNumber(dur.length),
      "unit" -> JsString(dur.unit.toString)
    )
    def read(value: JsValue) = {
      value.asJsObject.getFields("length", "unit") match {
        case Seq(JsNumber(length), JsString(unit)) => FiniteDuration(length.toLong, unit.toLowerCase)
        case _ => deserializationError("FiniteDuration expected")
      }
    }
  }
  implicit val timerStateRequestFormat: RootJsonFormat[TimerStateRequest] = jsonFormat1(TimerStateRequest.apply)
  implicit val timerFormat: RootJsonFormat[Timer] = jsonFormat3(Timer.apply)
}

object TimekeeperService {
  final val BasePath = "timers"
}
final class TimekeeperService extends TimekeeperServiceJsonSupport {
  import TimekeeperService._

  val database: mutable.Map[TimerId, Timer] = mutable.Map.empty

  def route: Route = getAllTimersRoute ~ getOneTimerRoute ~ deleteTimerRoute() ~ updateTimerRoute() ~ createTimerRoute

  def pathTimers: Directive0 = path(BasePath)

  def handleCreateTimer(request: TimerStateRequest): Timer = {
    val timer = Timer(TimerId.generate, request.state, FiniteDuration.apply(0, SECONDS))
    database += timer.id -> timer
    timer
  }

  def handleGetAllTimers(): Set[Timer] = database.values.toSet

  def handleGetOneTimer(id: TimerId): Option[Timer] = database.get(id)

  def handleUpdateTimer(id: TimerId, request: TimerStateRequest): Option[Timer] =
    database.get(id).fold[Option[Timer]](None)(timer => (database += id -> timer.copy(state = request.state)).get(id))

  // API

  // Create new timer in running state
  // POST /timers { "state": "running" }
  // Create new timer in stopped state
  // POST /timers { "state": "stopped" }
  def createTimerRoute: Route = post {
    pathTimers {
      entity(as[TimerStateRequest]) { request =>
        complete(handleCreateTimer(request))
      }
    }
  }

  // Get timer
  // GET /timers/{id}
  // {
  //   "id": "...",
  //   "state": ".."
  // }
  def getAllTimersRoute: Route = get {
    pathTimers {
      complete(handleGetAllTimers())
    }
  }

  def getOneTimerRoute: Route =
    get {
      rejectEmptyResponse {
        path(BasePath / Segment) { id =>
          complete(handleGetOneTimer(TimerId(id)))
        }
      }
    }

  // Start timer
  // PUT /timers/{id} { "state": "running" }
  // Stop timer
  // PUT /timers/{id} { "state": "stopped" }
  def updateTimerRoute(): Route =
    put {
      rejectEmptyResponse {
        path(BasePath / Segment) { id =>
          entity(as[TimerStateRequest]) { request =>
            complete(handleUpdateTimer(TimerId(id), request))
          }
        }
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
