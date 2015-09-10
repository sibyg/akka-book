package com.example

import akka.actor._
import akka.event.Logging

import scala.concurrent.duration.Duration.Undefined
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class MyActor extends Actor {
  val log = Logging(context.system, this)

  context.setReceiveTimeout(30 seconds)

  override def receive: Receive = {
    case "test" => log.info("received test")
    case ReceiveTimeout => context.setReceiveTimeout(Undefined)
      throw new RuntimeException("Receive time out")
    case _ => log.info("unknown msg")
  }

  override def postStop(): Unit = {
    log.info("Post Stop message")
  }
}

object MyActor {
  def props() = Props(new MyActor())
}


object Manager {

  case object Shutdown

  def props() = Props(new Manager())
}

class Manager extends Actor {

  val log = Logging(context.system, this)

  import com.example.Manager._

  val worker = context.watch(context.actorOf(MyActor.props(), "worker"))

  override def receive: Actor.Receive = {
    case "job" => worker
    case Shutdown =>
      log.info("Shutdown received")
      worker ! PoisonPill
      context become shuttingDown
  }

  def shuttingDown: Receive = {
    case "job" => sender() ! "service is unavailable, shutting down"
    case Terminated(`worker`) => context stop self
  }
}


object Application316_318 extends App {

  import akka.pattern.gracefulStop

  val system = ActorSystem("316")
  val myActor: ActorRef = system.actorOf(MyActor.props(), "myActor")
  val manager: ActorRef = system.actorOf(Manager.props(), "manager")

  myActor ! "test"
  myActor ! "DSAGT"
//  myActor ! PoisonPill

  try {
    val stopped: Future[Boolean] = gracefulStop(manager, 10 seconds, Manager.Shutdown)
    Await.result(stopped, 6 seconds)
  } catch {
    case e: akka.pattern.AskTimeoutException =>
  }

  // check how this is handled after gracefulStop
  manager ! "job"
  //  system.stop(myActor)

  system.shutdown()
}
