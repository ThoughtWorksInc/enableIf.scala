package com.thoughtworks

import org.scalatest._
import enableIf._

import scala.util.control.TailCalls._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

/** @author
  *   杨博 (Yang Bo) &lt;pop.atry@gmail.com&gt;
  */
class EnableIfTest extends AnyFreeSpec with Matchers {

  "Boolean condition" in {

    object ShouldEnable {

      @enableIf(true)
      def whichIsEnabled = "good"

    }
    object ShouldDisable {

      @enableIf(false)
      def whichIsEnabled = "bad"
    }

    import ShouldEnable._
    import ShouldDisable._
    assert(whichIsEnabled == "good")

  }

  "Function condition" in {
    object ShouldEnable {

      @enableIf({ c => true })
      object whichIsEnabled {
        def innerMethod = "good"
      }

    }
    object ShouldDisable {

      @enableIf({ c =>
        false
      })
      def whichIsEnabled = 2
    }

    import ShouldEnable._
    import ShouldDisable._
    assert(whichIsEnabled.innerMethod == "good")

  }

  "Add TailRec.flatMap for Scala 2.10 " in {

    @enableIf(scala.util.Properties.versionNumberString.startsWith("2.10."))
    implicit class FlatMapForTailRec[A](underlying: TailRec[A]) {
      final def flatMap[B](f: A => TailRec[B]): TailRec[B] = {
        tailcall(f(underlying.result))
      }
    }

    def ten = done(10)

    def tenPlusOne = ten.flatMap(i => done(i + 1))

    assert(tenPlusOne.result == 11)

  }
}
