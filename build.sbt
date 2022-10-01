organization := "com.thoughtworks.enableIf"

name := "enableIf"

libraryDependencies ++= {
  if (
    VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector(">=2.13"))
  ) {
    Nil
  } else {
    Seq(
      compilerPlugin(
        "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
      )
    )
  }
}

scalacOptions ++= {
  if (
    VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector(">=2.13"))
  ) {
    Seq("-Ymacro-annotations")
  } else {
    Nil
  }
}

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.13" % Test

libraryDependencies ++= {
  if (scalaBinaryVersion.value == "2.10") {
    Seq("org.scalamacros" %% "quasiquotes" % "2.1.1")
  } else {
    Nil
  }
}

startYear := Some(2016)
