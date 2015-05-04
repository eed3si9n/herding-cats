
  [FunctorSource]: $catsBaseUrl$/core/src/main/scala/cats/Functor.scala
  [@blaisorblade]: https://twitter.com/blaisorblade

### Functor

LYAHFGG:

> 今度は、`Functor` （ファンクター）という型クラスを見ていきたいと思います。`Functor` は、**全体を写せる** (map over) ものの型クラスです。

本のとおり、[実装がどうなってるか][FunctorSource]をみてみよう:

```scala
/**
 * Functor.
 *
 * The name is short for "covariant functor".
 *
 * Must obey the laws defined in cats.laws.FunctorLaws.
 */
@typeclass trait Functor[F[_]] extends functor.Invariant[F] { self =>
  def map[A, B](fa: F[A])(f: A => B): F[B]

  ....
}
```

このように使うことができる:

```console:new
scala> import cats._, cats.std.all._
scala> Functor[List].map(List(1, 2, 3)) { _ + 1 }
```

このような用例は**関数構文**と呼ぶことにする:

`@typeclass` アノテーションによって自動的に `map` 関数が `map`
演算子になることは分かると思う。 `fa` の所がメソッドの `this` になって、第2パラメータリストが、
`map` 演算子のパラメータリストとなる:

```scala
// 生成されるコードの予想
object Functor {
  trait Ops[F[_], A] {
    def typeClassInstance: Functor[F]
    def self: F[A]
    def map[B](f: A => B): F[B] = typeClassInstance.map(self)(f)
  }
}
```

これは、Scala collection ライブラリの `map`　とかなり近いものに見えるが、
この `map` は `CanBuildFrom` の自動変換を行わない。

#### ファンクターとしての Either

Cats は `Either[A, B]` の `Functor` インスタンスを定義する。

```console
scala> import cats.syntax.functor._
scala> (Right(1): Either[String, Int]) map { _ + 1 }
scala> (Left("boom!"): Either[String, Int]) map { _ + 1 }
```

上のデモが正しく動作するのは現在の所 `Either[A, B]` には標準ライブラリでは
`map` を実装してないということに依存していることに注意してほしい。
例えば、`List(1, 2, 3)` を例に使った場合は、
`Functor[List]` の `map` ではなくて、
リストの実装の `map` が呼び出されてしまう。
そのため、演算子構文の方が読み慣れていると思うけど、
標準ライブラリが `map` を実装していないことを確信しているか、
多相関数内で使うか以外は演算子構文は避けた方がいい。
回避策としては関数構文を使うことだ。

#### ファンクターとしての関数

Cats は `Function1` に対する `Functor` のインスタンスも定義する。

```console
scala> val h = ((x: Int) => x + 1) map {_ * 7}
scala> h(3)
```

これは興味深い。つまり、`map` は関数を合成する方法を与えてくれるが、順番が `f compose g` とは逆順だ。通りで Scalaz は `map` のエイリアスとして ` ∘` を提供するわけだ。`Function1` のもう1つのとらえ方は、定義域 (domain) から値域 (range) への無限の写像だと考えることができる。入出力に関しては飛ばして [Functors, Applicative Functors and Monoids](http://learnyouahaskell.com/functors-applicative-functors-and-monoids) へ行こう (本だと、「ファンクターからアプリカティブファンクターへ」)。

> ファンクターとしての関数
> ...
>
> ならば、型 `fmap :: (a -> b) -> (r -> a) -> (r -> b)` が意味するものとは？この型は、`a` から `b` への関数と、`r` から `a` への関数を引数に受け取り、`r` から `b` への関数を返す、と読めます。何か思い出しませんか？そう！関数合成です！

あ、すごい Haskell も僕がさっき言ったように関数合成をしているという結論になったみたいだ。ちょっと待てよ。

```haskell
ghci> fmap (*3) (+100) 1
303
ghci> (*3) . (+100) \$ 1  
303
```

Haskell では `fmap` は `f compose g` を同じ順序で動作してるみたいだ。Scala でも同じ数字を使って確かめてみる:

```console
scala> (((_: Int) * 3) map {_ + 100}) (1)
```

何かがおかしい。`fmap` の宣言と Cats の `map` 関数を比べてみよう:

```haskell
fmap :: (a -> b) -> f a -> f b

```

そしてこれが Cats:

```scala
def map[A, B](fa: F[A])(f: A => B): F[B]

```

順番が逆になっている。これに関して Paolo Giarrusso ([@blaisorblade][@blaisorblade]) 氏が説明してくれた:

> これはよくある Haskell 対 Scala の差異だ。
>
> Haskell では、point-free プログラミングをするために、「データ」の引数が通常最後に来る。例えば、
> `map f list` という引数順を利用して
> `map f . map g . map h` と書くことでリストの変換子を得ることができる。
> (ちなみに、map は fmap を List ファンクターに限定させたものだ)
>
> 一方 Scala では、「データ」引数はレシーバとなる。
> これは、しばしば型推論にとっても重要であるため、map を関数のメソッドとして定義するのは無理がある。
> Scala が `(x => x + 1) map List(1, 2, 3)` の型推論を行おうとするのを考えてみてほしい。

これが、どうやら有力な説みたいだ。

#### 関数の持ち上げ

LYAHFGG:

> `fmap` も、関数とファンクター値を取ってファンクター値を返す 2 引数関数と思えますが、そうじゃなくて、関数を取って「元の関数に似てるけどファンクター値を取ってファンクター値を返す関数」を返す関数だと思うこともできます。`fmap` は、関数 `a -> b` を取って、関数 `f a -> f b` を返すのです。こういう操作を、関数の**持ち上げ** (lifting) といいます。

```haskell
ghci> :t fmap (*2)  
fmap (*2) :: (Num a, Functor f) => f a -> f a  
ghci> :t fmap (replicate 3)  
fmap (replicate 3) :: (Functor f) => f a -> f [a]  
```

パラメータ順が逆だということは、この持ち上げ (lifting) ができないということだろうか?
幸いなことに、Cats は `Functor` 型クラス内に派生関数を色々実装している:

```scala
@typeclass trait Functor[F[_]] extends functor.Invariant[F] { self =>
  def map[A, B](fa: F[A])(f: A => B): F[B]

  ....

  // derived methods

  /**
   * Lift a function f to operate on Functors
   */
  def lift[A, B](f: A => B): F[A] => F[B] = map(_)(f)

  /**
   * Empty the fa of the values, preserving the structure
   */
  def void[A](fa: F[A]): F[Unit] = map(fa)(_ => ())

  /**
   * Tuple the values in fa with the result of applying a function
   * with the value
   */
  def fproduct[A, B](fa: F[A])(f: A => B): F[(A, B)] = map(fa)(a => a -> f(a))

  /**
   * Replaces the `A` value in `F[A]` with the supplied value.
   */
  def as[A, B](fa: F[A], b: B): F[B] = map(fa)(_ => b)
}
```

見ての通り、`lift` も入っている!

```console
scala> val lifted = Functor[List].lift {(_: Int) * 2}
scala> lifted(List(1, 2, 3))
```

これで `{(_: Int) * 2}` という関数を `List[Int] => List[Int]` に持ち上げることができた。
他の派生関数も演算子構文で使ってみる:

```console
scala> List(1, 2, 3).void
scala> List(1, 2, 3) fproduct {(_: Int) * 2}
scala> List(1, 2, 3) as "x"
```

### Functor則

LYAHFGG:

> すべてのファンクターの性質や挙動は、ある一定の法則に従うことになっています。
> ...
> ファンクターの第一法則は、「`id` でファンクター値を写した場合、ファンクター値が変化してはいけない」というものです。

`Either[A, B]` を使って確かめてみる。

```console
scala> val x: Either[String, Int] = Right(1)
scala> import cats.syntax.eq._
scala> assert { (x map identity) === x }
```

> 第二法則は、2つの関数 `f` と `g` について、「`f` と `g` の合成関数でファンクター値を写したもの」と、「まず `g`、次に `f` でファンクター値を写したもの」が等しいことを要求します。

言い換えると、

```console
scala> val f = {(_: Int) * 3}
scala> val g = {(_: Int) + 1}
scala> assert { (x map (f map g)) === (x map f map g) }
```

これらの法則は Functor の実装者が従うべき法則で、コンパイラはチェックしてくれない。
