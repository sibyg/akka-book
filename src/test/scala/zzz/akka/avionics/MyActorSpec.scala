package zzz.akka.avionics

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.gracefulStop
import akka.testkit.TestKit
import org.scalatest._
import zzz.akka.investigation.MyActor

import scala.concurrent.Await
import scala.concurrent.duration._

// It is helpful to create a base class for your tests
class TestKitSpec(actorSystem: ActorSystem) extends
TestKit(actorSystem)
with WordSpecLike
with Matchers
with BeforeAndAfterAll
class MyActorSpec extends TestKitSpec(ActorSystem("MyActorSpec")) with BeforeAndAfterEach {
  override def afterAll() { system.shutdown() }


  override protected def afterEach(): Unit =
  // Await on the result, giving timeouts for the gracefulStop
  // as well as the timeout on the Future that's running
    Await.result(gracefulStop(system.actorOf(Props[MyActor]), 5.seconds)(system), 6.seconds);

  def makeActor(): ActorRef = system.actorOf(Props[MyActor], "MyActor")
  "My Actor" should {
    "throw if constructed with the wrong name" in {
      evaluating {
        // use a generated name
        val a = system.actorOf(Props[MyActor])
      } should produce [Exception]
    }
    "construct without exception" in {
      val a = makeActor()
      // The throw will cause the test to fail
    }
    "respond with a Pong to a Ping" in {
      val a = makeActor()
      a ! "Ping"
      expectMsg("Pong")
    }
  }
}