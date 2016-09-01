---
out: SemigroupK.html
---

  [Semigroup]: Semigroup.html

### SemigroupK

4日目に出てきた [Semigroup][Semigroup] は関数型プログラミングの定番で、色んな所に出てくる。

```console:new
scala> import cats._, cats.instances.all._, cats.syntax.semigroup._
scala> List(1, 2, 3) |+| List(4, 5, 6)
scala> "one" |+| "two"
```

似たもので `SemigroupK` という型コンストラクタ `F[_]` のための型クラスがある。

```scala
@typeclass trait SemigroupK[F[_]] { self =>

  /**
   * Combine two F[A] values.
   */
  @simulacrum.op("<+>", alias = true)
  def combineK[A](x: F[A], y: F[A]): F[A]

  /**
   * Given a type A, create a concrete Semigroup[F[A]].
   */
  def algebra[A]: Semigroup[F[A]] =
    new Semigroup[F[A]] {
      def combine(x: F[A], y: F[A]): F[A] = self.combineK(x, y)
    }
}
```

これは `combineK` 演算子とシンボルを使ったエイリアスである `<+>` をを可能とする。使ってみる。

```console
scala> import cats.syntax.semigroupk._
scala> List(1, 2, 3) <+> List(4, 5, 6)
```

`Semigroup` と違って、`SemigroupK` は `F[_]` の型パラメータに何が入っていても大丈夫だ。

#### SemigroupK としての Option

`Option[A]` は型パラメータ `A` が `Semigroup` である時に限って `Option[A]` も `Semigroup` を形成する。

```console
scala> :paste
object Catnip {
  implicit class IdOp[A](val a: A) extends AnyVal {
    def some: Option[A] = Some(a)
  }
  def none[A]: Option[A] = None
}
import Catnip._
scala> case class Foo(x: String)
```

そのため、これはうまくいかない:

```scala
scala> Foo("x").some |+| Foo("y").some
<console>:33: error: value |+| is not a member of Option[Foo]
       Foo("x").some |+| Foo("y").some
                     ^
```

だけど、これは大丈夫:

```console
scala> Foo("x").some <+> Foo("y").some
```

この 2つの型クラスの振る舞いは微妙に異なるので注意が必要だ。

```console
scala> 1.some |+| 2.some
scala> 1.some <+> 2.some
```

`Semigroup` は `Option` の中身の値もつなげるが、`SemigroupK` の方は最初の選択する。

#### SemigroupK 則

```
trait SemigroupKLaws[F[_]] {
  implicit def F: SemigroupK[F]

  def semigroupKAssociative[A](a: F[A], b: F[A], c: F[A]): IsEq[F[A]] =
    F.combineK(F.combineK(a, b), c) <-> F.combineK(a, F.combineK(b, c))
}
```
