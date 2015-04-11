package nl.proja.incubator.gpio

import akka.actor.{Actor, ActorSelection}
import com.typesafe.config.ConfigFactory
import nl.proja.pishake.util.{ActorSupport, FutureSupport}

trait PiShakeActorSupport extends FutureSupport with ActorSupport {
  this: Actor =>

  private val piShakeUrl = ConfigFactory.load().getString("incubator.pishake.akka-url")

  def piShakeActorFor(actor: String): ActorSelection = actorForPath(piShakeUrl, actor)
}
