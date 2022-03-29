package com.thoughtworks

import scala.annotation.StaticAnnotation
import scala.reflect.internal.annotations.compileTimeOnly
import scala.reflect.macros.Context
import scala.util.matching.Regex


object enableIf {
  private def getRegex(artifactId: String, regex: Regex): Regex = {
    new Regex(s".*(${artifactId})-(${regex.toString})", "artifactId", "regex")
  }

  private def getRegex(artifactId: String, version: String): Regex = {
    val versionRegex = s"${version.replace(".", "\\.")}.*"
    new Regex(s".*(${artifactId})-(${versionRegex})", "artifactId", "regex")
  }

  def crossScalaBinaryVersion(artifactId: String): String = {
    val scalaBinaryVersion = scala.util.Properties
      .versionNumberString
      .split("\\.").take(2)
      .mkString(".")
    s"${artifactId}_${scalaBinaryVersion}"
  }

  def crossScalaFullVersion(artifactId: String): String = {
    val scalaFullVersion = scala.util.Properties.versionNumberString
    s"${artifactId}_${scalaFullVersion}"
  }

  def classpathContains(classpathPart: String): Context => Boolean = {
    c => c.classPath.exists(_.getPath.contains(classpathPart))
  }

  def classpathMatches(regex: Regex): Context => Boolean = {
    c => c.classPath.exists { dep =>
      regex.pattern.matcher(dep.getPath).matches()
    }
  }

  def classpathMatchesArtifact(artifactId: String, regex: Regex): Context => Boolean = {
    classpathMatches(getRegex(artifactId, regex))
  }

  def classpathMatchesArtifact(artifactId: String, version: String): Context => Boolean = {
    classpathMatches(getRegex(artifactId, version))
  }


  def isEnabled(c: Context, booleanCondition: Boolean) = booleanCondition

  def isEnabled(c: Context, functionCondition: Context => Boolean) =
    functionCondition(c)

  private[enableIf] object Macros {
    def macroTransform(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
      import c.universe._
      val Apply(Select(Apply(_, List(condition)), _), List(_@_*)) = c.macroApplication
      if (c.eval(c.Expr[Boolean](
        q"""
          import _root_.com.thoughtworks.enableIf._
          _root_.com.thoughtworks.enableIf.isEnabled(${reify(c).tree}, $condition)
        """))) {
        c.Expr(q"..${annottees.map(_.tree)}")
      } else {
        c.Expr(EmptyTree)
      }
    }
  }

}

@compileTimeOnly("enableIf.scala requires macros paradise plugin")
final class enableIf(condition: Context => Boolean) extends StaticAnnotation {

  throw new AssertionError("enableIf.scala requires macro paradise plugin")

  def this(condition: Boolean) = this { _ => condition }

  import scala.language.experimental.macros

  def macroTransform(annottees: Any*): Any =
    macro enableIf.Macros.macroTransform

}
