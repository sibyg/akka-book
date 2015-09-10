package zzz.akka.avionics

import akka.actor.{Props, Actor, ActorSystem}
import akka.testkit.{TestActorRef, TestLatch, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class AltimeterSpec extends TestKit(ActorSystem("AltimeterSpec"))
with ImplicitSender
with WordSpecLike
with Matchers
with BeforeAndAfterAll {
  import Altimeter._
  override def afterAll() { system.shutdown() }

  // We'll instantiate a Helper class for every test, making
  // things nicely reusable.
  class Helper {
    object EventSourceSpy {
      // The latch gives us fast feedback when
      // something happens
      val latch = TestLatch(1)
    }
    // Our special derivation of EventSource gives us the
    // hooks into concurrency
    trait EventSourceSpy extends EventSource {
      def sendEvent[T](event: T): Unit =
        EventSourceSpy.latch.countDown()
      // We don't care about processing the messages that
      // EventSource usually processes so we simply don't
      // worry about them.
      def eventSourceReceive = Actor.emptyBehavior

    }
    // The slicedAltimeter constructs our Altimeter with
    // the EventSourceSpy
    def slicedAltimeter = new Altimeter with EventSourceSpy

    // This is a helper method that will give us an ActorRef
    // and our plain ol' Altimeter that we can work with
    // directly.
    def actor() = {
      val a = TestActorRef[Altimeter](Props(slicedAltimeter))
      (a, a.underlyingActor)
    }
  }
}