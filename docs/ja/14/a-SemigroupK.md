---
out: SemigroupK.html
---

  [Semigroup]: Semigroup.html

### SemigroupK

4日目に出てきた [Semigroup][Semigroup] は関数型プログラミングの定番で、色んな所に出てくる。

```scala mdoc
import cats._, cats.syntax.all._

List(1, 2, 3) |+| List(4, 5, 6)

"one" |+| "two"
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

```scala mdoc
List(1, 2, 3) <+> List(4, 5, 6)
```

`Semigroup` と違って、`SemigroupK` は `F[_]` の型パラメータが何であっても動作する。

#### SemigroupK としての Option

`Option[A]` は型パラメータ `A` が `Semigroup` である時に限って `Option[A]` も `Semigroup` を形成する。そこで `Semigroup` を形成しないデータ型を定義して邪魔してみよう:

```scala mdoc
case class Foo(x: String)
```

これはうまくいかない:

```scala mdoc:fail
Foo("x").some |+| Foo("y").some
```

だけど、これは大丈夫:

```scala mdoc
Foo("x").some <+> Foo("y").some
```

この 2つの型クラスの振る舞いは微妙に異なるので注意が必要だ。

```scala mdoc
1.some |+| 2.some

1.some <+> 2.some
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
