package com.fortysevendeg.lambdatestasync

import akka.actor.{ Actor, Props }

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Promise }

private[lambdatestasync] class EventActor[T](
  test: EventTest[T],
  info: String,
  timeout: FiniteDuration,
  pr1: Promise[(Boolean, String)],
  pr2: Promise[List[T]]
) extends Actor {

  import EventActor._

  implicit val ec: ExecutionContext = context.dispatcher

  val stimeout = context.system.scheduler.scheduleOnce(timeout) {
    self ! Timeout
  }

  def finish = {
    ready = false
    stimeout.cancel()
  }

  def done(r: Boolean, info: String) = {
    pr1.trySuccess((r, info))
    finish
  }

  def fail(ex: Exception) = {
    pr1.tryFailure(ex)
    finish
  }

  var events = List.empty[T]
  var ready = false

  override def receive: Receive = {
    case SubscribeDone ⇒
      ready = true
    case UnsubscribeDone ⇒
      pr2.trySuccess(events)
      context.stop(self)
    case Event(event: T @unchecked) if ready ⇒
      events = event +: events
      // TODO try
      val r = test.test(events, false)
      r match {
        case Success ⇒
          done(true, info)
        case Fail(msg) ⇒
          done(false, s"$msg $info")
        case Continue ⇒
      }
    case Timeout ⇒
      // TODO try
      val r = test.test(events, true)
      r match {
        case Success ⇒
          done(true, info)
        case Fail(msg) ⇒
          done(false, s"$msg $info")
        case Continue ⇒
      }
  }
}

private[lambdatestasync] object EventActor {

  sealed trait EventActorMsgs

  case object SubscribeDone extends EventActorMsgs

  case object UnsubscribeDone extends EventActorMsgs

  case class Event[T](event: T) extends EventActorMsgs

  case object Timeout extends EventActorMsgs

  def props[T](
    test: EventTest[T],
    info: String,
    timeout: FiniteDuration,
    pr1: Promise[(Boolean, String)],
    pr2: Promise[List[T]]
  ) = Props(new EventActor[T](test, info, timeout, pr1, pr2))
}
