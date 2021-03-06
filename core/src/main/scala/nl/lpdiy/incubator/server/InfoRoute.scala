package nl.lpdiy.incubator.server

import akka.actor.Actor
import akka.pattern.ask
import akka.util.Timeout
import nl.lpdiy.incubator.gpio.PiShakeActorSupport
import nl.lpdiy.pishake.model.SystemInfo
import nl.lpdiy.pishake.operation.SystemActor
import nl.lpdiy.pishake.util.{ExecutionContextProvider, FutureSupport}
import spray.http.StatusCodes._
import spray.httpx.marshalling.Marshaller
import spray.routing.HttpServiceBase

import scala.language.{existentials, postfixOps}

case class InfoMessage(raspberryPi: Option[SystemInfo], jvm: JvmVitals)

trait InfoRoute extends HttpServiceBase with JvmVitalsProvider with PiShakeActorSupport with FutureSupport {
  this: Actor with ExecutionContextProvider =>

  implicit def marshaller: Marshaller[Any]

  implicit def timeout: Timeout

  val infoRoute = pathPrefix("info") {
    pathEndOrSingleSlash {
      get {
        onSuccess(info) {
          complete(OK, _)
        }
      }
    }
  }

  def info = piShakeActorFor(SystemActor.name) ? SystemActor.Info map {
    case system: SystemInfo => InfoMessage(Some(system), vitals())
    case any => InfoMessage(None, vitals())
  }
}

