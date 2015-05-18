package example
package internal

import scala.reflect.macros.blackbox.Context

object ActMImpl extends ActMBase {
  override def actMImpl[F[_], A](c: Context)(body: c.Expr[A])
    (implicit ev: c.WeakTypeTag[F[_]], ev2: c.WeakTypeTag[A]): c.Expr[F[A]] =
    super.actMImpl[F, A](c)(body)
}
