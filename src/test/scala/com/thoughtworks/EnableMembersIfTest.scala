package com.thoughtworks

import com.thoughtworks.enableIf.{classpathMatchesArtifact, crossScalaBinaryVersion}
import org.scalatest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

/** @author
  *   杨博 (Yang Bo) &lt;pop.atry@gmail.com&gt;
  */
class EnableMembersIfTest extends AnyFreeSpec with Matchers {

  "Boolean condition" in {

    @enableMembersIf(true)
    object ShouldEnable {
      def whichIsEnabled = "good"
    }
    @enableMembersIf(false)
    object ShouldDisable {
      def whichIsEnabled = "bad"
    }

    import ShouldEnable._
    import ShouldDisable._
    assert(whichIsEnabled == "good")

  }

  "Boolean condition on Class" in {

    @enableMembersIf(false)
    class ShouldDisableClassOnly

    object ShouldDisableClassOnly {
      def whichIsEnabled = "good"
    }
    @enableMembersIf(false)
    object ShouldDisable {
      def whichIsEnabled = "bad"
    }

    import ShouldDisableClassOnly._
    import ShouldDisable._
    assert(whichIsEnabled == "good")

  }

  "Test Artifact and " in {
    @enableMembersIf(classpathMatchesArtifact(crossScalaBinaryVersion("quasiquotes"), "2.1.1"))
    object ShouldEnable {
      def whichIsEnabled = "good"
    }

    @enableMembersIf(classpathMatchesArtifact("scala-library", "2\\.1[123]\\..*".r))
    object ShouldDisable1 {
      def whichIsEnabled = "bad"
    }

    @enableMembersIf(classpathMatchesArtifact("scala", "2\\.1[123]\\..*".r))
    object ShouldDisable2 {
      def whichIsEnabled = "bad"
    }

    import ShouldEnable._
    import ShouldDisable1._
    import ShouldDisable2._

    if (scala.util.Properties.versionNumberString < "2.11") {
      assert(whichIsEnabled == "good")
    } else {
      assert(whichIsEnabled == "bad")
    }
  }
}
