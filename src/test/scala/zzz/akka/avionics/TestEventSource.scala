package zzz.akka.avionics

import akka.actor.{ActorSystem, Actor}
import akka.testkit.{TestActorRef, TestKit}
import org.scalatest.{Matchers, BeforeAndAfterAll, WordSpecLike}

// We can't test a "trait" very easily, so we're going to
// create a specific EventSource derivation that conforms to
// the requirements of the trait so that we can test the
// production code.
class TestEventSource extends Actor with ProductionEventSource {
  override def receive: Receive = eventSourceReceive
}

class EventSourceSpec extends TestKit(ActorSystem("EventSourceSpec")) with WordSpecLike with Matchers with BeforeAndAfterAll {
  import EventSource._

  override protected def afterAll(): Unit = system.shutdown()

  "EventSource" should {
    "allow us to register a listener" in {
      val real = TestActorRef[TestEventSource].underlyingActor
      real.receive(RegisterListener(testActor))
      real.listeners should contain (testActor)
    }

    "allow us to unregister a listener" in {
      val real = TestActorRef[TestEventSource].underlyingActor
      real.receive(UnregisterListener(testActor))
      real.listeners.size should be (0)
    }

    "send the event to our test actor" in {
      val testA = TestActorRef[TestEventSource]
      testA ! RegisterListener(testActor)
      testA.underlyingActor.sendEvent("Fibinocci")
      expectMsg("Fibinocci")
    }
  }


}