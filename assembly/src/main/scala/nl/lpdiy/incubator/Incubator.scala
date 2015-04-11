package nl.lpdiy.incubator

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import nl.lpdiy.pishake.PiShake
import scala.language.postfixOps

object Incubator extends App {

  val config = ConfigFactory.load().getConfig("incubator")

  implicit val system = ActorSystem(config.getString("akka"))

  PiShake.run
  Bootstrap.run
}
