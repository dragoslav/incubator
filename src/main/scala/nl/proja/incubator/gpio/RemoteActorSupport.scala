package nl.proja.incubator.gpio

import akka.actor.{Actor, ActorSelection}
import com.typesafe.config.ConfigFactory
import nl.proja.pishake.util.{ActorSupport, FutureSupport}

trait RemoteActorSupport extends FutureSupport with ActorSupport {
  this: Actor =>

  private val remoteUrl = ConfigFactory.load().getString("akka.remote.url")

  def remoteActorFor(actor: String): ActorSelection = remoteActorFor(remoteUrl, actor)
}
