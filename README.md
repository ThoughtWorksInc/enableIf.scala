# enableIf.scala

[![Build Status](https://travis-ci.org/ThoughtWorksInc/enableIf.scala.svg?branch=master)](https://travis-ci.org/ThoughtWorksInc/enableIf.scala)

**enableIf.scala** is a library that switches Scala code at compile-time, like `#if` in C/C++.

## Motivation

Suppose you want to create a library for both Scala 2.10 and Scala 2.11. When you implement the library, you may want to call [the `flatMap` method on `TailRec`](http://www.scala-lang.org/api/current/scala/util/control/TailCalls$$TailRec.html). However, the method does not exist on Scala 2.10.

With the help of this library, I can create my own implementation of `flatMap` for Scala 2.10 target, and the Scala 2.11 target should still use the `flatMap` method implemented by Scala standard library.

## Usage

### Step 1: Add the library dependency in your `build.sbt`

```
libraryDependencies += "com.thoughtworks.enableIf" %% "enableif" % "latest.release"
```

### Step 2: Create an implicit class for Scala 2.10

``` scala
import com.thoughtworks.enableIf

@enableIf(scala.util.Properties.versionNumberString.startsWith("2.10."))
implicit class FlatMapForTailRec[A](underlying: TailRec[A]) {
  final def flatMap[B](f: A => TailRec[B]): TailRec[B] = {
    tailcall(f(underlying.result))
  }
}
```

The `@enableIf` annotation accepts a `Boolean` expression that indicates if the `FlatMapForTailRec` definition should be compiled. The `Boolean` expression is evaluated at compile-time instead of run-time.

### Step 3: Call the `flatMap` method on your `TailRec`

``` scala
import scala.util.control.TailCalls._
def ten = done(10)
def tenPlusOne = ten.flatMap(i => done(i + 1))
assert(tenPlusOne.result == 11)
```

For Scala 2.10, the expression `scala.util.Properties.versionNumberString.startsWith("2.10.")` is evaluated to `true`, hence the `FlatMapForTailRec` definition will be enabled. As a result, `ten.flatMap` will call to `flatMap` of the implicit class `FlatMapForTailRec`.

For Scala 2.11, the expression `scala.util.Properties.versionNumberString.startsWith("2.10.")` is evaluated to `false`, hence the `FlatMapForTailRec` definition will be disabled. As a result, `ten.flatMap` will call the native `TailRec.flatmap`.

## Limitation

The `enableIf` annotation does not work for top level traits, classes and objects.
