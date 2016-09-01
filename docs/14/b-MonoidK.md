---
out: MonoidK.html
---

### MonoidK

There's also `MonoidK`.

```scala
@typeclass trait MonoidK[F[_]] extends SemigroupK[F] { self =>

  /**
   * Given a type A, create an "empty" F[A] value.
   */
  def empty[A]: F[A]

  /**
   * Given a type A, create a concrete Monoid[F[A]].
   */
  override def algebra[A]: Monoid[F[A]] =
    new Monoid[F[A]] {
      def empty: F[A] = self.empty
      def combine(x: F[A], y: F[A]): F[A] = self.combineK(x, y)
    }

  ....
}
```

This adds `empty[A]` function to the contract.
The notion of emptiness here is defined in terms of the left and right identity laws with regards to `combineK`.
Given that `combine` and `combineK` behave differently, `Monoid[F[A]].empty` and `MonoidK[F].empty[A]` could also be different.

```console:new
scala> import cats._, cats.instances.all._
scala> Monoid[Option[Int]].empty
scala> MonoidK[Option].empty[Int]
```

In case of `Option[Int]` they happened to be both `None`.

#### MonoidK laws

```scala
trait MonoidKLaws[F[_]] extends SemigroupKLaws[F] {
  override implicit def F: MonoidK[F]

  def monoidKLeftIdentity[A](a: F[A]): IsEq[F[A]] =
    F.combineK(F.empty, a) <-> a

  def monoidKRightIdentity[A](a: F[A]): IsEq[F[A]] =
    F.combineK(a, F.empty) <-> a
}
```
