---
out: Semigroupal.html
---

  [fafm]: http://learnyouahaskell.com/functors-applicative-functors-and-monoids
  [mootws]: making-our-own-typeclass-with-simulacrum.html

### Semigroupal

[Functors, Applicative Functors and Monoids][fafm]:

> ここまではファンクター値を写すために、もっぱら 1 引数関数を使ってきました。では、2 引数関数でファンクターを写すと何が起こるでしょう？

```scala mdoc
import cats._

{
  val hs = Functor[List].map(List(1, 2, 3, 4)) ({(_: Int) * (_:Int)}.curried)
  Functor[List].map(hs) {_(9)}
}
```

LYAHFGG:

> では、ファンクター値 `Just (3 *)` とファンクター値 `Just 5` があったとして、
> `Just (3 *)` から関数を取り出して `Just 5` の中身に適用したくなったとしたらどうしましょう?
>
> `Control.Applicative` モジュールにある型クラス `Applicative` に会いに行きましょう！型クラス `Applicative` は、2つの関数 `pure` と `<*>` を定義しています。

Cats はこれを `Cartesian`、`Apply`、 `Applicative` に分けている。以下が `Cartesian` のコントラクト:

```scala
/**
 * [[Semigroupal]] captures the idea of composing independent effectful values.
 * It is of particular interest when taken together with [[Functor]] - where [[Functor]]
 * captures the idea of applying a unary pure function to an effectful value,
 * calling `product` with `map` allows one to apply a function of arbitrary arity to multiple
 * independent effectful values.
 *
 * That same idea is also manifested in the form of [[Apply]], and indeed [[Apply]] extends both
 * [[Semigroupal]] and [[Functor]] to illustrate this.
 */
@typeclass trait Semigroupal[F[_]] {
  def product[A, B](fa: F[A], fb: F[B]): F[(A, B)]
}
```

Semigroupal は `product` 関数を定義して、これは `F[A]` と `F[B]` から、効果 `F[_]` に包まれたペア `(A, B)` を作る。

#### Cartesian 則

`Cartesian` には結合則という法則が1つのみある:

```scala
trait CartesianLaws[F[_]] {
  implicit def F: Cartesian[F]

  def cartesianAssociativity[A, B, C](fa: F[A], fb: F[B], fc: F[C]): (F[(A, (B, C))], F[((A, B), C)]) =
    (F.product(fa, F.product(fb, fc)), F.product(F.product(fa, fb), fc))
}
```
