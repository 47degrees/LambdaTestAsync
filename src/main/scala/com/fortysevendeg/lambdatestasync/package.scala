package com.fortysevendeg

import java.net.InetAddress
import akka.actor.{ ActorRef, ActorSystem, Props }
import com.fortysevendeg.lambdatest._
import com.persist.JsonOps._
import com.persist.logging.{ FileAppender, LoggingSystem }
import scala.concurrent.{ Await, Future, Promise, TimeoutException }
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util._

package object lambdatestasync {

  /*
  private[lambdatestasync] def pos(offset: Int = 0): String = {
    val f = new Exception("foo").getStackTrace.apply(2 + offset)
    s"${f.getFileName} Line ${f.getLineNumber}"
  }
  */

  /**
    * The type of messages sent to a Probe actor.
    * @param r  the reference to the actor that the Proble actor should forward the message to.
    * @param msg the message to be forwarded.
    */
  case class SendMsg(r: ActorRef, msg: Any)

  /**
    * The incremental result of event tests.
    * Can be success, fail or continue.
    */
  sealed trait Result

  /**
    * The test succeeded.
    */
  case object Success extends Result

  /**
    * The test failed.
    *
    * @param msg a message to include in the failure report.
    */
  case class Fail(msg: String) extends Result

  /**
    * Continue incremental processing.
    */
  case object Continue extends Result

  /**
    * A wrapper LambdaTest for setting up and terminating an actor system.
    *
    * @param name name of the actor system. Default is "Wrap".
    * @param body a function whose paramter is the actor system and whose result is
    *             a LambdaAct,
    * @return the LambdaAct
    */
  def actorSystemWrap(name: String = "Wrap")(body: ActorSystem ⇒ LambdaAct): LambdaAct = {
    SingleLambdaAct(t ⇒ {
      val system = ActorSystem(name)
      try {
        body(system).eval(t)
      } finally {
        Await.result(system.terminate(), 20 seconds)
      }
    })
  }

  /**
    * Used to show Json logs.
    */
  val logShow = {
    Some {
      (j: Json) ⇒ s"log\n${Pretty(j, indent = 2)}"
    }
  }

  /**
    * Used to show actor messages.
    */
  val msgShow = {
    Some {
      (x: Any) ⇒ s"msg $x"
    }
  }

  /**
    * A wrapper LambdaAct for starting and stopping a Persist Logger.
    * @param body a function whose parameter is the the event stream of log messages and
    *             whose result is a LambdaAct.
    * @param system  the actor system (needed by the logging system).
    * @return   the LambdaAct.
    */
  def logWrap(body: Events[Json] ⇒ LambdaAct)(implicit system: ActorSystem): LambdaAct = {
    SingleLambdaAct(t ⇒ {
      val host = InetAddress.getLocalHost.getHostName
      val p = Promise[Events[Json]]
      val eventAppender = new EventLogAppenderBuilder(p)
      val loggingSystem = LoggingSystem(system, "test", "1.0.0", "localhost",
        appenderBuilders = List(FileAppender, eventAppender))
      val events = Await.result(p.future, 1.second)
      try {
        body(events).eval(t)
      } finally {
        Await.result(loggingSystem.stop, 30 seconds)
      }
    })
  }

  /**
    * A Probe is an actor through which messages are sent and an event stream that
    * captures those messages. Messages sent to this actor should be of type SendMsg.
    * @param name the name of the actor.
    * @param system the actor system. Normally supplied as an implicit parameter.
    * @return  the pair containing the actor ref and the event stream.
    */
  def Probe(name: String)(implicit system: ActorSystem): (ActorRef, Events[Any]) = {
    val p = Promise[Events[Any]]
    val ref: ActorRef = system.actorOf(ProbeActor.props(p, None), name)
    val events: Events[Any] = Await.result(p.future, 2.seconds)
    (ref, events)
  }

  /**
    * An intercept is an actor that is inserted between other actors and
    * captures the event stream of messages from the first to the second actor.
    * @param name name of the intercept actor
    * @param top properties for creating the second actor.
    * @param system the actor system. Normally supplied as an implicit parameter.
    * @return A piar containing the actor ref for the intercept actor (to be passed to the
    *         first actor) and the event stream of messages from first to second.
    */
  def Intercept(name: String, top: Props)(implicit system: ActorSystem) = {
    val p = Promise[Events[Any]]
    val to = system.actorOf(top)
    val ref: ActorRef = system.actorOf(ProbeActor.props(p, Some(to)), name)
    val events: Events[Any] = Await.result(p.future, 2.seconds)
    (ref, events)
  }

  /**
    * This compound testing action is used to test for asyncronous events.
    *
    * @param events  the stream of events.
    * @param info    a description of this test. Default is no desription.
    * @param show    if set this is used to show the sequence of events at the end of the test.
    *                For actors this would be msgShow and for logs this would be logShow.
    *                Defaults to don't show events.
    * @param timeout the timeout for the pattern to be matched. To succeed all events to be matched
    *                must have occurred before the timeout. Defaults to 1 second.
    * @param p       the source position used for reporting. Normally defaulted.
    * @param body    the body of the test. The body will cause the events to be produced.
    * @param test    the test to be done on the events.
    * @param system  the actor system. Normally passed implicitly.
    * @tparam T the event type. This will be Json for log events and Any for Actor messages.
    * @return the action.
    */
  def expectEvents[T](
    events: Events[T],
    info: String = "",
    show: Option[T ⇒ String] = None,
    timeout: FiniteDuration = 1 second,
    p: String = srcPos()
  )(body: ⇒ LambdaAct)(test: EventTest[T])(implicit system: ActorSystem): LambdaAct = {

    import EventActor._

    SingleLambdaAct {
      case t ⇒
        val pr1 = Promise[(Boolean, String)]
        val pr2 = Promise[List[T]]
        val eventActor: ActorRef = system.actorOf(EventActor.props(test, info, timeout, pr1, pr2))
        val subscribeDone = () ⇒ {
          eventActor ! SubscribeDone
        }
        val unsubscribeDone = () ⇒ {
          eventActor ! UnsubscribeDone
        }

        def send(event: T): Unit = {
          eventActor ! Event(event)
        }

        val uid = events.subscribe(subscribeDone, send)
        val t1 = body.eval(t)
        val t2 = try {
          val (r, info1) = Await.result(pr1.future, timeout + 1.seconds)
          if (r) {
            t1.success(info1, p)
          } else {
            t1.fail(info1, p)
          }
        } catch {
          case ex: Exception ⇒
            t1.unExpected(ex, p)
        }
        events.unsubscribe(unsubscribeDone, uid)
        val all = Await.result(pr2.future, 1.second)
        val t3 = {
          show match {
            case Some(f) ⇒ all.foldRight(t) {
              case (event, t2) ⇒ t2.label(s"Event:${
                f(event)
              }", exec {
              }, false, "")
            }
            case None ⇒ t2
          }
        }
        t3
    }
  }

}
