package com.thoughtworks

import org.scalatest._
import enableIf._

import scala.util.control.TailCalls._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers


/**
 * @author 沈达 (Darcy Shen) &lt;sadhen@zoho.com&gt;
 */
class EnableWithArtifactTest extends AnyFreeSpec with Matchers {
  "Test if we are using quasiquotes explicitly" in {

    object ExplicitQ {

      @enableIf(classpathMatchesArtifact(crossScalaBinaryVersion("quasiquotes"), "2.1.1"))
      def whichIsEnabled = "good"
    }
    object ImplicitQ {
      @enableIf(classpathMatchesArtifact("scala-library", "2\\.1[123]\\..*".r))
      def whichIsEnabled = "bad"

      @enableIf(classpathMatchesArtifact("scala", "2\\.1[123]\\..*".r))
      def whichIsEnabled = "bad"
    }


    import ExplicitQ._
    import ImplicitQ._
    if (scala.util.Properties.versionNumberString < "2.11") {
      assert(whichIsEnabled == "good")
    } else {
      assert(whichIsEnabled == "bad")
    }
  }

  "Add TailRec.flatMap for Scala 2.10 " in {

    @enableIf(classpathMatchesArtifact("scala-library", "2\\.10.*".r))
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
