# LambdaTestAsync

[![Maven Central](https://img.shields.io/maven-central/v/com.fortysevendeg/lambda-test-async_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/com.fortysevendeg/lambda-test-async_2.12)

LambdaTestAsync is an extension to 
[LambdaTest](https://github.com/47deg/LambdaTest). 
LambdaTest is a small clean easily extensible functional testing library for Scala.
LambdaTestAsync extends LambdaTest with support for asynchronous testing.

You should understand the features of the base LambdaTest system before 
reading the documentation below.

## Jar File

Include LambdaTestAsync jar

    "com.fortysevendeg" % "lambda-test-sync_2.12" % "1.2.1" % "test"
    
## Event Streams

LambdaTestAsync is based on streams of asynchronous events. Assertions test that 
these streams have specified properties.

There is a general purpose set of features that can be used on any kind of event streams.
Two specializations are provided:

1. Akka actor messages.
2. Logged messages.

## Actor Messages

See the [TestActor](https://github.com/47deg/LambdaTestAsync/blob/master/src/test/scala/demo/TestActor.scala) 
demo. Here we are testing a stateful Actor A that responds to messages sent to it and also sends messages to another
Actor B.

To set up the Akka actor system, the `actorSystemWrap` wrapper is used.

A Probe is used to send messages to A and to record its responses in event stream pEvents.
An Intercept is used between A and B to record messages sent from A to B in event stream bEvents.

The test is done with the `expectEvents` compound assertion action. Note the optional `show` parameter that
when set prints out the sequence of events. The test has two parts. The first part is the body that will cause 
the events to be generated. When the `SendMsg(a,Ping)` is sent to the probe, the probe sends the message `Ping` to A.
The second part is the test. Here each list of messages must exectly match a specified list.

## More on Tests

There are two kinds of build-in tests. 

* **`HasExact`**. the list of events must exactly match a specified list.
* **`Has`**. the list of events must contain all the elements of a specified list in order. The list of events may also contain other events.

It is also possible to write your own tests. Each custom test should extend the `EventTest[T]` trait where `T` is the type of the events.
You will need to implement the `test` method of `EventTest`. This method is called once for each event and once after all events (typically as the result of a timeout). It has two parameters.

* **`events`**. The list of all events so far in reverse order (the head will be the most recent event).
* **`done`**. Set to true only on the last call.

There are three possible results.

* **`Success`**. The test succeeded. No further processing is needed.
* **`Fail`**. The test failed. No further processing is needed. Fail includes a text message that describes why the test failed.
* **`Continue`**. Keep testing. (If done is true, the test will fail, since there is nothing to continue to).

## Logged Messages

See the [TestLog](https://github.com/47deg/LambdaTestAsync/blob/master/src/test/scala/demo/TestLog.scala) 
demo.

## Extensions
