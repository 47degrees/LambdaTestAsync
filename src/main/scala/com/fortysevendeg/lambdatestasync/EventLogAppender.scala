package com.fortysevendeg.lambdatestasync

import akka.actor.ActorRefFactory
import com.persist.JsonOps._
import com.persist.logging.{ LogAppender, LogAppenderBuilder, RichMsg }
import akka.pattern.gracefulStop
import scala.concurrent.duration._

import scala.concurrent.{ Future, Promise }

private[lambdatestasync] case class EventLogAppenderBuilder(result: Promise[Events[Json]]) extends LogAppenderBuilder {

  override def apply(factory: ActorRefFactory, standardHeaders: Map[String, RichMsg]): LogAppender = {
    val ela = new EventLogAppender(factory, standardHeaders)
    result.trySuccess(ela)
    ela
  }
}

private[lambdatestasync] class EventLogAppender(factory: ActorRefFactory, standardHeaders: Map[String, RichMsg]) extends Events[Json] with LogAppender {

  import EventLogAppenderActor._

  val eventLogActor = factory.actorOf(EventLogAppenderActor.props)

  override def subscribe(done: () ⇒ Unit, send: (Json) ⇒ Unit): String = {
    def uid = java.util.UUID.randomUUID.toString
    eventLogActor ! Subscribe(done, send, uid)
    uid
  }

  override def unsubscribe(done: () ⇒ Unit, uid: String): Unit = {
    eventLogActor ! Unsubscribe(done, uid)
  }

  override def append(baseMsg: Map[String, RichMsg], category: String): Unit = {
    val msg = baseMsg
    eventLogActor ! Send(msg)
  }

  override def finish(): Future[Unit] = Future.successful(())

  override def stop(): Future[Unit] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    gracefulStop(eventLogActor, 2.seconds).map(b ⇒ ())
  }
}
