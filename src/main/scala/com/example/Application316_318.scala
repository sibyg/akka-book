package com.example

import akka.actor._
import akka.event.Logging

import scala.concurrent.duration.Duration.Undefined
import scala.concurrent.duration._

class MyActor extends Actor {
  val log = Logging(context.system, this)

  context.setReceiveTimeout(30 milliseconds)

  override def receive: Receive = {
    case "test" => log.info("received test")
    case ReceiveTimeout => context.setReceiveTimeout(Undefined)
      throw new RuntimeException("Receive time out")
    case _ => log.info("unknown msg")
  }
}

object MyActor {
  def props() = Props(new MyActor())
}

object Application316_318 extends App {
  val system = ActorSystem("316")
  val myActor: ActorRef = system.actorOf(MyActor.props(), "myActor")

  myActor ! "test"
  myActor ! "DSAGT"

  system.awaitTermination()
}
