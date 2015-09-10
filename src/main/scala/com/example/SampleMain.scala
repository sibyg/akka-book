package com.example

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

object SampleMain extends App {
  val system = ActorSystem("MyActorSystem")
  val pingActor = system.actorOf(PingActor.props, "pingActor")
  pingActor ! PingActor.Initialize
  // This example app will ping pong 3 times and thereafter terminate the ActorSystem - 
  // see counter logic in PingActor
  system.awaitTermination()
}


class PongActor extends Actor with ActorLogging {

  import com.example.PongActor._

  def receive = {
    case PingActor.PingMessage(text) =>
      log.info("In PongActor - received message: {}", text)
      sender() ! PongMessage("pong")
  }
}

object PongActor {
  val props = Props[PongActor]

  case class PongMessage(text: String)

}

class PingActor extends Actor with ActorLogging {

  import com.example.PingActor._

  var counter = 0
  val pongActor = context.actorOf(PongActor.props, "pongActor")

  def receive = {
    case Initialize =>
      log.info("In PingActor - starting ping-pong")
      pongActor ! PingMessage("ping")
    case PongActor.PongMessage(text) =>
      log.info("In PingActor - received message: {}", text)
      counter += 1
      if (counter == 3) context.system.shutdown()
      else sender() ! PingMessage("ping")
  }
}

object PingActor {
  val props = Props[PingActor]

  case object Initialize

  case class PingMessage(text: String)

}
