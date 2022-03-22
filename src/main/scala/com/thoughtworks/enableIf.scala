package com.thoughtworks

import scala.annotation.StaticAnnotation
import scala.reflect.internal.annotations.compileTimeOnly
import scala.reflect.macros.Context
import scala.util.matching.Regex

object enableIf {
  def hasArtifactInClasspath(artifactId: String, regex: Regex)(c: Context): Boolean = {
    c.classPath.exists(
      _.getPath.matches(s".*${artifactId}-${regex.toString}")
    )
  }

  def hasArtifactInClasspath(artifactId: String, version: String)(c: Context): Boolean = {
    val versionRegex = version.replace(".", "\\.")
    c.classPath.exists(
      _.getPath.matches(s".*${artifactId}-${versionRegex}.*")
    )
  }

  def hasRegexInClasspath(regex: String): Context => Boolean = {
    c => c.classPath.exists(
      _.getPath.matches(regex)
    )
  }

  def hasRegexInClasspath(regex: Regex): Context => Boolean = {
    c => c.classPath.exists(
      _.getPath.matches(regex.toString)
    )
  }

  def isEnabled(c: Context, booleanCondition: Boolean) = booleanCondition

  def isEnabled(c: Context, functionCondition: Context => Boolean) =
    functionCondition(c)

  private[enableIf] object Macros {
    def macroTransform(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
      import c.universe._
      val Apply(Select(Apply(_, List(condition)), _), List(_ @_*)) =
        c.macroApplication
      if (
        c.eval(c.Expr[Boolean](q"""
          _root_.com.thoughtworks.enableIf.isEnabled(${reify(
            c
          ).tree}, $condition)
        """))
      ) {
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
