package au.id.reif.timekeeper

import java.time.Clock

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import au.id.reif.timekeeper.service.TimekeeperService

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object Main extends App {
  override def main(args: Array[String]) {

    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher
    val service = new TimekeeperService(system, Clock.systemDefaultZone())

    val bindingFuture = Http().bindAndHandle(service.route, "0.0.0.0", 8080)

    println(s"Server online at http://0.0.0.0:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return

    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
