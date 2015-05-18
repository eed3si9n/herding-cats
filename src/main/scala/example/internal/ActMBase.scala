package example
package internal

import scala.reflect.macros.blackbox.Context

abstract class ActMBase { self =>
  def actMImpl[F[_], A](c: Context)(body: c.Expr[A])
    (implicit ev: c.WeakTypeTag[F[_]], ev2: c.WeakTypeTag[A]): c.Expr[F[A]] =
    {
      import c.universe._, c.internal._, decorators._
      import org.scalamacros.resetallattrs._
      val actMMacro = ActMMacro(c, self)
      val code = actMMacro.actMTransform(body.tree)
      // for (t <- code) t.setPos(t.pos.makeTransparent)
      c.Expr[F[A]](c.resetAllAttrs(code))
    }
}
