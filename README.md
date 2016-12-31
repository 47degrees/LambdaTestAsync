# LambdaTestAsync

[![Maven Central](https://img.shields.io/maven-central/v/com.fortysevendeg/lambda-test-async_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/com.fortysevendeg/lambda-test-async_2.12)

LambdaTestAsync is an extension to 
[LambdaTest](https://github.com/47deg/LambdaTest). 
LambdaTest is a small clean easily extensible functional testing library for Scala.
LambdaTestAsync extends LambdaTest with support for asynchronous testing.

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

## Logged Messages

