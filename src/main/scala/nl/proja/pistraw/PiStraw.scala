package nl.proja.pistraw

import akka.actor._
import nl.proja.pishake.util.ActorSupport

import scala.language.postfixOps

object PiStraw extends App {

  object Start extends Serializable

  object Shutdown extends Serializable

  implicit val system = ActorSystem("PiStraw")

  val elasticSearchActor = ActorSupport.actorOf(ElasticSearchActor)
  val incubatorActor = ActorSupport.actorOf(IncubatorActor)

  elasticSearchActor ! Start
  incubatorActor ! Start

  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run() = {
      incubatorActor ! Shutdown
      elasticSearchActor ! Shutdown

      incubatorActor ! PoisonPill
      elasticSearchActor ! PoisonPill
    }
  })
}


