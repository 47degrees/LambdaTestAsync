package com.fortysevendeg.lambdatestasync

import akka.actor.{ Actor, Props }
import com.persist.JsonOps._

private[lambdatestasync] class EventLogAppenderActor extends Actor {
  import EventLogAppenderActor._
  private var senders = Map.empty[String, (Json) ⇒ Unit]

  override def receive: Receive = {
    case Subscribe(done, send, uid) ⇒
      senders += uid → send
      done()
    case Unsubscribe(done, uid) ⇒
      senders -= uid
      done()
    case Send(msg) ⇒
      for ((uid, sender) ← senders) sender(msg)
  }
}

private[lambdatestasync] object EventLogAppenderActor {

  case class Subscribe(done: () ⇒ Unit, send: (Json) ⇒ Unit, uid: String)
  case class Unsubscribe(done: () ⇒ Unit, uid: String)
  case class Send(msg: Map[String, Any])

  def props = Props(new EventLogAppenderActor)

}