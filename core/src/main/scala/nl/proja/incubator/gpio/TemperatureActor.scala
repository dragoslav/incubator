package nl.proja.incubator.gpio

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import akka.actor._
import com.typesafe.config.ConfigFactory
import nl.proja.incubator.Bootstrap.{Shutdown, Start}
import nl.proja.incubator.store.ElasticSearch
import nl.proja.incubator.store.ElasticSearchActor.IndexDocument
import nl.proja.pishake.operation.DS18B20Controller
import nl.proja.pishake.util.ActorDescription

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

  object GetTemperatureStatistics

  case class TemperatureStatistics(average: Double, minimum: Double, maximum: Double, target: Double)

}

class TemperatureActor extends Actor with ActorLogging with PiShakeActorSupport with ElasticSearch with TimerTaskActor {

  import TemperatureActor._

  private lazy val temperatureSensor = piShakeActorFor(DS18B20Controller.name)

  private val temperatures = new mutable.LinkedHashMap[String, Temperature]()

  def receive = {
    case Start => startTimer({ () =>
      temperatureSensor ! DS18B20Controller.ReadTemperature
      temperatures.get(targetTemperature).foreach { temperature =>
        store(temperature.copy(timestamp = OffsetDateTime.now()))
      }
    }, ConfigFactory.load().getConfig("incubator").getInt("temperature-read-period") seconds)

    case Shutdown => cancelTimer()

    case sensor: DS18B20Controller.Temperature =>
      val temperature = Temperature(sensor.sensor, sensor.value, sensor.timestamp)
      log.debug(s"${temperature.name}: ${temperature.value}°C [${temperature.timestamp.format(DateTimeFormatter.ISO_INSTANT)}]")
      temperatures.put(sensor.sensor, temperature)
      store(temperature)

    case GetTemperatures => sender ! temperatures.values

    case GetTemperature(name) => sender ! temperatures.get(name)

    case SetTargetTemperature(Some(value)) => sender ! temperatures.put(targetTemperature, Temperature(targetTemperature, value, OffsetDateTime.now()))

    case SetTargetTemperature(None) => sender ! temperatures.remove(targetTemperature)

    case GetTemperatureStatistics =>
      temperatures.get(targetTemperature) match {
        case None =>
        case Some(Temperature(_, target, _)) =>
          val readings = temperatures.values.filter(_.name != targetTemperature).map(_.value)
          if (readings.size > 0)
            sender ! TemperatureStatistics(readings.sum / readings.size, readings.min, readings.max, target)
      }
  }

  def store(temperature: Temperature) = {
    val index = s"incubator-${temperature.timestamp.format(DateTimeFormatter.ofPattern("YYYY-MM-dd"))}"
    elasticSearch ! IndexDocument(index, "temperature", Map(temperature.name -> temperature.value, "timestamp" -> temperature.timestamp))
  }
}

