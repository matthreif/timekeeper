package au.id.reif.timekeeper.service

import java.time.Clock
import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.http.scaladsl.server.{Directive0, Directive1, Route}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.util.Timeout
import au.id.reif.timekeeper.actor.TimekeeperActor
import au.id.reif.timekeeper.actor.TimekeeperActor.{GetTimer, Start, Stop}
import au.id.reif.timekeeper.domain.{Running, Stopped, Timer, TimerId, TimerState}
import spray.json._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

// collect your json format instances into a support trait:
trait TimekeeperServiceJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
//  implicit val timerIdFormat: JsonFormat[TimerId] = jsonFormat1(TimerId.apply)
  implicit object timerIdFormat extends JsonFormat[TimerId] {
    override def read(json: JsValue): TimerId = TimerId(json.convertTo[String])
    override def write(obj: TimerId): JsValue = JsString(obj.id)
  }
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
    def read(value: JsValue): FiniteDuration = {
      value.asJsObject.getFields("length", "unit") match {
        case Seq(JsNumber(length), JsString(unit)) => FiniteDuration(length.toLong, unit.toLowerCase)
        case _ => deserializationError("FiniteDuration expected")
      }
    }
  }
  implicit val timerStateRequestFormat: RootJsonFormat[TimerStateRequest] = jsonFormat1(TimerStateRequest.apply)
  implicit val timerFormat: RootJsonFormat[Timer] = jsonFormat3(Timer.apply)
}

case class TimerStateRequest(state: TimerState)

object TimekeeperService {
  final val BasePath = "timers"
}
final class TimekeeperService(system: ActorSystem, clock: Clock) extends TimekeeperServiceJsonSupport {
  import TimekeeperService._

  implicit val timeout: Timeout = Timeout(3 seconds)
  implicit val ec: ExecutionContext = system.dispatcher

  val database: mutable.Map[TimerId, ActorRef] = mutable.Map.empty

  def route: Route = getAllTimersRoute ~ getOneTimerRoute ~ deleteTimerRoute() ~ updateTimerRoute() ~ createTimerRoute

  def pathTimers: Directive0 = path(BasePath)
  def pathTimersWithId: Directive1[String] = path(BasePath / Segment)

  def handleCreateTimer(request: TimerStateRequest): Timer = {
    val timer = Timer(TimerId.generate, request.state, FiniteDuration.apply(0, SECONDS))
    database += timer.id -> system.actorOf(TimekeeperActor.props(timer, clock))
    timer
  }

  def handleGetAllTimers(): Future[Set[Timer]] =
    Future.sequence {
      database.values.toSet.map { timekeeper: ActorRef =>
        timekeeper.ask(GetTimer)
          .mapTo[Timer]
      }
    }

  def handleGetOneTimer(id: TimerId): Future[Option[Timer]] = {
    System.out.println(s"Database lookup for: $id -> ${database.get(id)}")
    database.get(id).fold[Future[Option[Timer]]](Future.successful(None)) { timekeeper: ActorRef =>
      timekeeper.ask(GetTimer)
        .mapTo[Timer]
        .map(Some(_))
    }
  }

  def handleUpdateTimer(id: TimerId, request: TimerStateRequest): Future[Option[Timer]] =
    database.get(id).fold[Future[Option[Timer]]](Future.successful(None)) { timekeeper: ActorRef =>
      timekeeper.ask(GetTimer)
        .mapTo[Timer]
        .map { timer =>
          (timer.state, request.state) match {
            case (Running, Stopped) =>
              timekeeper ! Stop
              Some(timer.copy(state = Stopped))
            case (Stopped, Running) =>
              timekeeper ! Start
              Some(timer.copy(state = Running))
            case _ =>
              Some(timer)
          }
        }
    }

  def handleDeleteTimer(id: TimerId): Future[Option[Timer]] =
    database.get(id).fold[Future[Option[Timer]]](Future.successful(None)) { timekeeper =>
      timekeeper.ask(GetTimer)
        .mapTo[Timer]
        .map { timer =>
          database -= id
          Some(timer)
        }
    }

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
      onSuccess(handleGetAllTimers())(complete(_))
    }
  }

  def getOneTimerRoute: Route =
    get {
      rejectEmptyResponse {
        pathTimersWithId { id =>
          onSuccess(handleGetOneTimer(TimerId(id)))(complete(_))
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
        pathTimersWithId { id =>
          entity(as[TimerStateRequest]) { request =>
            onSuccess(handleUpdateTimer(TimerId(id), request))(complete(_))
          }
        }
      }
  }

  // Delete timer
  // DELETE /timers/{id}
  def deleteTimerRoute(): Route =
    delete {
      rejectEmptyResponse {
        pathTimersWithId { id =>
          onSuccess(handleDeleteTimer(TimerId(id)))(complete(_))
        }
      }
  }
}
