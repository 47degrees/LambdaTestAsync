package com.fortysevendeg.lambdatestasync

import akka.actor.{ Actor, Props }

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Promise }

/*
private[lambdatestexpect] class ExpectActor(
  test: ⇒ Result,
  info: String,
  first: FiniteDuration,
  next: FiniteDuration,
  timeout: FiniteDuration,
  p: Promise[(Boolean, String)]
) extends Actor {

  import ExpectActor._

  // TODO no timeout but check after each test

  implicit val ec: ExecutionContext = context.dispatcher

  var snext = context.system.scheduler.scheduleOnce(first) {
    self ! Next
  }
  val stimeout = context.system.scheduler.scheduleOnce(timeout) {
    self ! Timeout
  }

  def finish = {
    snext.cancel()
    stimeout.cancel()
    context.stop(self)

  }

  def done(r: Boolean, info: String) = {
    p.trySuccess((r, info))
    finish
  }

  def fail(ex: Exception) = {
    p.tryFailure(ex)
    finish
  }

  override def receive: Receive = {
    case Next ⇒
      try {
        // TODO limit exec time of test
        val r = test
        r match {
          case Success ⇒ done(true, info)
          case Fail(msg) ⇒ done(false, s"$msg $info")
          case Continue ⇒
            snext = context.system.scheduler.scheduleOnce(next) {
              self ! Next
            }
        }
      } catch {
        case ex: Exception ⇒
          fail(ex)
      }
    case Timeout ⇒
      done(false, s"timeout [$timeout] $info")
  }
}

private[lambdatestexpect] object ExpectActor {

  trait ExpectMessages

  case object Next extends ExpectMessages

  case object Timeout extends ExpectMessages

  def props(
    test: ⇒ Result,
    info: String,
    first: FiniteDuration,
    next: FiniteDuration,
    timeout: FiniteDuration,
    p: Promise[(Boolean, String)]
  ) = {
    Props(new ExpectActor(test, info, first, next, timeout, p))
  }
}
*/ 