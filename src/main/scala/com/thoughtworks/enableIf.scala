package com.thoughtworks

import scala.annotation.StaticAnnotation
import scala.reflect.internal.annotations.compileTimeOnly
import scala.reflect.macros.Context
import scala.util.matching.Regex


object enableIf {
  private def getRegex(artifactId: String, regex: Regex): String = {
    s".*${artifactId}-${regex.toString}"
  }

  private def getRegex(artifactId: String, version: String): String = {
    val versionRegex = s"${version.replace(".", "\\.")}.*"
    getRegex(artifactId, new Regex(versionRegex))
  }

  private def getScalaRegex(artifactId: String, regex: Regex): String = {
    val scalaMajorVersion = scala.util.Properties.versionNumberString.split("\\.")
      .take(2).mkString(".")
    getRegex(s"${artifactId}_${scalaMajorVersion}", regex)
  }

  private def getScalaRegex(artifactId: String, version: String): String = {
    val versionRegex = s"${version.replace(".", "\\.")}.*"
    getScalaRegex(artifactId, new Regex(versionRegex))
  }

  def classpathMatches(regex: String): Context => Boolean = {
    c => c.classPath.exists(_.getPath.matches(regex))
  }

  def classpathMatches(regex: Regex): Context => Boolean = {
    c => c.classPath.exists(_.getPath.matches(regex.toString))
  }

  def classpathMatchesArtifact(artifactId: String, regex: Regex): Context => Boolean = {
    classpathMatches(getRegex(artifactId, regex))
  }

  def classpathMatchesArtifact(artifactId: String, version: String): Context => Boolean = {
    classpathMatches(getRegex(artifactId, version))
  }

  def classpathMatchesScalaArtifact(artifactId: String, regex: Regex): Context => Boolean = {
    classpathMatches(getScalaRegex(artifactId, regex))
  }

  def classpathMatchesScalaArtifact(artifactId: String, version: String): Context => Boolean = {
    classpathMatches(getScalaRegex(artifactId, version))
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
