package nl.lpdiy.incubator.server

import akka.actor.{ActorLogging, Props}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import nl.lpdiy.pishake.util.{ActorDescription, ActorExecutionContextProvider}
import spray.http.StatusCodes._
import spray.http.{HttpRequest, HttpResponse, Timedout}
import spray.routing._

import scala.concurrent.duration._

object HttpServerActor extends ActorDescription {

  lazy val timeout = Timeout(ConfigFactory.load().getInt("incubator.server.response-timeout").seconds)

  def props(args: Any*): Props = Props[HttpServerActor]
}

class HttpServerActor extends HttpServiceActor with ActorLogging with RestApiRoute with StaticContentRoutes with InfoRoute with ActorExecutionContextProvider {

  implicit val timeout = HttpServerActor.timeout

  def handleTimeouts: Receive = {
    case Timedout(x: HttpRequest) =>
      sender() ! HttpResponse(InternalServerError)
  }

  def receive = handleTimeouts orElse runRoute(staticContentRoutes ~ restApiRoutes)
}
