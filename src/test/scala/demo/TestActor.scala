package demo

import akka.actor.{ Actor, ActorRef, Props }
import com.fortysevendeg.lambdatest._
import com.fortysevendeg.lambdatestasync._

object A {

  case object Ping

  case object Pong0

  case object Pong1

  case class Count(i: Int)

  case object Ack

  def props(b: ActorRef) = Props(new A(b))

}

class A(b: ActorRef) extends Actor {

  import A._

  var even = true
  var count = 0

  override def receive: Receive = {
    case Ping ⇒
      val result = if (even) Pong0 else Pong1
      sender ! result
      even = !even
      count += 1
      b ! Count(count)
    case Ack ⇒
    case _ ⇒
  }
}

object B {
  def props = Props(new B)
}

class B extends Actor {

  import A._

  def receive = {
    case Count(i) ⇒ sender ! Ack
  }
}

// format: OFF

class TestActor extends LambdaTest {

  import A._

  def act = {
    actorSystemWrap() { implicit system =>

      test("actors") {
        val (p, pEvents) = Probe("p")
        val (b, bEvents) = Intercept("b", B.props)
        val a = system.actorOf(A.props(b))
        val range = (1 to 4).toList
        expectEvents(pEvents, "probe messages", show = msgShow) {
          expectEvents(bEvents, "b actor messages", show = msgShow) {
            exec {
              for (i <- range) p ! SendMsg(a, Ping)
            }
          } {
            MsgHasExact(range.map(Count(_)))
          }
        } {
          val s = for (i <- range) yield (if (i % 2 == 1) Pong0 else Pong1)
          MsgHasExact(s)
        }
      }
    }
  }
}

object TestActor {
  def main(args: Array[String]): Unit = {
    run("actorexpect", new TestActor)
  }
}
