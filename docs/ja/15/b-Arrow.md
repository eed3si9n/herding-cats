---
out: Arrow.html
---

  [Arrow_tutorial]: http://www.haskell.org/haskellwiki/Arrow_tutorial

### Arrow

これまで見たように、**射** (*arrow* もしくは *morphism*) は**ドメイン**と**コドメイン**間の写像だ。関数っぽい振る舞いをするものの抽象概念だと考えることもできる。

Cats では `Function1[A, B]`、 `Kleisli[F[_], A, B]`、 `Cokleisli[F[_], A, B]` などに対して Arrow のインスタンスが用意されている。

以下が `Arrow` の型クラスコントラクトだ:

```scala
package cats
package arrow

import cats.functor.Strong
import simulacrum.typeclass

@typeclass trait Arrow[F[_, _]] extends Split[F] with Strong[F] with Category[F] { self =>

  /**
   * Lift a function into the context of an Arrow
   */
  def lift[A, B](f: A => B): F[A, B]

  ....
}
```

### Category

以下は `Category` の型クラスコントラクトだ:

```scala
package cats
package arrow

import simulacrum.typeclass

/**
 * Must obey the laws defined in cats.laws.CategoryLaws.
 */
@typeclass trait Category[F[_, _]] extends Compose[F] { self =>

  def id[A]: F[A, A]

  ....
}
```

### Compose

以下は `Compose` の型クラスコントラクトだ:

```scala
package cats
package arrow

import simulacrum.typeclass

/**
 * Must obey the laws defined in cats.laws.ComposeLaws.
 */
@typeclass trait Compose[F[_, _]] { self =>

  @simulacrum.op("<<<", alias = true)
  def compose[A, B, C](f: F[B, C], g: F[A, B]): F[A, C]

  @simulacrum.op(">>>", alias = true)
  def andThen[A, B, C](f: F[A, B], g: F[B, C]): F[A, C] =
    compose(g, f)

  ....
}
```

これは `<<<` と `>>>` という2つの演算子を可能とする。

```console:new
scala> import cats._, cats.data._, cats.implicits._
scala> val f = (_:Int) + 1
scala> val g = (_:Int) * 100
scala> (f >>> g)(2)
scala> (f <<< g)(2)
```

### Strong

Haskell の [Arrow tutorial](http://www.haskell.org/haskellwiki/Arrow_tutorial) を読んでみる:

> first と second は既存の arrow より新たな arrow を作る。それらは、与えられた引数の1番目もしくは2番目の要素に対して変換を行う。その実際の定義は特定の arrow に依存する。

以下は Cats の `Strong` だ:

```scala
package cats
package functor

import simulacrum.typeclass

/**
 * Must obey the laws defined in cats.laws.StrongLaws.
 */
@typeclass trait Strong[F[_, _]] extends Profunctor[F] {

  /**
   * Create a new `F` that takes two inputs, but only modifies the first input
   */
  def first[A, B, C](fa: F[A, B]): F[(A, C), (B, C)]

  /**
   * Create a new `F` that takes two inputs, but only modifies the second input
   */
  def second[A, B, C](fa: F[A, B]): F[(C, A), (C, B)]
}
```

これは `first[C]` と `second[C]` というメソッドを可能とする。

```console
scala> val f_first = f.first[Int]
scala> f_first((1, 1))
scala> val f_second = f.second[Int]
scala> f_second((1, 1))
```

ここで `f` は 1を加算する関数であるため、`f_first` と `f_second` が何をやっているかは明らかだと思う。

### Split

> `(***)` は 2つの射を値のペアに対して (1つの射はペアの最初の項で、もう 1つの射はペアの 2つめの項で) 実行することで 1つの新しいに射へと組み合わせる。

Cats ではこれは `split` と呼ばれる。

```
package cats
package arrow

import simulacrum.typeclass

@typeclass trait Split[F[_, _]] extends Compose[F] { self =>

  /**
   * Create a new `F` that splits its input between `f` and `g`
   * and combines the output of each.
   */
  def split[A, B, C, D](f: F[A, B], g: F[C, D]): F[(A, C), (B, D)]
}
```

これは `split` 演算子として使うことができる:

```console
scala> (f split g)((1, 1))
```
