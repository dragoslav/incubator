package nl.proja.incubator.gpio

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import nl.proja.incubator.Bootstrap.{Shutdown, Start}
import nl.proja.incubator.gpio.TemperatureActor.{GetTemperatureStatistics, TemperatureStatistics}
import nl.proja.incubator.store.ElasticSearch
import nl.proja.incubator.store.ElasticSearchActor.IndexDocument
import nl.proja.pishake.model.Gpio
import nl.proja.pishake.operation.GpioController
import nl.proja.pishake.operation.GpioOut.{Pulse, Blink, Low}
import nl.proja.pishake.util.ActorDescription

import scala.concurrent.duration._
import scala.language.postfixOps

object HeaterActor extends ActorDescription {

  def props(args: Any*): Props = Props[HeaterActor]

}

class HeaterActor extends Actor with ActorLogging with RemoteActorSupport with ElasticSearch with TimerTaskActor {

  private val period = ConfigFactory.load().getConfig("incubator").getInt("temperature-read-period") seconds
  private lazy val gpio = remoteActorFor(GpioController.name)

  def receive = {
    case Start => startTimer({ () =>
      actorFor(TemperatureActor) ! GetTemperatureStatistics
    }, period)

    case Shutdown =>
      cancelTimer()
      implicit val timeout = Timeout(5 seconds)
      offload(gpio ? Low(Gpio.Pin.Pin01))

    case TemperatureStatistics(average, min, max, target) =>
      if (target > average) {
        log.debug(s"heater: on")
        gpio ! Pulse(Gpio.Pin.Pin01, period + 1.seconds)
        store(target / 2)
      }
      else {
        log.debug(s"heater: off")
        gpio ! Low(Gpio.Pin.Pin01)
        store(0)
      }
  }

  def store(on: Double) = {
    val timestamp = OffsetDateTime.now()
    val index = s"incubator-${timestamp.format(DateTimeFormatter.ofPattern("YYYY-MM-dd"))}"
    elasticSearch ! IndexDocument(index, "heater", Map("heater" -> on, "timestamp" -> timestamp))
  }
}

