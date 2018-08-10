organization := "com.thoughtworks.enableIf"

name := "enableIf"

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5-M1" % Test

testFrameworks += new TestFramework("utest.runner.Framework")

libraryDependencies ++= {
  if (scalaBinaryVersion.value == "2.10") {
    Seq("org.scalamacros" %% "quasiquotes" % "2.1.1")
  } else {
    Nil
  }
}

crossScalaVersions in ThisBuild := Seq("2.10.7", "2.11.12", "2.12.6", "2.13.0-M3")

startYear := Some(2016)
