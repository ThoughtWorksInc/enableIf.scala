package com.thoughtworks

import scala.annotation.StaticAnnotation
import scala.reflect.internal.annotations.compileTimeOnly
import scala.reflect.macros.Context
import scala.util.matching.{Regex, UnanchoredRegex}


object enableWithArtifact {

  def checkArtifact(c: Context, artifactId: String, regex: Regex): Boolean = {
    c.classPath.exists(
      _.getPath.matches(s".*${artifactId}-${regex.regex}")
    )
  }

  def checkArtifact(c: Context, artifactId: String, version: String): Boolean = {
    c.classPath.exists(
      _.getPath.matches(s".*${artifactId}-${version.replace(".", "\\.")}.*")
    )
  }

  private[enableWithArtifact] object Macros {
    def macroTransform(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
      import c.universe._
      val Apply(Select(Apply(_, List(artifactId, regex)), _), List(_@_*)) = c.macroApplication
      if (c.eval(c.Expr[Boolean](
        q"""
          _root_.com.thoughtworks.enableWithArtifact.checkArtifact(${reify(c).tree}, $artifactId, $regex)
        """))) {
        c.Expr(q"..${annottees.map(_.tree)}")
      } else {
        c.Expr(EmptyTree)
      }
    }
  }

}


@compileTimeOnly("enableIf.scala requires macros paradise plugin")
final class enableWithArtifact(artifactId: String, regex: Regex) extends StaticAnnotation {
  throw new AssertionError("enableWithArtifact.scala requires macro paradise plugin")

  def this(artifactId: String, regex: String) = this(artifactId, new Regex(regex))

  import scala.language.experimental.macros

  def macroTransform(annottees: Any*): Any = macro enableWithArtifact.Macros.macroTransform
}
