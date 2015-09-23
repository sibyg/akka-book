package com.example

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.event.LoggingReceive
import akka.util.Timeout
import com.example.Storage.StorageException
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

object FaultHandlingDocSample extends App {

  val config = ConfigFactory.parseString(
    """
      akka.loglevel = "DEBUG"
      akka.actor.debug {
      receive = on
      lifecycle = on
      }
    """)

  val system = ActorSystem("FaultToleranceSample", config)
  val worker = system.actorOf(Props[Worker], name = "worker")
  val listener = system.actorOf(Props[Listener], name = "listener")
}

class Listener extends Actor with ActorLogging {

  import com.example.Worker._

  context.setReceiveTimeout(15 seconds)

  override def receive: Actor.Receive = {
    case Progress(percent) =>
      log.info("Current progress:{} %", percent)
      if (percent > 100.0) {
        log.info("Thats all, shutting down")
        context.system.shutdown()
      }

    case ReceiveTimeout =>
      log.error("Shutting down due to unavailable service")
      context.system.shutdown()
  }
}

object Worker {

  case object Start

  case object Do

  case class Progress(percent: Double)

}

class Worker extends Actor with ActorLogging {
  implicit val askTimeout = Timeout(5 seconds)


  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: CounterService.ServiceUnavailable => Stop
  }

  override def receive: Receive = ???
}

object CounterService {

  case class Increment(n: Int)

  case object GetCurrentCount

  case class CurrentCount(key: String, count: Long)

  class ServiceUnavailable(msg: String) extends RuntimeException(msg)

  private case object Reconnect

}

class CounterService extends Actor {

  import com.example.Counter._
  import com.example.Storage._

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 5 seconds) {
    case _: StorageException => Restart
  }

  val key = self.path.name
  var storage: Option[ActorRef] = None
  var counter: Option[ActorRef] = None
  var backlog = IndexedSeq.empty[(ActorRef, Any)]
  val MaxBacklog = 10000


  override def preStart() {
    initStorage()
  }

  /**
   * The child storage is restarted in case of failure, but after 3 restarts,
   * and still failing it will be stopped. Better to back-off than continuously
   * failing. When it has been stopped we will schedule a Reconnect after a delay.
   * Watch the child so we receive Terminated message when it has been terminated.
   */
  def initStorage() {

    storage = Some(context.watch(context.actorOf(Props[Storage], name = "storage")))
    // Tell the counter, if any, to use the new storage
    counter foreach {
      _ ! UseStorage(storage)
    }
    // We need the initial value to be able to operate
    storage.get ! Get(key)
  }
}

object Storage {

  case class Entry(key: String, value: Long)

  case class Get(key: String)

  case class Store(entry: Entry)

  class StorageException(msg: String) extends RuntimeException(msg)

}

object Counter {

  case class UseStorage(storage: Option[ActorRef])

}

/**
 * The in memory count variable that will send current
 * value to the `Storage`, if there is any storage
 * available at the moment.
 */
class Counter(key: String, initialValue: Long) extends Actor {

  import com.example.Counter._
  import com.example.CounterService._
  import com.example.Storage._

  var count = initialValue
  var storage: Option[ActorRef] = None

  override def receive: Actor.Receive = LoggingReceive {
    case UseStorage(s) =>
      storage = s
      storeCount()

    case Increment(n) =>
      count += n
      storeCount()

    case GetCurrentCount => sender() ! CurrentCount(key, count)
  }

  def storeCount(): Unit = {
    // Delegate dangerous work, to protect our valuable state.
    // We can continue without storage.
    storage foreach {
      _ ! Store(Entry(key, count))
    }
  }
}

/**
 * Saves key/value pairs to persistent storage when receiving `Store` message.
 * Replies with current value when receiving `Get` message.
 * Will throw StorageException if the underlying data store is out of order.
 */
class Storage extends Actor {

  import com.example.Storage._

  val db = DummyDB

  override def receive: Actor.Receive = LoggingReceive {
    case Store(Entry(key, value)) => db.save(key, value)
    case Get(key) => sender() != Entry(key, db.load(key).getOrElse(0L))
  }
}

object DummyDB {
  private var db = Map[String, Long]()

  def save(key: String, value: Long): Unit = synchronized {
    if (11 <= value && value <= 14)
      throw new StorageException("Simulated Storage Failure:" + value)
    db += (key -> value)
  }

  @throws(classOf[StorageException])
  def load(key: String): Option[Long] = synchronized {
    db.get(key)
  }
}