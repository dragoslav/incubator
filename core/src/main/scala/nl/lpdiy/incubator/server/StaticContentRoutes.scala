package nl.lpdiy.incubator.server

import akka.actor.Actor
import akka.util.Timeout
import nl.lpdiy.pishake.util.{ActorSupport, ExecutionContextProvider, FutureSupport}
import spray.httpx.marshalling.Marshaller
import spray.routing.HttpServiceBase

trait StaticContentRoutes extends HttpServiceBase with JvmVitalsProvider with ActorSupport with FutureSupport {
  this: Actor with ExecutionContextProvider =>

  implicit def marshaller: Marshaller[Any]

  implicit def timeout: Timeout

  val staticContentRoutes = pathEndOrSingleSlash {
    get {
      getFromResource("public/incubator.html")
    }
  } ~ pathPrefix("^(css|js|images)$".r) { dir =>
    get {
      getFromResourceDirectory(s"public/$dir")
    }
  } ~ path("^incubator\\.html$".r) { extension =>
    get {
      getFromResource(s"public/incubator.html")
    }
  }
}
