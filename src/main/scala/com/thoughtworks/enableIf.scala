package com.thoughtworks

import scala.annotation.StaticAnnotation
import scala.reflect.internal.annotations.compileTimeOnly
import scala.reflect.macros.Context
import scala.util.matching.Regex


object enableIf {
  val classpathRegex = "(.*)/([^/]*)-([^/]*)\\.jar".r

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
      regex.matches(dep.getPath)
    }
  }

  def classpathMatchesArtifact(artifactId: String, version: String): Context => Boolean = {
    c => c.classPath.exists { dep =>
      classpathRegex.findAllMatchIn(dep.getPath).exists { m =>
        artifactId.equals(m.group(2)) && version.equals(m.group(3))
      }
    }
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
