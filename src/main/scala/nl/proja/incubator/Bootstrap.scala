package nl.proja.incubator

import akka.actor._
import nl.proja.pishake.util.ActorSupport

import scala.language.postfixOps

object Bootstrap extends App {

  object Start extends Serializable

  object Shutdown extends Serializable

  implicit val system = ActorSystem("Incubator")

  val actors = ActorSupport.actorOf(ElasticSearchActor) :: ActorSupport.actorOf(IncubatorActor) :: Nil

  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run() = {
      actors.reverse.foreach { actor =>
        actor ! Shutdown
        actor ! PoisonPill
      }
    }
  })

  actors.foreach(actor => actor ! Start)
}
