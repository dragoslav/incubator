package nl.proja.incubator

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import com.typesafe.config.ConfigFactory
import nl.proja.incubator.gpio.IncubatorActor
import nl.proja.incubator.server.HttpServerActor
import nl.proja.incubator.store.ElasticSearchActor
import nl.proja.pishake.util.ActorSupport
import spray.can.Http

import scala.language.postfixOps

object Bootstrap extends App {

  object Start extends Serializable

  object Shutdown extends Serializable

  implicit val system = ActorSystem("Incubator")
  implicit val timeout = HttpServerActor.timeout

  val config = ConfigFactory.load().getConfig("incubator.server")

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
  IO(Http)(system) ? Http.Bind(ActorSupport.actorOf(HttpServerActor), config.getString("interface"), config.getInt("port"))
}
