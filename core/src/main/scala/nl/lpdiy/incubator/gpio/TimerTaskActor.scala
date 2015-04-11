package nl.lpdiy.incubator.gpio

import akka.actor.{Actor, Cancellable}

import scala.concurrent.duration.{FiniteDuration, _}
import scala.language.postfixOps

trait TimerTaskActor {
  this: Actor =>

  private var timer: Option[Cancellable] = None

  def startTimer(callback: () => Unit, interval: FiniteDuration) = {
    implicit val ec = context.system.dispatcher
    timer = Some(context.system.scheduler.schedule(0 seconds, interval, new Runnable {
      def run() = {
        callback()
      }
    }))
  }

  def cancelTimer() = timer.map(_.cancel())
}
