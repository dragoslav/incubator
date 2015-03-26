package nl.proja.pistraw

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import nl.proja.pishake.common.{ActorDescription, ActorSupport, FutureSupport}
import nl.proja.pishake.model.SystemInfo
import nl.proja.pishake.operation.SystemActor
import nl.proja.pishake.operation.SystemActor.Info

import scala.concurrent.duration._
import scala.language.postfixOps

object PiStraw extends App {
  implicit val system = ActorSystem("PiStraw")

  ActorSupport.actorOf(PiStrawActor) ! "START"
}

object PiStrawActor extends ActorDescription {

  def props(args: Any*): Props = Props[PiStrawActor]

}

class PiStrawActor extends Actor with ActorLogging with FutureSupport with ActorSupport {

  implicit val timeout = Timeout(5 seconds)

  val remoteUrl = ConfigFactory.load().getString("akka.remote.url")

  def receive = {
    case "START" => offLoad(remoteActorFor(remoteUrl, SystemActor.name) ? Info) match {
      case info: SystemInfo => println(info)
      case any => println(any)
    }
  }
}


