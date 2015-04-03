package nl.proja.incubator.server

import akka.actor.Actor
import akka.util.Timeout
import nl.proja.incubator.json.{SerializationFormat, SnakeCaseSerializationFormat}
import nl.proja.pishake.util.{ActorSupport, ExecutionContextProvider, FutureSupport}
import org.json4s.native.Serialization._
import spray.http.CacheDirectives.`no-store`
import spray.http.HttpEntity
import spray.http.HttpHeaders.{RawHeader, `Cache-Control`}
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.httpx.marshalling.Marshaller
import spray.routing.HttpServiceBase

import scala.concurrent.Future
import scala.language.{existentials, postfixOps}

trait RestApiRoute extends HttpServiceBase with RestApiController with HiRoute {
  this: Actor with ExecutionContextProvider =>

  implicit def timeout: Timeout

  protected def noCachingAllowed = respondWithHeaders(`Cache-Control`(`no-store`), RawHeader("Pragma", "no-cache"))

  protected def allowXhrFromOtherHosts = respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*"))

  implicit val marshaller: Marshaller[Any] = Marshaller.of[Any](`application/json`) { (value, contentType, ctx) =>
    implicit val formats = SerializationFormat(SnakeCaseSerializationFormat)

    val response = value match {
      case exception: Exception => throw new RuntimeException(exception)
      case response: AnyRef => write(response)
      case any => write(any.toString)
    }
    ctx.marshalTo(HttpEntity(contentType, response))
  }

  val route = noCachingAllowed {
    allowXhrFromOtherHosts {
      pathPrefix("api" / "v1") {
        respondWithMediaType(`application/json`) {
          hiRoute ~ pathPrefix("temperature") {
            pathEndOrSingleSlash {
              get {
                onSuccess(getTemperatures) {
                  complete(OK, _)
                }
              } ~ post {
                entity(as[String]) { request =>
                  onSuccess(setTargetTemperature(request)) {
                    complete(OK, _)
                  }
                }
              }
            } ~ path("target") {
              pathEndOrSingleSlash {
                get {
                  rejectEmptyResponse {
                    onSuccess(getTargetTemperature) {
                      complete(OK, _)
                    }
                  }
                } ~ put {
                  entity(as[String]) { request =>
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

  def getTemperatures(implicit timeout: Timeout): Future[Any] = Future {
    ""
  }

  def getTargetTemperature(implicit timeout: Timeout) = Future {
    ""
  }

  def setTargetTemperature(request: String)(implicit timeout: Timeout) = Future {
    ""
  }

  def resetTargetTemperature()(implicit timeout: Timeout) = Future {
    ""
  }

}
