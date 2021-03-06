package nl.lpdiy.incubator

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import com.typesafe.config.ConfigFactory
import nl.lpdiy.incubator.gpio.{HeaterActor, TemperatureActor}
import nl.lpdiy.incubator.server.HttpServerActor
import nl.lpdiy.incubator.store.ElasticSearchActor
import nl.lpdiy.incubator.gpio.HeaterActor
import nl.lpdiy.pishake.util.ActorSupport
import spray.can.Http

import scala.language.postfixOps

object Bootstrap extends App {

  object Start extends Serializable

  object Shutdown extends Serializable

  implicit val system = ActorSystem(ConfigFactory.load().getConfig("incubator").getString("akka"))

  def run(implicit system: ActorSystem) = {
    val config = ConfigFactory.load().getConfig("incubator")

    implicit val timeout = HttpServerActor.timeout

    val actors = ActorSupport.actorOf(ElasticSearchActor) :: ActorSupport.actorOf(TemperatureActor) :: ActorSupport.actorOf(HeaterActor) :: Nil

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() = {
        actors.reverse.foreach { actor =>
          actor ! Shutdown
          actor ! PoisonPill
        }
      }
    })

    actors.foreach(actor => actor ! Start)
    IO(Http)(system) ? Http.Bind(ActorSupport.actorOf(HttpServerActor), config.getString("server.interface"), config.getInt("server.port"))
  }

  run
}
