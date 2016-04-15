# enableIf.scala

[![Build Status](https://travis-ci.org/ThoughtWorksInc/enableIf.scala.svg?branch=master)](https://travis-ci.org/ThoughtWorksInc/enableIf.scala)

**enableIf.scala** is a library that switches Scala code at compile-time, like `#if` in C/C++.

## Motivation

Suppose you want to create a library for both Scala 2.10 and Scala 2.11. When you implement the library, you may want to call [the `flatMap` method on `TailRec`](http://www.scala-lang.org/api/current/scala/util/control/TailCalls$$TailRec.html). However, the method does not exist on Scala 2.10.

With the help of this library, I can create my own implementation of `flatMap` for Scala 2.10 target, and the Scala 2.11 target should still use the `flatMap` method implemented by Scala standard library.

## Usage

### Step 1: Add the library dependency in your `build.sbt`

```
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

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

## Enable different code for Scala.js and JVM targets

Suppose you want to create a Buffer-like collection, you may want create an `scala.collection.mutable.ArrayBuffer` for JVM target, and the native `js.Array` for Scala.js target.

``` scala
private object Jvm {
  
  /**
   * Enable if no Scala.js plugin is found (i.e. Normal JVM target)
   */
  @enableIf(c => !c.compilerSettings.exists(_.matches("""^-Xplugin:.*scalajs-compiler_[0-9\.\-]*\.jar$""")))
  def newBuffer[A] = collection.mutable.ArrayBuffer.empty[A]
  
}

private object Js {

  /**
   * Enable if a Scala.js plugin is found (i.e. Scala.js target)
   */
  @inline
  @enableIf(c => c.compilerSettings.exists(_.matches("""^-Xplugin:.*scalajs-compiler_[0-9\.\-]*\.jar$""")))
  def newBuffer[A] = new scalajs.js.Array[A]

  /**
   * Enable if a Scala.js plugin is found (i.e. Scala.js target)
   */
  @enableIf(c => c.compilerSettings.exists(_.matches("""^-Xplugin:.*scalajs-compiler_[0-9\.\-]*\.jar$""")))
  @inline
  implicit final class ReduceToSizeOps[A] @inline()(array: scalajs.js.Array[A]) {
    @inline def reduceToSize(newSize: Int) = array.length = newSize
  }

}

import Js._
import Jvm._

val optimizedBuffer = newBuffer[Int]

optimizedBuffer += 1
optimizedBuffer += 2
optimizedBuffer += 3

// resolved to native ArrayBuffer.reduceToSize for JVM, implicitly converted to ReduceToSizeOps for Scala.js
optimizedBuffer.reduceToSize(1)
```

You can define a `c` parameter because the `enableIf` annotation accepts either a `Boolean` expression or a `scala.reflect.macros.Context => Boolean` function. You can extract information from the macro context `c`.
