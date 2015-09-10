package zzz.akka.investigation
import akka.actor.Actor
case class Gamma(g: String)
case class Beta(b: String, g: Gamma)
case class Alpha(b1: Beta, b2: Beta)
class MyActor extends Actor {
  def receive = {
    // Literal String match
    case "Hello" =>
      println("Hi")
    // Literal Int match
    case 42 =>
      println("I don't know the question." +
        "Go ask the Earth Mark II.")
    // Matches any string at all
    case s: String =>
      println(s"You sent me a string: $s")
      // Match a more complex case class structure
    case Alpha(Beta(b1, Gamma(g1)), Beta(b2, Gamma(g2))) =>
      println(s"beta1: $b1, beta2: $b2, gamma1: $g1, gamma2: $g2")
    // Catch all. Matches any message type
    case _ =>
      println("Huh?")
  }
}