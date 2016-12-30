package com.fortysevendeg.lambdatestasync

/**
  * Trait for sequences of events.
  * @tparam T
  */
trait Events[T] {

  /**
    * Subscribe to an event sequence.
    * @param done called when the event subscribe is about to start.
    * @param send sends the next event.
    * @return a unique id.
    */
  def subscribe(done: () ⇒ Unit, send: (T) ⇒ Unit): String

  /**
    * Unsubscribe from a sequence of events.
    * @param done called when the unsubscribe is complete.
    * @param uid  the unique id returned by subscribe.
    */
  def unsubscribe(done: () ⇒ Unit, uid: String)

}

