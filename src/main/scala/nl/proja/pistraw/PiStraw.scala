package nl.proja.pistraw

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import nl.proja.pishake.model.{Gpio, SystemInfo}
import nl.proja.pishake.operation.DS18B20Controller.{DS18B20, ReadDS18B20}
import nl.proja.pishake.operation.GpioOut.Pulse
import nl.proja.pishake.operation.SystemActor.Info
import nl.proja.pishake.operation.{DS18B20Controller, GpioController, SystemActor}
import nl.proja.pishake.util.{ActorDescription, ActorSupport, FutureSupport}
import nl.proja.pistraw.PiStrawActor.Start

import scala.concurrent.duration._
import scala.language.postfixOps

object PiStraw extends App {
  implicit val system = ActorSystem("PiStraw")

  ActorSupport.actorOf(PiStrawActor) ! Start
}

object PiStrawActor extends ActorDescription {

  def props(args: Any*): Props = Props[PiStrawActor]

  object Start extends Serializable

}

class PiStrawActor extends Actor with ActorLogging with FutureSupport with ActorSupport {

  implicit val timeout = Timeout(5 seconds)

  val remoteUrl = ConfigFactory.load().getString("akka.remote.url")
  lazy val gpioController = remoteActorFor(remoteUrl, GpioController.name)
  lazy val dS18B20Controller = remoteActorFor(remoteUrl, DS18B20Controller.name)

  def receive = {
    case Start => dS18B20()

    case DS18B20(serialNumber, temperature) => println(s"$serialNumber: $temperature°C")
  }

  def info() = offload(remoteActorFor(remoteUrl, SystemActor.name) ? Info) match {
    case info: SystemInfo => println(info)
    case any => println(any)
  }

  def led() = {
    gpioController ! Pulse(Gpio.Pin.Pin01, 3 seconds)
  }

  def dS18B20() = {
    dS18B20Controller ! ReadDS18B20
  }

}


