package com.example

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging

import scala.concurrent.duration._

class MyActor extends Actor {
  val log = Logging(context.system, this)

  override def receive: Receive = {
    case "test" => log.info("received test")
    case _ => log.info("unknown msg")
  }
}

object MyActor {
  def props() = Props(new MyActor())
}

object Application316 extends App {
  val system = ActorSystem("316")
  val myActor: ActorRef = system.actorOf(MyActor.props(), "myActor")

  myActor ! "test"
  myActor ! "DSAGT"

  system.awaitTermination()
}
