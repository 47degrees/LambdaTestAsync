package demo

import com.fortysevendeg.lambdatest._
import com.fortysevendeg.lambdatestasync._
import com.persist.logging._

// format: OFF

class TestLog extends LambdaTest with ClassLogging {

  def act = {
    actorSystemWrap() { implicit system => logWrap { logEvents =>
      test("Event") {
        expectEvents(logEvents, "log test", show = logShow) {
          exec {
            log.error(Map("msg" -> "test", "count" -> 10))
            log.warn("hello")
          }
        } {
          LogHas(List(Map("@severity" -> "WARN", "msg" -> "hello")))
        }
      }
    }
    }
  }
}

object TestLog {
  def main(args: Array[String]): Unit = {
    run("logexpect", new TestLog)
  }
}
