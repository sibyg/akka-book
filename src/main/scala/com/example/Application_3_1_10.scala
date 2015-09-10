package com.example

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging

class HotSwapActor extends Actor {

  import context._

  val log = Logging(context.system, self)

  override def receive: Receive = {
    case "default" =>
      log.info("Default")
      become(angry)

    case "stack" =>
      log.info("Stack")
      become({
        case "swap" =>
          log.info("Swap")
          unbecome()
      }, false)

    case _ =>
      log.info("Unknown msg")
  }

  def angry: Receive = {
    case "angry" => log.info("Angry")
      become(happy)
  }

  def happy: Receive = {
    case "happy" => log.info("Happy")
      unbecome()
  }
}

object HotSwapActor {
  def props() = Props(new HotSwapActor())
}

object Application_310 extends App {
  val system = ActorSystem("become_unbecome")
  private val hotSwapActor: ActorRef = system.actorOf(HotSwapActor.props(), "hotswapActor")
  hotSwapActor ! "stack"
  hotSwapActor ! "swap"
  hotSwapActor ! "default"
  hotSwapActor ! "angry"
  hotSwapActor ! "happy"
  hotSwapActor ! "default"

  system.awaitTermination()
}
