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
import nl.proja.pishake.util.{ActorDescription, ActorSupport, FutureSupport}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps

object TemperatureActor extends ActorDescription {

  def props(args: Any*): Props = Props[TemperatureActor]

  val targetTemperature = "target"

  object GetTemperatures

  case class GetTemperature(name: String)

  case class SetTargetTemperature(value: Option[Double])

  case class Temperature(name: String, value: Double, timestamp: OffsetDateTime)

}

class TemperatureActor extends Actor with ActorLogging with FutureSupport with ActorSupport {

  import TemperatureActor._

  private val config = ConfigFactory.load().getConfig("incubator")

  private implicit val timeout = Timeout(5 seconds)
  private val remoteUrl = ConfigFactory.load().getString("akka.remote.url")

  private lazy val elasticSearch = actorFor(ElasticSearchActor)
  private lazy val temperatureSensor = remoteActorFor(remoteUrl, DS18B20Controller.name)

  private var temperatureReader: Option[Cancellable] = None

  private val temperatures = new mutable.LinkedHashMap[String, Temperature]()

  def receive = {
    case Start =>
      implicit val ec = context.system.dispatcher
      temperatureReader = Some(context.system.scheduler.schedule(0 seconds, config.getInt("temperature-read-period") seconds, new Runnable {
        def run() = {
          temperatureSensor ! DS18B20Controller.ReadTemperature
          temperatures.get(targetTemperature).foreach { temperature =>
            store(temperature.copy(timestamp = OffsetDateTime.now()))
          }
        }
      }))

    case Shutdown => temperatureReader.map(_.cancel())

    case sensor: DS18B20Controller.Temperature =>
      val temperature = Temperature(sensor.sensor, sensor.value, sensor.timestamp)
      log.debug(s"${temperature.name}: ${temperature.value}°C [${temperature.timestamp.format(DateTimeFormatter.ISO_INSTANT)}]")
      temperatures.put(sensor.sensor, temperature)
      store(temperature)

    case GetTemperatures => sender ! temperatures.values

    case GetTemperature(name) => sender ! temperatures.get(name)

    case SetTargetTemperature(Some(value)) => sender ! temperatures.put(targetTemperature, Temperature(targetTemperature, value, OffsetDateTime.now()))

    case SetTargetTemperature(None) => sender ! temperatures.remove(targetTemperature)
  }

  def store(temperature: Temperature) = {
    val index = s"incubator-${temperature.timestamp.format(DateTimeFormatter.ofPattern("YYYY-MM-dd"))}"
    elasticSearch ! IndexDocument(index, "temperature", Map(temperature.name -> temperature.value, "timestamp" -> temperature.timestamp))
  }
}

