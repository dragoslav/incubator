package nl.proja.incubator.gpio

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import akka.actor._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import nl.proja.incubator.Bootstrap.{Shutdown, Start}
import nl.proja.incubator.store.ElasticSearchActor
import nl.proja.incubator.store.ElasticSearchActor.IndexDocument
import nl.proja.pishake.operation.DS18B20Controller
import nl.proja.pishake.operation.DS18B20Controller.{DS18B20, ReadDS18B20}
import nl.proja.pishake.util.{ActorDescription, ActorSupport, FutureSupport}

import scala.concurrent.duration._
import scala.language.postfixOps

object IncubatorActor extends ActorDescription {

  def props(args: Any*): Props = Props[IncubatorActor]

}

case class Temperature(value: Double, timestamp: String = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))

class IncubatorActor extends Actor with ActorLogging with FutureSupport with ActorSupport {

  private val config = ConfigFactory.load().getConfig("incubator")

  private implicit val timeout = Timeout(5 seconds)
  private val remoteUrl = ConfigFactory.load().getString("akka.remote.url")

  private lazy val elasticSearch = actorFor(ElasticSearchActor)
  private lazy val dS18B20Controller = remoteActorFor(remoteUrl, DS18B20Controller.name)

  private var temperatureReader: Option[Cancellable] = None

  def receive = {
    case Start =>
      implicit val ec = context.system.dispatcher
      temperatureReader = Some(context.system.scheduler.schedule(0 seconds, config.getInt("temperature-read-period") seconds, new Runnable {
        def run() = {
          dS18B20Controller ! ReadDS18B20
        }
      }))

    case Shutdown => temperatureReader.map(_.cancel())

    case sensor: DS18B20 =>
      log.debug(s"${sensor.name}: ${sensor.temperature}°C [${sensor.timestamp.format(DateTimeFormatter.ISO_INSTANT)}]")
      elasticSearch ! IndexDocument(s"incubator-${sensor.timestamp.format(DateTimeFormatter.ofPattern("YYYY-MM-dd"))}", s"temperature-${sensor.name}", sensor)

  }

}