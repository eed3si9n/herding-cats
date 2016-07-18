
  [fafm]: http://learnyouahaskell.com/functors-applicative-functors-and-monoids
  [mootws]: making-our-own-typeclass-with-simulacrum.html

### Apply

[Functors, Applicative Functors and Monoids][fafm]:

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

#### Apply としての Option

これを `Apply[Option].ap` と一緒に使ってみる:

```console
scala> :paste
object Catnip {
  implicit class IdOp[A](val a: A) extends AnyVal {
    def some: Option[A] = Some(a)
  }
  def none[A]: Option[A] = None
}
import Catnip._
scala> Apply[Option].ap({{(_: Int) + 3}.some })(9.some)
scala> Apply[Option].ap({{(_: Int) + 3}.some })(10.some)
scala> Apply[Option].ap({{(_: String) + "hahah"}.some })(none[String])
scala> Apply[Option].ap({ none[String => String] })("woot".some)
```

どちらかが失敗すると、`None` が返ってくる。

昨日の [simulacrum を用いた独自型クラスの定義][mootws]で見たとおり、
simulacrum は型クラス・コントラクト内で定義された関数を演算子として (魔法の力で) 転写する。

```console
scala> import cats.syntax.apply._
scala> ({(_: Int) + 3}.some) ap 9.some
scala> ({(_: Int) + 3}.some) ap 10.some
scala> ({(_: String) + "hahah"}.some) ap none[String]
scala> (none[String => String]) ap "woot".some
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

  /**
   * Given a value and a function in the Apply context, applies the
   * function to the value.
   */
  def ap[A, B](ff: F[A => B])(fa: F[A]): F[B]

  override def product[A, B](fa: F[A], fb: F[B]): F[(A, B)] =
    ap(map(fa)(a => (b: B) => (a, b)))(fb)

  /**
   * ap2 is a binary version of ap, defined in terms of ap.
   */
  def ap2[A, B, Z](ff: F[(A, B) => Z])(fa: F[A], fb: F[B]): F[Z] =
    map(product(fa, product(fb, ff))) { case (a, (b, f)) => f(a, b) }

  /**
   * Applies the pure (binary) function f to the effectful values fa and fb.
   *
   * map2 can be seen as a binary version of [[cats.Functor]]#map.
   */
  def map2[A, B, Z](fa: F[A], fb: F[B])(f: (A, B) => Z): F[Z] =
    map(product(fa, fb)) { case (a, b) => f(a, b) }

  ....
}
```

2項演算子に関しては、`map2` を使うことでアプリカティブ・スタイルを隠蔽することができる。
同じものを 2通りの方法で書いて比較してみる:

```console
scala> import cats.syntax.cartesian._
scala> (3.some |@| List(4).some) map { _ :: _ }
scala> Apply[Option].map2(3.some, List(4).some) { _ :: _ }
```

同じ結果となった。

`Apply[F].ap` の 2パラメータ版は `Apply[F].ap2` と呼ばれる:

```console
scala> Apply[Option].ap2({{ (_: Int) :: (_: List[Int]) }.some })(3.some, List(4).some)
```

`map2` の特殊形で `tuple2` というものもあって、このように使う:

```console
scala> Apply[Option].tuple2(1.some, 2.some)
scala> Apply[Option].tuple2(1.some, none[Int])
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
