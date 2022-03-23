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

  "enableWithArtifact by artifactId and version" in {

    object ShouldEnable {

      @enableWithArtifact("scala", scala.util.Properties.versionNumberString)
      def whichIsEnabled = "good"

      /**
       * sbt is using the self-managed scala library, that's why we are using
       * `scala` as artifactId and `2.12.15` as the version string
       * eg. $HOME/.sbt/boot/scala-2.12.15/lib/scala-library.jar
       *
       * For most usages of enableWithArtifact, 3rd-party libraries should be used
       */
      @enableWithArtifact("scala-library", scala.util.Properties.versionNumberString)
      def whichIsEnabled = "good"
    }
    object ShouldDisable {

      @enableWithArtifact("scala-library", "0.0.0")
      def whichIsEnabled = "bad"
    }

    import ShouldEnable._
    import ShouldDisable._
    assert(whichIsEnabled == "good")

  }

  "Test if we are using quasiquotes explicitly" in {

    object ExplicitQ {

      @enableWithArtifact("quasiquotes", "2.1.1")
      def whichIsEnabled = "good"
    }
    object ImplicitQ {
      @enableWithArtifact("scala-library", "2\\.1[123]\\..*".r)
      def whichIsEnabled = "bad"

      @enableWithArtifact("scala", "2\\.1[123]\\..*".r)
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

    @enableWithArtifact("scala-library", "2\\.10.*".r)
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
