package nl.proja.incubator.server

import akka.actor.Actor
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import nl.proja.pishake.model.SystemInfo
import nl.proja.pishake.operation.SystemActor
import nl.proja.pishake.util.{ActorSupport, ExecutionContextProvider, FutureSupport}
import spray.http.StatusCodes._
import spray.httpx.marshalling.Marshaller
import spray.routing.HttpServiceBase

import scala.language.{existentials, postfixOps}

case class HiMessage(message: String, raspberryPi: Option[SystemInfo], jvm: JvmVitals)

trait HiRoute extends HttpServiceBase with JvmVitalsProvider with ActorSupport with FutureSupport {
  this: Actor with ExecutionContextProvider =>

  implicit def marshaller: Marshaller[Any]

  implicit def timeout: Timeout

  private lazy val hi = ConfigFactory.load().getString("incubator.hi-message")
  private lazy val remoteUrl = ConfigFactory.load().getString("akka.remote.url")

  val hiRoute = pathPrefix("hi") {
    pathEndOrSingleSlash {
      get {
        onSuccess(hiMessage) {
          complete(OK, _)
        }
      }
    }
  }

  def hiMessage = remoteActorFor(remoteUrl, SystemActor.name) ? SystemActor.Info map {
    case system: SystemInfo => HiMessage(hi, Some(system), vitals())
    case any => HiMessage(hi, None, vitals())
  }
}

