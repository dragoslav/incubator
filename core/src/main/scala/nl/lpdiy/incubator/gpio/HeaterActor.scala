package nl.lpdiy.incubator.gpio

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import nl.lpdiy.incubator.Bootstrap
import nl.lpdiy.incubator.store.{ElasticSearch, ElasticSearchActor}
import Bootstrap.{Shutdown, Start}
import TemperatureActor.{GetTemperatureStatistics, TemperatureStatistics}
import ElasticSearchActor.IndexDocument
import nl.lpdiy.pishake.model.Gpio
import nl.lpdiy.pishake.operation.GpioController
import nl.lpdiy.pishake.operation.GpioOut.{Low, Pulse}
import nl.lpdiy.pishake.util.ActorDescription

import scala.concurrent.duration._
import scala.language.postfixOps

object HeaterActor extends ActorDescription {

  def props(args: Any*): Props = Props[HeaterActor]

}

class HeaterActor extends Actor with ActorLogging with PiShakeActorSupport with ElasticSearch with TimerTaskActor {

  private val period = ConfigFactory.load().getConfig("incubator").getInt("temperature-read-period") seconds
  private lazy val gpio = piShakeActorFor(GpioController.name)

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

