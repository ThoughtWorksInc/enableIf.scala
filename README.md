# enableIf.scala <a href="http://thoughtworks.com/"><img align="right" src="https://www.thoughtworks.com/imgs/tw-logo.png" title="ThoughtWorks" height="15"/></a>

[![Join the chat at https://gitter.im/ThoughtWorksInc/enableIf.scala](https://badges.gitter.im/ThoughtWorksInc/enableIf.scala.svg)](https://gitter.im/ThoughtWorksInc/enableIf.scala?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/ThoughtWorksInc/enableIf.scala.svg)](https://travis-ci.org/ThoughtWorksInc/enableIf.scala)
[![Latest version](https://index.scala-lang.org/thoughtworksinc/enableif.scala/enableif/latest.svg)](https://index.scala-lang.org/thoughtworksinc/enableif.scala)
[![Scaladoc](https://javadoc.io/badge/com.thoughtworks.enableIf/enableif_2.12.svg?label=scaladoc)](https://javadoc.io/page/com.thoughtworks.enableIf/enableif_2.12/latest/com/thoughtworks/index.html)

**enableIf.scala** is a library that switches Scala code at compile-time, like `#if` in C/C++.

## Motivation

Suppose you want to create a library for both Scala 2.10 and Scala 2.11. When you implement the library, you may want to call [the `flatMap` method on `TailRec`](http://www.scala-lang.org/api/current/scala/util/control/TailCalls$$TailRec.html). However, the method does not exist on Scala 2.10.

With the help of this library, You can create your own implementation of `flatMap` for Scala 2.10 target, and the Scala 2.11 target should still use the `flatMap` method implemented by Scala standard library.

## Macros
| Name | Description |
|------|-------------|
| @enableIf | switches single member via predicates |
| @enableMembersIf | switches all members via predicates |
| @enableWithClasspath | switches single member via classpath regex |
| @enableWithArtifact | switches single member via artifactId and version |

## Usage

### Step 1: Add the library dependency in your `build.sbt`

``` sbt
// Enable macro annotation by scalac flags for Scala 2.13
scalacOptions ++= {
  import Ordering.Implicits._
  if (VersionNumber(scalaVersion.value).numbers >= Seq(2L, 13L)) {
    Seq("-Ymacro-annotations")
  } else {
    Nil
  }
}

// Enable macro annotation by compiler plugins for Scala 2.12
libraryDependencies ++= {
  import Ordering.Implicits._
  if (VersionNumber(scalaVersion.value).numbers >= Seq(2L, 13L)) {
    Nil
  } else {
    Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
  }
}

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

 * The `enableIf` annotation does not work for top level traits, classes and objects.
 * The boolean condition been evaluated must refer `class`s or `object`s via fully quantified names from dependency libraries
 * The boolean condition been evaluated must not refer other `class`s or `object`s from the same library.

## Enable different code for Scala.js and JVM targets

Suppose you want to create a Buffer-like collection, you may want create an `ArrayBuffer` for JVM target, and the native `js.Array` for Scala.js target.

``` scala
/**
 * Enable members in `Jvm` if no Scala.js plugin is found (i.e. Normal JVM target)
 */
@enableMembersIf(c => !c.compilerSettings.exists(_.matches("""^-Xplugin:.*scalajs-compiler_[0-9\.\-]*\.jar$""")))
private object Jvm {
  
  def newBuffer[A] = collection.mutable.ArrayBuffer.empty[A]
  
}


/**
 * Enable members in `Js` if a Scala.js plugin is found (i.e. Scala.js target)
 */
@enableMembersIf(c => c.compilerSettings.exists(_.matches("""^-Xplugin:.*scalajs-compiler_[0-9\.\-]*\.jar$""")))
private object Js {

  @inline def newBuffer[A] = new scalajs.js.Array[A]

  @inline implicit final class ReduceToSizeOps[A] @inline()(array: scalajs.js.Array[A]) {
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

## Enable different code for Apache Spark 3.1.x and 3.2.x
For breaking API changes of 3rd-party libraries, simply annotate the target method with the artifactId and the version to make it compatible.

Sometimes, we need to use the regex to match the rest part of a dependency's classpath. For example, `"3\\.2.*".r` below will match `"3.2.0.jar"`.
``` scala
object XYZ {
  @enableWithArtifact("spark-catalyst_2.12", "3\\.2.*".r)
  private def getFuncName(f: UnresolvedFunction): String = {
    // For Spark 3.2.x
    f.nameParts.last
  }
  
  @enableWithArtifact("spark-catalyst_2.12", "3\\.1.*".r)
  private def getFuncName(f: UnresolvedFunction): String = {
    // For Spark 3.1.x
    f.name.funcName
  }
}
```

The rest part regex could also be used to identify classifiers. Take `"org.bytedeco" % "ffmpeg" % "5.0-1.5.7"` for example:

```
ffmpeg-5.0-1.5.7-android-arm-gpl.jar
ffmpeg-5.0-1.5.7-android-arm.jar
ffmpeg-5.0-1.5.7-android-arm64.jar
ffmpeg-5.0-1.5.7-linux-arm64-gpl.jar
...
```

If there is a key difference between gpl and non-gpl implementation, the following macro might be used:
``` scala
@enableWithArtifact("ffmpeg", "5.0-1.5.7-.*-gpl.jar")
```

If `@enableWithArtifact` is not flexible enough for you to identify the specific dependency, please use `@enableWithClasspath`.

Hints to show the full classpath:
``` bash
sbt "show Compile / fullClasspath"

mill show foo.compileClasspath
```

