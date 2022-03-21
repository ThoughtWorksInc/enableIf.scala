package com.thoughtworks

import scala.annotation.StaticAnnotation
import scala.reflect.internal.annotations.compileTimeOnly
import scala.reflect.macros.Context
import scala.util.matching.{Regex, UnanchoredRegex}


object enableWithClasspath {
  def checkClasspathRegex(c: Context, regex: String): Boolean = {
    c.classPath.exists(
      _.getPath.matches(regex)
    )
  }

  def checkClasspathRegex(c: Context, regex: Regex): Boolean = {
    c.classPath.exists(
      _.getPath.matches(regex.toString)
    )
  }

  private[enableWithClasspath] object Macros {
    def macroTransform(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
      import c.universe._
      val Apply(Select(Apply(_, List(regex)), _), List(_@_*)) = c.macroApplication
      if (c.eval(c.Expr[Boolean](
        q"""
          _root_.com.thoughtworks.enableWithClasspath.checkClasspathRegex(${reify(c).tree}, $regex)
        """))) {
        c.Expr(q"..${annottees.map(_.tree)}")
      } else {
        c.Expr(EmptyTree)
      }
    }
  }

}


@compileTimeOnly("enableIf.scala requires macros paradise plugin")
final class enableWithClasspath(regex: Regex) extends StaticAnnotation {
  throw new AssertionError("enableWithClasspath.scala requires macro paradise plugin")

  def this(regex: String) = this(new Regex(regex))

  import scala.language.experimental.macros

  def macroTransform(annottees: Any*): Any = macro enableWithClasspath.Macros.macroTransform
}
