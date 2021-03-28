
  [fafm]: http://learnyouahaskell.com/functors-applicative-functors-and-monoids
  [mootws]: making-our-own-typeclass-with-simulacrum.html

### Apply

[Functors, Applicative Functors and Monoids][fafm]:

> ここまではファンクター値を写すために、もっぱら 1 引数関数を使ってきました。では、2 引数関数でファンクターを写すと何が起こるでしょう？

```scala mdoc
import cats._, cats.syntax.all._

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

Cats は `Applicative` を `Apply` と `Applicative` に分けている。以下が `Apply` のコントラクト:

```scala
/**
 * Weaker version of Applicative[F]; has apply but not pure.
 *
 * Must obey the laws defined in cats.laws.ApplyLaws.
 */
@typeclass(excludeParents = List("ApplyArityFunctions"))
trait Apply[F[_]] extends Functor[F] with Cartesian[F] with ApplyArityFunctions[F] { self =>

  /**
   * Given a value and a function in the Apply context, applies the
   * function to the value.
   */
  def ap[A, B](ff: F[A => B])(fa: F[A]): F[B]

  ....
}
```

`Apply` は `Functor`、`Cartesian`、そして `ApplyArityFunctions` を拡張することに注目してほしい。
`<*>` 関数は、Cats の `Apply` では `ap` と呼ばれる。(これは最初は `apply` と呼ばれていたが、`ap` に直された。+1)

LYAHFGG:

> `<*>` は `fmap` の強化版なのです。`fmap` が普通の関数とファンクター値を引数に取って、関数をファンクター値の中の値に適用してくれるのに対し、`<*>` は関数の入っているファンクター値と値の入っているファンクター値を引数に取って、1つ目のファンクターの中身である関数を2つ目のファンクターの中身に適用するのです。

#### Applicative Style

LYAHFGG:

> `Applicative` 型クラスでは、`<*>` を連続して使うことができ、
> 1つだけでなく、複数のアプリカティブ値を組み合わせて使うことができます。

以下は Haskell で書かれた例:

```haskell
ghci> pure (-) <*> Just 3 <*> Just 5
Just (-2)
```

Cats には apply 構文というものがある。

```scala mdoc
(3.some, 5.some) mapN { _ - _ }

(none[Int], 5.some) mapN { _ - _ }

(3.some, none[Int]) mapN { _ - _ }
```

これは `Option` から `Cartesian` が形成可能であることを示す。

#### Apply としての List

LYAHFGG:

> リスト（正確に言えばリスト型のコンストラクタ `[]`）もアプリカティブファンクターです。意外ですか？

apply 構文で書けるかためしてみよう:

```scala mdoc
(List("ha", "heh", "hmm"), List("?", "!", ".")) mapN {_ + _}
```

#### `*>` と `<*` 演算子

`Apply` は `<*` と `*>` という 2つの演算子を可能とし、これらも `Apply[F].map2` の特殊形だと考えることができる。

定義はシンプルに見えるけども、面白い効果がある:

```scala mdoc
1.some <* 2.some

none[Int] <* 2.some

1.some *> 2.some

none[Int] *> 2.some
```

どちらか一方が失敗すると、`None` が返ってくる。

#### Option syntax

次にへ行く前に、`Optiona` 値を作るために Cats が導入する syntax をみてみる。

```scala mdoc
9.some

none[Int]
```

これで `(Some(9): Option[Int])` を `9.some` と書ける。

#### Apply としての Option

これを `Apply[Option].ap` と一緒に使ってみる:

```scala mdoc:reset
import cats._, cats.syntax.all._

Apply[Option].ap({{(_: Int) + 3}.some })(9.some)

Apply[Option].ap({{(_: Int) + 3}.some })(10.some)

Apply[Option].ap({{(_: String) + "hahah"}.some })(none[String])

Apply[Option].ap({ none[String => String] })("woot".some)
```

どちらかが失敗すると、`None` が返ってくる。

昨日の [simulacrum を用いた独自型クラスの定義][mootws]で見たとおり、
simulacrum は型クラス・コントラクト内で定義された関数を演算子として (魔法の力で) 転写する。

```scala mdoc
({(_: Int) + 3}.some) ap 9.some

({(_: Int) + 3}.some) ap 10.some

({(_: String) + "hahah"}.some) ap none[String]

(none[String => String]) ap "woot".some
```

#### Apply の便利な関数

LYAHFGG:

> `Control.Applicative` には `liftA2` という、以下のような型を持つ関数があります。

```haskell
liftA2 :: (Applicative f) => (a -> b -> c) -> f a -> f b -> f c .
```

Scala ではパラメータが逆順であることを覚えているだろうか。
つまり、`F[B]` と `F[A]` を受け取った後、`(A, B) => C` という関数を受け取る関数だ。
これは `Apply` では `map2` と呼ばれている。

```scala
@typeclass(excludeParents = List("ApplyArityFunctions"))
trait Apply[F[_]] extends Functor[F] with Cartesian[F] with ApplyArityFunctions[F] { self =>
  def ap[A, B](ff: F[A => B])(fa: F[A]): F[B]

  def productR[A, B](fa: F[A])(fb: F[B]): F[B] =
    map2(fa, fb)((_, b) => b)

  def productL[A, B](fa: F[A])(fb: F[B]): F[A] =
    map2(fa, fb)((a, _) => a)

  override def product[A, B](fa: F[A], fb: F[B]): F[(A, B)] =
    ap(map(fa)(a => (b: B) => (a, b)))(fb)

  /** Alias for [[ap]]. */
  @inline final def <*>[A, B](ff: F[A => B])(fa: F[A]): F[B] =
    ap(ff)(fa)

  /** Alias for [[productR]]. */
  @inline final def *>[A, B](fa: F[A])(fb: F[B]): F[B] =
    productR(fa)(fb)

  /** Alias for [[productL]]. */
  @inline final def <*[A, B](fa: F[A])(fb: F[B]): F[A] =
    productL(fa)(fb)

  /**
   * ap2 is a binary version of ap, defined in terms of ap.
   */
  def ap2[A, B, Z](ff: F[(A, B) => Z])(fa: F[A], fb: F[B]): F[Z] =
    map(product(fa, product(fb, ff))) { case (a, (b, f)) => f(a, b) }

  def map2[A, B, Z](fa: F[A], fb: F[B])(f: (A, B) => Z): F[Z] =
    map(product(fa, fb))(f.tupled)

  def map2Eval[A, B, Z](fa: F[A], fb: Eval[F[B]])(f: (A, B) => Z): Eval[F[Z]] =
    fb.map(fb => map2(fa, fb)(f))

  ....
}
```

2項演算子に関しては、`map2` を使うことでアプリカティブ・スタイルを隠蔽することができる。
同じものを 2通りの方法で書いて比較してみる:

```scala mdoc
(3.some, List(4).some) mapN { _ :: _ }

Apply[Option].map2(3.some, List(4).some) { _ :: _ }
```

同じ結果となった。

`Apply[F].ap` の 2パラメータ版は `Apply[F].ap2` と呼ばれる:

```scala mdoc
Apply[Option].ap2({{ (_: Int) :: (_: List[Int]) }.some })(3.some, List(4).some)
```

`map2` の特殊形で `tuple2` というものもあって、このように使う:

```scala mdoc
Apply[Option].tuple2(1.some, 2.some)

Apply[Option].tuple2(1.some, none[Int])
```

2つ以上のパラメータを受け取る関数があったときはどうなるんだろうかと気になっている人は、
`Apply[F[_]]` が `ApplyArityFunctions[F]` を拡張することに気付いただろうか。
これは `ap3`、`map3`、`tuple3` ... から始まって
`ap22`、`map22`、`tuple22` まで自動生成されたコードだ。

#### Apply則

Apply には合成則という法則のみが1つある:

```scala
trait ApplyLaws[F[_]] extends FunctorLaws[F] {
  implicit override def F: Apply[F]

  def applyComposition[A, B, C](fa: F[A], fab: F[A => B], fbc: F[B => C]): IsEq[F[C]] = {
    val compose: (B => C) => (A => B) => (A => C) = _.compose
    fa.ap(fab).ap(fbc) <-> fa.ap(fab.ap(fbc.map(compose)))
  }
}
```
