package example

import cats._
import cats.std.all._

case class First[A: Eq](val unwrap: Option[A])
object First {
  implicit def firstMonoid[A: Eq]: Monoid[First[A]] = new Monoid[First[A]] {
    def combine(a1: First[A], a2: First[A]): First[A] =
      First((a1.unwrap, a2.unwrap) match {
        case (Some(x), _) => Some(x)
        case (None, y)    => y
      })
    def empty: First[A] = First(None: Option[A])
  }
  implicit def firstEq[A: Eq]: Eq[First[A]] = new Eq[First[A]] {
    def eqv(a1: First[A], a2: First[A]): Boolean =
      Eq[Option[A]].eqv(a1.unwrap, a2.unwrap)
  }
}
