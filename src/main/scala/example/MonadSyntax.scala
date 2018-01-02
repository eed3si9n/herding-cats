package example

import reflect.macros.blackbox.Context
import cats._

object MonadSyntax extends MonadImplicit0 {
  def actM[F[_], A](body: A): F[A] = macro internal.ActMImpl.actMImpl[F, A]
}

sealed trait MonadImplicit0 extends MonadImplicit1 {
  implicit def toFuncOp[A, B](fa: A => B)(implicit ev: Monad[A => ?]): MonadOp[A => ?, B] = new MonadOp[A => ?, B](fa)
}

sealed trait MonadImplicit1 {
  implicit def toMonadOp[F[_], A](fa: F[A])(implicit ev: Monad[F]): MonadOp[F, A] = new MonadOp(fa)
}

class MonadOp[F[_], A](fa: F[A]) {
  def next: A = sys.error(s"next was not macro'ed away!")
}
