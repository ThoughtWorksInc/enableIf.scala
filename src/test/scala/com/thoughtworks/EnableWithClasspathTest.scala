package com.thoughtworks

import org.scalatest._
import enableIf._

import scala.util.control.TailCalls._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

/** @author
  *   沈达 (Darcy Shen) &lt;sadhen@zoho.com&gt;
  */
class EnableWithClasspathTest extends AnyFreeSpec with Matchers {

  "enableWithClasspath by regex" in {

    object ShouldEnable {

      @enableIf(classpathMatches(".*scala.*".r))
      def whichIsEnabled = "good"

    }
    object ShouldDisable {

      @enableIf(classpathMatches(".*should_not_exist.*".r))
      def whichIsEnabled = "bad"
    }

    import ShouldEnable._
    import ShouldDisable._
    assert(whichIsEnabled == "good")

  }

  "Add TailRec.flatMap for Scala 2.10 " in {

    @enableIf(classpathMatches(".*scala-library-2.10.*".r))
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
