package com.thoughtworks

import utest._

/**
  * @author 杨博 (Yang Bo) &lt;pop.atry@gmail.com&gt;
  */
object EnableMembersIfTest extends TestSuite {

  def tests = this {

    "Boolean condition" - {

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

    "Boolean condition on Class" - {

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

}
