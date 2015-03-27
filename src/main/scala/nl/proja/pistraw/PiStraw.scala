package nl.proja.pistraw

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import nl.proja.pishake.model.{Gpio, SystemInfo}
import nl.proja.pishake.operation.GpioOut.Pulse
import nl.proja.pishake.operation.SystemActor.Info
import nl.proja.pishake.operation.{GpioController, SystemActor}
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

  def receive = {
    case Start => led()
  }

  lazy val gpioController = remoteActorFor(remoteUrl, GpioController.name)

  def info() = offload(remoteActorFor(remoteUrl, SystemActor.name) ? Info) match {
    case info: SystemInfo => println(info)
    case any => println(any)
  }

  def led() = {
    gpioController ! Pulse(Gpio.Pin.Pin01, 3 seconds)
  }
}


