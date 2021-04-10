---
out: monadic-functions.html
---

  [fafmm]: http://learnyouahaskell.com/for-a-few-monads-more
  [925]: https://github.com/typelevel/cats/pull/925
  [TraverseFilter]: TraverseFilter.html

### 便利なモナディック関数特集

[すごいHaskellたのしく学ぼう](http://www.amazon.co.jp/dp/4274068854) 曰く:

> この節では、モナド値を操作したり、モナド値を返したりする関数（両方でも可！）をいくつか紹介します。そんな関数は**モナディック関数**と呼ばれます。

Haskell 標準の `Monad` と違って Cats の `Monad` は後知恵である
より弱い型クラスを用いた粒度の高い設計となっている。

- `Monad`
- extends `FlatMap` and `Applicative`
- extends `Apply`
- extends `Functor`

そのため、全てのモナドがアプリカティブ・ファンクターとファンクターであることは自明となっていて、
モナドを形成する全てのデータ型に対して `ap` や `map` 演算子を使うことができる。

#### flatten メソッド

LYAHFGG:

> 実は、任意の入れ子になったモナドは平らにできるんです。そして実は、これはモナド特有の性質なのです。このために、`join` という関数が用意されています。

`Cats` でこれに当たる関数は `flatten` と呼ばれており、`FlatMap` にて定義されている。
simulacrum のお陰で `flatten` はメソッドとしても導入されている。

```scala
@typeclass trait FlatMap[F[_]] extends Apply[F] {
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  /**
   * also commonly called join
   */
  def flatten[A](ffa: F[F[A]]): F[A] =
    flatMap(ffa)(fa => fa)

  ....
}
```

`Option[A]` は既に `flatten` を実装するので、
これを抽象型にするために抽象関数を書く必要がある。

```scala mdoc
import cats._, cats.syntax.all._

def join[F[_]: FlatMap, A](fa: F[F[A]]): F[A] =
  fa.flatten

join(1.some.some)
```

どうせ関数にしてしまうのなら、関数構文をそのまま使えばいい。

```scala mdoc
FlatMap[Option].flatten(1.some.some)
```

#### filterM メソッド

LYAHFGG:

> `Control.Monad` モジュールの `filterM` こそ、まさにそのための関数です！
> ...
> 述語は `Bool` を結果とするモナド値を返しています。

Cats では `filterM` を提供しないが、[TraverseFilter][TraverseFilter] に `filterA` がある。

#### foldM 関数

LYAHFGG:

> `foldl` のモナド版が `foldM` です。

Cats には `foldM` が無いみたいだったので、自分で定義してみたけどもスタック・セーフじゃなかった。Tomas Mikula がそれを修正した実装を追加してくれて、それが [#925][925] でマージされた。

```scala
  /**
   * Left associative monadic folding on `F`.
   */
  def foldM[G[_], A, B](fa: F[A], z: B)(f: (B, A) => G[B])(implicit G: Monad[G]): G[B] =
    foldLeft(fa, G.pure(z))((gb, a) => G.flatMap(gb)(f(_, a)))
```

使ってみる。

```scala mdoc
def binSmalls(acc: Int, x: Int): Option[Int] =
  if (x > 9) none[Int] else (acc + x).some

(Foldable[List].foldM(List(2, 8, 3, 1), 0) {binSmalls})

(Foldable[List].foldM(List(2, 11, 3, 1), 0) {binSmalls})
```

上の例では `binSmals` が 9 より多きい数を見つけると `None` を返す。
