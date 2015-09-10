package com.example.c_315

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class ActorA extends Actor with ActorLogging {

  def receive = {
    case Request =>
      log.info("In ActorA - Request")
      sender() ! 10
    case _ =>
      log.info("In ActorA - Unknown")
  }
}

object ActorA {
  def props(): Props = Props(new ActorA())
}

class ActorB extends Actor with ActorLogging {

  def receive = {
    case Request =>
      log.info("In ActorB - Request")
      sender() ! "ActorBResponse"
    case _ =>
      log.info("In ActorB - Unknown")
  }
}

object ActorB {
  def props(): Props = Props(new ActorB())
}

class ActorC extends Actor with ActorLogging {

  def receive = {
    case Request =>
      log.info("In ActorC - Request")
      sender() ! 10.45
    case _ =>
      log.info("In ActorC - Unknown")
  }
}

object ActorC {
  def props(): Props = Props(new ActorC())
}

class ActorD extends Actor with ActorLogging {
  def receive = {
    case r: Result => log.info("Result=" + r)
  }
}

object ActorD {
  def props() = Props(new ActorD())
}

object Request {}

case class Result(x: Int, s: String, d: Double)

object ApplicationMain315 extends App {
  val system = ActorSystem("315")
  val actorA = system.actorOf(ActorA.props(), "actorA")
  val actorB = system.actorOf(ActorB.props(), "actorB")
  val actorC = system.actorOf(ActorC.props(), "actorC")
  val actorD = system.actorOf(ActorD.props(), "actorD")

  // implicit timeout
  implicit val timeout = Timeout(5 seconds)

  private val f: Future[Result] = for {
    x <- ask(actorA, Request).mapTo[Int]
    y <- ask(actorB, Request).mapTo[String]
    z <- ask(actorC, Request).mapTo[Double]
  } yield Result(x, y, z)

  f pipeTo actorD

  // thereafter terminate the ActorSystem -
  system.awaitTermination()
}