package com.thoughtworks

import scala.annotation.StaticAnnotation
import scala.reflect.internal.annotations.compileTimeOnly
import scala.reflect.macros.Context
import scala.util.matching.Regex

object enableIf {
  private def getArtifactIds(artifactId: String): (String, String) = {
    val scalaMajorVersion = scala.util.Properties.versionNumberString.split("\\.")
      .take(2).mkString(".")
    val javaAID = artifactId.stripSuffix(scalaMajorVersion)
    val scalaAID = s"${javaAID}_${scalaMajorVersion}"
    (javaAID, scalaAID)
  }

  private def getRegexList(artifactId: String, regex: Regex): List[String] = {
    val (javaAID, scalaAID) = getArtifactIds(artifactId)
    List(s".*${javaAID}-${regex.toString}", s".*${scalaAID}-${regex.toString}")
  }

  private def getRegexList(artifactId: String, version: String): List[String] = {
    val versionRegex = s"${version.replace(".", "\\.")}.*"
    getRegexList(artifactId, new Regex(versionRegex))
  }

  def hasArtifactInClasspath(artifactId: String, regex: Regex)(c: Context): Boolean = {
    getRegexList(artifactId, regex).exists(regex =>
      hasRegexInClasspath(regex)(c)
    )
  }

  def hasArtifactInClasspath(artifactId: String, version: String)(c: Context): Boolean = {
    getRegexList(artifactId, version).exists(regex =>
      hasRegexInClasspath(regex)(c)
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

  private def evalHasRegexInClassPath(regexStr: String)(c: Context): Boolean = {
    import c.universe._
    val regex = Literal(Constant(regexStr))
    c.eval(c.Expr[Boolean](q"""
      _root_.com.thoughtworks.enableIf.hasRegexInClasspath(${regex})(${reify(c).tree})
    """))
  }

  private[enableIf] object Macros {
    def macroTransform(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
      import c.universe._
      val Apply(Select(Apply(_, List(origCondition)), _), List(_@_*)) = c.macroApplication
      val condition = origCondition match {
        case Apply(Ident(name), List(arg)) if "hasRegexInClasspath".equals(name.decoded) =>
          val regex: String = arg match {
            case Select(Tuple2(Literal(Constant(regexStr)), _)) => regexStr.asInstanceOf[String]
            case Literal(Constant(regexStr)) => regexStr.asInstanceOf[String]
            case _ =>
              throw new IllegalArgumentException("hasRegexInClasspath only accepts String/Regex literals")
          }
          val result = evalHasRegexInClassPath(regex)(c)
          Literal(Constant(result))
        case Apply(Ident(name), List(Literal(Constant(artifactId)), arg)) if "hasArtifactInClasspath".equals(name.decoded) =>
          val regexes = arg match {
            case Select(Tuple2(Literal(Constant(regexStr)), _)) =>
              getRegexList(artifactId.asInstanceOf[String], new Regex(regexStr.asInstanceOf[String]))
            case Literal(Constant(version)) =>
              getRegexList(artifactId.asInstanceOf[String], version.asInstanceOf[String])
            case _ =>
              throw new IllegalArgumentException("hasArtifactInClasspath only accepts String/Regex literals")
          }
          val result = regexes.exists(evalHasRegexInClassPath(_)(c))
          Literal(Constant(result))
        case _ =>
          origCondition
      }
      if (c.eval(c.Expr[Boolean](
        q"""
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
