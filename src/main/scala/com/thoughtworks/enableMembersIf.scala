package com.thoughtworks

import scala.annotation.StaticAnnotation
import scala.reflect.internal.annotations.compileTimeOnly
import scala.reflect.macros.Context

object enableMembersIf {

  private[enableMembersIf] object Macros {
    def macroTransform(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {

      import c.universe._
      def constructors(body: List[Tree]): List[Tree] = {
        (for {
          constructor @ DefDef(_, nme.CONSTRUCTOR, _, _, _, _) <- body.view
        } yield constructor).take(1).toList
      }

      val Apply(Select(Apply(_, List(condition)), _), List(_ @_*)) =
        c.macroApplication
      if (c.eval(c.Expr[Boolean](q"""
          _root_.com.thoughtworks.enableIf.isEnabled(${reify(c).tree}, $condition)
        """))) {
        c.Expr(q"..${annottees.map(_.tree)}")
      } else {
        val head = annottees.head.tree match {
          case ClassDef(mods, name, tparams, Template(parents, self, body)) =>
            ClassDef(
              mods,
              name,
              tparams,
              Template(parents, self, constructors(body))
            )
          case ModuleDef(mods, name, Template(parents, self, body)) =>
            ModuleDef(mods, name, Template(parents, self, constructors(body)))
        }
        c.Expr(q"$head; ..${annottees.tail}")

      }
    }
  }

}

@compileTimeOnly("enableIf.scala requires macros paradise plugin")
final class enableMembersIf(condition: Context => Boolean)
    extends StaticAnnotation {

  throw new AssertionError("enableIf.scala requires macro paradise plugin")

  def this(condition: Boolean) = this { _ => condition }

  import scala.language.experimental.macros

  def macroTransform(annottees: Any*): Any =
    macro enableMembersIf.Macros.macroTransform

}
