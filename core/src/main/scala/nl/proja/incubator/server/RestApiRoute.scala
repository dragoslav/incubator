package nl.proja.incubator.server

import akka.actor.Actor
import akka.pattern.ask
import akka.util.Timeout
import nl.proja.incubator.gpio.TemperatureActor
import nl.proja.incubator.json.{OffsetDateTimeSerializer, SerializationFormat, SnakeCaseSerializationFormat}
import nl.proja.pishake.util.{ActorSupport, ExecutionContextProvider, FutureSupport}
import org.json4s.native.Serialization._
import spray.http.CacheDirectives.`no-store`
import spray.http.HttpEntity
import spray.http.HttpHeaders.{RawHeader, `Cache-Control`}
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.httpx.marshalling.Marshaller
import spray.httpx.unmarshalling.Unmarshaller
import spray.routing.HttpServiceBase

import scala.concurrent.Future
import scala.language.{existentials, postfixOps}

trait RestApiRoute extends HttpServiceBase with RestApiController with InfoRoute {
  this: Actor with ExecutionContextProvider =>

  implicit def timeout: Timeout

  protected def noCachingAllowed = respondWithHeaders(`Cache-Control`(`no-store`), RawHeader("Pragma", "no-cache"))

  protected def allowXhrFromOtherHosts = respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*"))

  implicit val marshaller: Marshaller[Any] = Marshaller.of[Any](`application/json`) { (value, contentType, ctx) =>
    implicit val formats = SerializationFormat(SnakeCaseSerializationFormat, OffsetDateTimeSerializer)
    val response = value match {
      case exception: Exception => throw new RuntimeException(exception)
      case response: AnyRef => write(response)
      case any => write(any.toString)
    }
    ctx.marshalTo(HttpEntity(contentType, response))
  }

  implicit val unmarshaller = Unmarshaller[Map[String, Any]](`application/json`) {
    case HttpEntity.NonEmpty(contentType, data) =>
      implicit val formats = SerializationFormat(SnakeCaseSerializationFormat)
      read[Map[String, Any]](data.asString)

    case HttpEntity.Empty => Map[String, Any]()
  }

  val restApiRoutes = noCachingAllowed {
    allowXhrFromOtherHosts {
      pathPrefix("api" / "v1") {
        respondWithMediaType(`application/json`) {
          infoRoute ~ pathPrefix("temperatures") {
            pathEndOrSingleSlash {
              get {
                onSuccess(getTemperatures) {
                  complete(OK, _)
                }
              } ~ post {
                entity(as[Map[String, Any]]) { request =>
                  onSuccess(setTargetTemperature(request)) {
                    complete(OK, _)
                  }
                }
              }
            } ~ path(Segment) { name =>
              pathEndOrSingleSlash {
                get {
                  rejectEmptyResponse {
                    onSuccess(getTemperature(name)) {
                      complete(OK, _)
                    }
                  }
                }
              }
            } ~ path("target") {
              pathEndOrSingleSlash {
                put {
                  entity(as[Map[String, Any]]) { request =>
                    onSuccess(setTargetTemperature(request)) {
                      complete(OK, _)
                    }
                  }
                } ~ delete {
                  onSuccess(resetTargetTemperature()) {
                    complete(NoContent, _)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

trait RestApiController extends ActorSupport with FutureSupport {
  this: Actor with ExecutionContextProvider =>

  def getTemperatures(implicit timeout: Timeout): Future[Any] = actorFor(TemperatureActor) ? TemperatureActor.GetTemperatures

  def getTemperature(name: String = TemperatureActor.targetTemperature)(implicit timeout: Timeout) = actorFor(TemperatureActor) ? TemperatureActor.GetTemperature(name)

  def setTargetTemperature(request: Map[String, Any])(implicit timeout: Timeout) = request.get("value") match {
    case None => actorFor(TemperatureActor) ? TemperatureActor.SetTargetTemperature(None)
    case Some(value: Double) => actorFor(TemperatureActor) ? TemperatureActor.SetTargetTemperature(Some(value))
    case Some(value: BigInt) => actorFor(TemperatureActor) ? TemperatureActor.SetTargetTemperature(Some(value.doubleValue()))
    case _ => Future {}
  }

  def resetTargetTemperature()(implicit timeout: Timeout) = actorFor(TemperatureActor) ? TemperatureActor.SetTargetTemperature(None)

}
