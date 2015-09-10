package com.example

import akka.actor._
import akka.event.Logging


class SomeActor extends Actor with Stash {
  val log = Logging(context.system, this)

  override def receive: Receive = {
    case msg =>
      stash()
    case "open" =>
      unstashAll()
      context.become({
        case "write" =>
          log.info("Writing")
        case "close" =>
          unstashAll()
          context.unbecome()
        case msg => stash()
      }, discardOld = false) // stack on top instead of replacing
  }
}

object SomeActor {
  def props() = Props(new SomeActor())
}

object Application_3_1_11 extends App {
  val system = ActorSystem("Stashing")
  val actorRef: ActorRef = system.actorOf(SomeActor.props())

  actorRef ! "msg"
  actorRef ! "write"
  actorRef ! "close"


}
