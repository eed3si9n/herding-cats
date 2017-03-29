---
out: Cartesian.html
---

  [fafm]: http://learnyouahaskell.com/functors-applicative-functors-and-monoids
  [mootws]: making-our-own-typeclass-with-simulacrum.html

### Cartesian

[Functors, Applicative Functors and Monoids][fafm]:

> ここまではファンクター値を写すために、もっぱら 1 引数関数を使ってきました。では、2 引数関数でファンクターを写すと何が起こるでしょう？

```console
scala> import cats._, cats.data._, cats.implicits._
scala> val hs = Functor[List].map(List(1, 2, 3, 4)) ({(_: Int) * (_:Int)}.curried)
scala> Functor[List].map(hs) {_(9)}
```

LYAHFGG:

> では、ファンクター値 `Just (3 *)` とファンクター値 `Just 5` があったとして、
> `Just (3 *)` から関数を取り出して `Just 5` の中身に適用したくなったとしたらどうしましょう?
>
> `Control.Applicative` モジュールにある型クラス `Applicative` に会いに行きましょう！型クラス `Applicative` は、2つの関数 `pure` と `<*>` を定義しています。

Cats はこれを `Cartesian`、`Apply`、 `Applicative` に分けている。以下が `Cartesian` のコントラクト:

```scala
/**
 * [[Cartesian]] captures the idea of composing independent effectful values.
 * It is of particular interest when taken together with [[Functor]] - where [[Functor]]
 * captures the idea of applying a unary pure function to an effectful value,
 * calling `product` with `map` allows one to apply a function of arbitrary arity to multiple
 * independent effectful values.
 *
 * That same idea is also manifested in the form of [[Apply]], and indeed [[Apply]] extends both
 * [[Cartesian]] and [[Functor]] to illustrate this.
 */
@typeclass trait Cartesian[F[_]] {
  def product[A, B](fa: F[A], fb: F[B]): F[(A, B)]
}
```

Cartesian は `product` 関数を定義して、これは `F[A]` と `F[B]` から、効果 `F[_]` に包まれたペア `(A, B)` を作る。`product` のシンボリックなエイリアスは `|@|` で、これは applicative style とも呼ばれる。

#### Option syntax

次にへ行く前に、`Optiona` 値を作るために Cats が導入する syntax をみてみる。

```console
scala> 9.some
scala> none[Int]
```

これで `(Some(9): Option[Int])` を `9.some` と書ける。

#### Applicative Style

LYAHFGG:

> `Applicative` 型クラスでは、`<*>` を連続して使うことができ、
> 1つだけでなく、複数のアプリカティブ値を組み合わせて使うことができます。

以下は Haskell で書かれた例:

```haskell
ghci> pure (-) <*> Just 3 <*> Just 5
Just (-2)
```

Cats には CartesianBuilder 構文というものがある。

```console
scala> (3.some |@| 5.some) map { _ - _ }
scala> (none[Int] |@| 5.some) map { _ - _ }
scala> (3.some |@| none[Int]) map { _ - _ }
```

これは `Option` から `Cartesian` が形成可能であることを示す。

#### Cartesian としての List

LYAHFGG:

> リスト（正確に言えばリスト型のコンストラクタ `[]`）もアプリカティブファンクターです。意外ですか？

CartesianBuilder 構文で書けるかためしてみよう:

```console
scala> (List("ha", "heh", "hmm") |@| List("?", "!", ".")) map {_ + _}
```

#### *> と <* 演算子

`Cartesian` は `<*` と `*>` という 2つの演算子を可能とし、
これらも `Apply[F].product` の特殊形だと考えることができる:

```scala
abstract class CartesianOps[F[_], A] extends Cartesian.Ops[F, A] {
  def |@|[B](fb: F[B]): CartesianBuilder[F]#CartesianBuilder2[A, B] =
    new CartesianBuilder[F] |@| self |@| fb

  def *>[B](fb: F[B])(implicit F: Functor[F]): F[B] = F.map(typeClassInstance.product(self, fb)) { case (a, b) => b }

  def <*[B](fb: F[B])(implicit F: Functor[F]): F[A] = F.map(typeClassInstance.product(self, fb)) { case (a, b) => a }
}
```

定義はシンプルに見えるけども、面白い効果がある:

```console
scala> 1.some <* 2.some
scala> none[Int] <* 2.some
scala> 1.some *> 2.some
scala> none[Int] *> 2.some
```

どちらか一方が失敗すると、`None` が返ってくる。

#### Cartesian 則

`Cartesian` には結合則という法則が1つのみある:

```scala
trait CartesianLaws[F[_]] {
  implicit def F: Cartesian[F]

  def cartesianAssociativity[A, B, C](fa: F[A], fb: F[B], fc: F[C]): (F[(A, (B, C))], F[((A, B), C)]) =
    (F.product(fa, F.product(fb, fc)), F.product(F.product(fa, fb), fc))
}
```
