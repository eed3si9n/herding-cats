---
out: MonoidK.html
---

### MonoidK

`MonoidK` もある。

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

これはコントラクトに `empty[A]` 関数を追加する。
ここでの空の値の概念は `combineK` に対する左右単位元として定義される。
`combine` と `combineK` の振る舞いが異なるため、`Monoid[F[A]].empty` と `MonoidK[F].empty[A]` も異なる値を取り得る。

```console:new
scala> import cats._, cats.instances.all._
scala> Monoid[Option[Int]].empty
scala> MonoidK[Option].empty[Int]
```

`Option[Int]` に関しては、両方とも `None` みたいだ。

#### MonoidK 則

```scala
trait MonoidKLaws[F[_]] extends SemigroupKLaws[F] {
  override implicit def F: MonoidK[F]

  def monoidKLeftIdentity[A](a: F[A]): IsEq[F[A]] =
    F.combineK(F.empty, a) <-> a

  def monoidKRightIdentity[A](a: F[A]): IsEq[F[A]] =
    F.combineK(a, F.empty) <-> a
}
```
