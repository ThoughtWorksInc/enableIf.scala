package com.thoughtworks

import org.scalatest._
import enableIf._

import scala.util.control.TailCalls._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

/** @author
  *   沈达 (Darcy Shen) &lt;sadhen@zoho.com&gt;
  */
class EnableWithArtifactTest extends AnyFreeSpec with Matchers {
  "test the constant regex of classpath" in {
    assert {
      "/path/to/scala-library-2.10.8.jar" match {
        case classpathRegex(_, artifactId, version) =>
          "scala-library".equals(artifactId) && "2.10.8".equals(version)
      }
    }
    assert {
      "/path/to/quasiquotes_2.10-2.1.1.jar" match {
        case classpathRegex(_, artifactId, version) =>
          "quasiquotes_2.10".equals(artifactId) && "2.1.1".equals(version)
      }
    }
  }

  "Test if we are using quasiquotes explicitly" in {

    object ExplicitQ {

      @enableIf(
        classpathMatchesArtifact(
          crossScalaBinaryVersion("quasiquotes"),
          "2.1.1"
        )
      )
      def whichIsEnabled = "good"
    }
    object ImplicitQ {
      @enableIf(classpathMatches(".*scala-library-2\\.1[123]\\..*".r))
      def whichIsEnabled = "bad"

      @enableIf(classpathMatches(".*scala-2\\.1[123]\\..*".r))
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

    @enableIf(classpathMatches(".*scala-library-2\\.10.*".r))
    implicit class FlatMapForTailRec[A](underlying: TailRec[A]) {
      final def flatMap[B](f: A => TailRec[B]): TailRec[B] = {
        tailcall(f(underlying.result))
      }
    }

    def ten = done(10)

    def tenPlusOne = ten.flatMap(i => done(i + 1))

    assert(tenPlusOne.result == 11)
  }

  "Add TailRec.flatMap for Scala 2.10 via classpathContains " in {

    @enableIf(classpathContains("scala-library-2.10."))
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
