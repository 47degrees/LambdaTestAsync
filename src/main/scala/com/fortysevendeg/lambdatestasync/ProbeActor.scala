package com.fortysevendeg.lambdatestasync

import akka.actor.{ Actor, ActorRef, Props }

import scala.concurrent.Promise

private[lambdatestasync] class ProbeActor(p: Promise[Events[Any]], to: Option[ActorRef]) extends Actor {

  private var senders = Map.empty[String, (Any) ⇒ Unit]

  private object E extends Events[Any] {

    override def subscribe(done: () ⇒ Unit, send: (Any) ⇒ Unit): String = {
      def uid = java.util.UUID.randomUUID.toString
      senders += uid → send
      done()
      uid
    }

    override def unsubscribe(done: () ⇒ Unit, uid: String): Unit = {
      senders -= uid
      done()
    }
  }

  p.trySuccess(E)

  // TODO to does not capture underlying actor response!!!

  def receive: Receive = {
    case SendMsg(r, msg) ⇒ r ! msg
    case msg: Any ⇒
      for ((uid, sender) ← senders) {
        sender(msg)
      }
      to match {
        case Some(a) ⇒ a forward msg
        case None ⇒
      }
  }
}

private[lambdatestasync] object ProbeActor {

  def props(p: Promise[Events[Any]], to: Option[ActorRef]) = Props(new ProbeActor(p, to))
}