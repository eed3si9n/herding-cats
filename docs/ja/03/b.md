
  [fafm]: http://learnyouahaskell.com/functors-applicative-functors-and-monoids
  [mootws]: making-our-own-typeclass-with-simulacrum.html

### Apply

[Functors, Applicative Functors and Monoids][fafm]:

> ここまではファンクター値を写すために、もっぱら 1 引数関数を使ってきました。では、2 引数関数でファンクターを写すと何が起こるでしょう？

```console
scala> import cats._, cats.std.all._
scala> val hs = Functor[List].map(List(1, 2, 3, 4)) ({(_: Int) * (_:Int)}.curried)
scala> Functor[List].map(hs) {_(9)}
```

LYAHFGG:

> では、ファンクター値 `Just (3 *)` とファンクター値 `Just 5` があったとして、
> `Just (3 *)` から関数を取り出して `Just 5` の中身に適用したくなったとしたらどうしましょう?
>
> `Control.Applicative` モジュールにある型クラス `Applicative` に会いに行きましょう！型クラス `Applicative` は、2つの関数 `pure` と `<*>` を定義しています。

Cats はこれを `Apply` と `Applicative` に分けている。以下が `Apply` のコントラクト:

```scala
/**
 * Weaker version of Applicative[F]; has apply but not pure.
 *
 * Must obey the laws defined in cats.laws.ApplyLaws.
 */
@typeclass(excludeParents=List("ApplyArityFunctions"))
trait Apply[F[_]] extends Functor[F] with ApplyArityFunctions[F] { self =>

  /**
   * Given a value and a function in the Apply context, applies the
   * function to the value.
   */
  def apply[A, B](fa: F[A])(f: F[A => B]): F[B]

  ....
}
```

`Apply` は `Functor` を拡張することに注目してほしい。
`<*>` 関数は、Cats の `Apply` では `apply` と呼ばれる。

LYAHFGG:

> `<*>` は `fmap` の強化版なのです。`fmap` が普通の関数とファンクター値を引数に取って、関数をファンクター値の中の値に適用してくれるのに対し、`<*>` は関数の入っているファンクター値と値の入っているファンクター値を引数に取って、1つ目のファンクターの中身である関数を2つ目のファンクターの中身に適用するのです。

#### Catnip

次にへ行く前に、Scalaz の `Option` に型付けされた `Option` 値を作るための DSL を移植しよう。

```console
scala> :paste
object Catnip {
  implicit class IdOp[A](val a: A) extends AnyVal {
    def some: Option[A] = Some(a)
  }
  def none[A]: Option[A] = None
}
import Catnip._
```

これで `(Some(9): Option[Int])` を `9.some` と書けるようになった。

```console
scala> 9.some
scala> none[Int]
```

#### Apply としての Option

これを `Apply[Option].apply` と一緒に使ってみる:

```console
scala> Apply[Option].apply(9.some) {{(_: Int) + 3}.some }
scala> Apply[Option].apply(10.some) {{(_: Int) + 3}.some }
scala> Apply[Option].apply(none[String]) {{(_: String) + "hahah"}.some }
scala> Apply[Option].apply("woot".some) { none[String => String] }
```

どちらかが失敗すると、`None` が返ってくる。

昨日の [simulacrum を用いた独自型クラスの定義][mootws]で見たとおり、
simulacrum は型クラス・コントラクト内で定義された関数を演算子として (魔法の力で) 転写する。

```console
scala> import cats.syntax.apply._
scala> 9.some.apply({(_: Int) + 3}.some)
scala> 10.some.apply({(_: Int) + 3}.some)
scala> none[String].apply({(_: String) + "hahah"}.some)
scala> "woot".some.apply(none[String => String])
```

何が起こったのかは理解できるけど、どこかのコードでこれが出てきたら僕は混乱すると思う。
この `apply` は省略禁止。

#### Applicative Style

LYAHFGG:

> `Applicative` 型クラスでは、`<*>` を連続して使うことができ、
> 1つだけでなく、複数のアプリカティブ値を組み合わせて使うことができます。

以下は Haskell で書かれた例:

```haskell
ghci> pure (-) <*> Just 3 <*> Just 5
Just (-2)
```

Cats には ApplyBuilder 構文というものがある。

```console
scala> import cats.syntax.apply._
scala> (3.some |@| 5.some) map { _ - _ }
scala> (none[Int] |@| 5.some) map { _ - _ }
scala> (3.some |@| none[Int]) map { _ - _ }
```

#### Apply としての List

LYAHFGG:

> リスト（正確に言えばリスト型のコンストラクタ `[]`）もアプリカティブファンクターです。意外ですか？

ApplyBuilder 構文で書けるかためしてみよう:

```console
scala> (List("ha", "heh", "hmm") |@| List("?", "!", ".")) map {_ + _}
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
@typeclass(excludeParents=List("ApplyArityFunctions"))
trait Apply[F[_]] extends Functor[F] with ApplyArityFunctions[F] { self =>

  /**
   * Given a value and a function in the Apply context, applies the
   * function to the value.
   */
  def apply[A, B](fa: F[A])(f: F[A => B]): F[B]

  /**
   * apply2 is a binary version of apply, defined in terms of apply.
   */
  def apply2[A, B, Z](fa: F[A], fb: F[B])(f: F[(A, B) => Z]): F[Z] =
    apply(fb)(apply(fa)(map(f)(f => (a: A) => (b: B) => f(a, b))))

  /**
   * Applies the pure (binary) function f to the effectful values fa and fb.
   *
   * map2 can be seen as a binary version of [[cats.Functor]]#map.
   */
  def map2[A, B, Z](fa: F[A], fb: F[B])(f: (A, B) => Z): F[Z] =
    apply(fb)(map(fa)(a => (b: B) => f(a, b)))

  ....
}
```

2項演算子に関しては、`map2` を使うことでアプリカティブ・スタイルを隠蔽することができる。
同じものを 2通りの方法で書いて比較してみる:

```console
scala> (3.some |@| List(4).some) map { _ :: _ }
scala> Apply[Option].map2(3.some, List(4).some) { _ :: _ }
```

同じ結果となった。

`Apply[F].apply` の 2パラメータ版は `Apply[F].apply2` と呼ばれる:

```console
scala> Apply[Option].apply2(3.some, List(4).some) {{ (_: Int) :: (_: List[Int]) }.some }
```

`map2` の特殊形で `tuple2` というものもあって、このように使う:

```console
scala> Apply[Option].tuple2(1.some, 2.some)
scala> Apply[Option].tuple2(1.some, none[Int])
```

2つ以上のパラメータを受け取る関数があったときはどうなるんだろうかと気になっている人は、
`Apply[F[_]]` が `ApplyArityFunctions[F]` を拡張することに気付いただろうか。
これは `apply3`、`map3`、`tuple3` ... から始まって
`apply22`、`map22`、`tuple22` まで自動生成されたコードだ。

#### `*>` と `<*` 演算子

`Apply` は `<*` と `*>` という 2つの演算子を可能とし、
これらも `Apply[F].map2` の特殊形だと考えることができる:

```scala
abstract class ApplyOps[F[_], A] extends Apply.Ops[F, A] {
  ....

  /**
   * combine both contexts but only return the right value
   */
  def *>[B](fb: F[B]) = typeClassInstance.map2(self, fb)((a,b) => b)

  /**
   * combine both contexts but only return the left value
   */
  def <*[B](fb: F[B]) = typeClassInstance.map2(self, fb)((a,b) => a)
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

#### Apply則

Apply には合成則という法則のみが1つある:

```scala
trait ApplyLaws[F[_]] extends FunctorLaws[F] {
  implicit override def F: Apply[F]

  def applyComposition[A, B, C](fa: F[A], fab: F[A => B], fbc: F[B => C]): IsEq[F[C]] = {
    val compose: (B => C) => (A => B) => (A => C) = _.compose
    fa.apply(fab).apply(fbc) <-> fa.apply(fab.apply(fbc.map(compose)))
  }
}
```
