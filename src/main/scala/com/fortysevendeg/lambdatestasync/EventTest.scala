package com.fortysevendeg.lambdatestasync

import com.persist.JsonOps._

/**
  * Tests a sequence of events.
  *
  * @tparam T the type of the events.
  */
trait EventTest[T] {
  /**
    * The incremental event test.
    *
    * @param events the list of events so far.
    * @param done   true if there are no more events.
    * @return whether to succeed, failor continue.
    */
  def test(events: List[T], done: Boolean): Result

  /**
    * Ands two event tests.
    *
    * @param that the other event.
    * @return the and of the two tests.
    */
  final def &(that: EventTest[T]) = new EventTest[T] {
    def test(events: List[T], done: Boolean): Result = {
      val r1 = this.test(events, done)
      if (r1 == Success) {
        that.test(events, done)
      } else {
        r1
      }
    }
  }

  /**
    * Ors two event tests.
    *
    * @param that the other event.
    * @return the or of the two tests.
    */
  final def |(that: EventTest[T]) = new EventTest[T] {
    def test(events: List[T], done: Boolean): Result = {
      val r1 = this.test(events, done)
      r1 match {
        case Fail(msg) ⇒ that.test(events, done)
        case _ ⇒ r1
      }
    }
  }

}

/**
  * Helper methods for writing event tests.
  */
object EventTest {

  private def tailOption[T](events: List[T]): Option[List[T]] = {
    events match {
      case h +: t ⇒ Some(t)
      case _ ⇒ None
    }
  }

  /**
    * Looks for an exact match between a list of events and a list of patterns.
    *
    * @param patterns the list of patterns.
    * @param events   the list of events.
    * @param compare  compares a pttern to an event.
    * @tparam P the type of the patterns.
    * @tparam T the type of the events.
    * @return true if a match is found.
    */
  def hasExact[P, T](patterns: List[P], events: List[T], compare: (P, T) ⇒ Boolean): Boolean = {
    if (patterns.size != events.size) {
      false
    } else {
      (patterns zip events).forall {
        case (p, e) ⇒ compare(p, e)
      }
    }
  }

  /**
    * Checks that a list of patterns appear in order with a list of events.
    * The may be other events before, after and between matching patterns.
    *
    * @param patterns the list of patterns.
    * @param events   the list of events.
    * @param compare  compares a pattern to an event.
    * @tparam P the type of patterns.
    * @tparam T the type of events.
    * @return true if a match is found.
    */
  def has[P, T](patterns: List[P], events: List[T], compare: (P, T) ⇒ Boolean): Boolean = {
    if (events.size < patterns.size) {
      false
    } else if (patterns.size == 1) {
      events.exists(e ⇒ compare(patterns.head, e))
    } else {
      val rest = patterns.foldLeft[Option[List[T]]](Some(events)) {
        case (events1, p) ⇒
          events1.flatMap {
            case events2 ⇒ tailOption(events2.dropWhile(!compare(p, _)))
          }
      }
      rest != None
    }
  }

  /**
    * Compares a Json pattern to a Json message.
    * Each field of the pattern must occur within the message.
    *
    * @param pattern the pattern Json.
    * @param msg     the message Json.
    * @return true if there is a match.
    */
  def LogMatch(pattern: JsonObject, msg: Json): Boolean = {
    pattern.forall {
      case (field, value) ⇒ jget(msg, field) == value
    }
  }

}

/**
  * Checks that a list of patterns can be matched within a list
  * of log messages.
  *
  * @param patterns the list of patterns.
  */
case class LogHas(patterns: List[JsonObject]) extends EventTest[Json] {
  val patterns1 = patterns.reverse

  /**
    * Tests that log messages are matched by the patterns.
    *
    * @param events the list of log messages so far.
    * @param last   true if there are no more log messages.
    * @return success, failure or continue.
    */
  def test(events: List[Json], last: Boolean) = {
    if (!last) {
      if (EventTest.has(patterns1, events, EventTest.LogMatch)) {
        Success
      } else {
        Continue
      }
    } else {
      Fail("pattern not found")
    }
  }
}

/**
  * Checks for an exact Json pattern match on a list of log messages.
  *
  * @param patterns the list of patterns.
  */
case class LogHasExact(patterns: List[JsonObject]) extends EventTest[Json] {
  val patterns1 = patterns.reverse

  /**
    * Tests that that log messages are matched by the patterns.
    *
    * @param events the list of events so far.
    * @param last   true if there are no more log messages.
    * @return whether to succeed, fail, or continue
    */
  def test(events: List[Json], last: Boolean) = {
    if (!last) {
      if (EventTest.hasExact(patterns1, events, EventTest.LogMatch)) {
        Success
      } else {
        Continue
      }
    } else {
      Fail("pattern not found")
    }
  }
}

/**
  * Checks that a list of patterns can be matched within a list
  * of event messages.
  *
  * @param patterns the list of patterns
  */
case class MsgHas(patterns: List[Any]) extends EventTest[Any] {
  val patterns1 = patterns.reverse

  /**
    * Tests that the events are matched by the patterns.
    *
    * @param events the list of events so far.
    * @param last  true if there are no more events.
    * @return whether to succeed, fail, or continue
    */
  def test(events: List[Any], last: Boolean) = {
    if (!last) {
      if (EventTest.has(patterns1, events, (x: Any, y: Any) ⇒ x == y)) {
        Success
      } else {
        Continue
      }
    } else {
      Fail("pattern not found")
    }
  }
}

/**
  * Checks for an exact match of list of event messages against a list of patterns.
  *
  * @param patterns the list of patterns.
  */
case class MsgHasExact(patterns: List[Any]) extends EventTest[Any] {
  private val patterns1 = patterns.reverse

  /**
    * Test that the events exactly match the pattern.
    *
    * @param events the list of events.
    * @param last   true if this is the last call.
    * @return whether to succeed, fail, or continue
    */
  def test(events: List[Any], last: Boolean): Result = {
    if (!last) {
      if (EventTest.hasExact(patterns1, events, (x: Any, y: Any) ⇒ x == y)) {
        Success
      } else {
        Continue
      }
    } else {
      Fail("pattern not found")
    }
  }
}

