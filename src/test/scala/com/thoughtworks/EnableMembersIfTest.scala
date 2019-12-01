package com.thoughtworks

import org.scalatest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

/**
  * @author 杨博 (Yang Bo) &lt;pop.atry@gmail.com&gt;
  */
object EnableMembersIfTest extends AnyFreeSpec with Matchers {


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
  }
