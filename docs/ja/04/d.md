---
out: using-monoids-to-fold.html
---

  [FoldableSource]: $catsBaseUrl$/core/src/main/scala/cats/Foldable.scala

### モノイドを使ったデータ構造の畳み込み

LYAHFGG:

> 畳み込み相性の良いデータ構造は実にたくさんあるので、`Foldable` 型クラスが導入されました。`Functor` が関数で写せるものを表すように、`Foldable` は畳み込みできるものを表しています。

Cats でこれに対応するものも `Foldable` と呼ばれている。[型クラスのコントラクト][FoldableSource]も見てみよう:

```scala
/**
 * Data structures that can be folded to a summary value.
 *
 * In the case of a collection (such as `List` or `Set`), these
 * methods will fold together (combine) the values contained in the
 * collection to produce a single result. Most collection types have
 * `foldLeft` methods, which will usually be used by the associationed
 * `Fold[_]` instance.
 *
 * Foldable[F] is implemented in terms of two basic methods:
 *
 *  - `foldLeft(fa, b)(f)` eagerly folds `fa` from left-to-right.
 *  - `foldLazy(fa, b)(f)` lazily folds `fa` from right-to-left.
 *
 * Beyond these it provides many other useful methods related to
 * folding over F[A] values.
 *
 * See: [[https://www.cs.nott.ac.uk/~gmh/fold.pdf A tutorial on the universality and expressiveness of fold]]
 */
@typeclass trait Foldable[F[_]] extends Serializable { self =>

  /**
   * Left associative fold on 'F' using the function 'f'.
   */
  def foldLeft[A, B](fa: F[A], b: B)(f: (B, A) => B): B

  /**
   * Right associative lazy fold on `F` using the folding function 'f'.
   *
   * This method evaluates `b` lazily (in some cases it will not be
   * needed), and returns a lazy value. We are using `A => Fold[B]` to
   * support laziness in a stack-safe way.
   *
   * For more detailed information about how this method works see the
   * documentation for `Fold[_]`.
   */
  def foldLazy[A, B](fa: F[A], lb: Lazy[B])(f: A => Fold[B]): Lazy[B] =
    Lazy(partialFold[A, B](fa)(f).complete(lb))

  /**
   * Low-level method that powers `foldLazy`.
   */
  def partialFold[A, B](fa: F[A])(f: A => Fold[B]): Fold[B]
  ....
}
```

このように使う:

```console:new
scala> import cats._, cats.std.all._
scala> Foldable[List].foldLeft(List(1, 2, 3), 1) {_ * _}
```

`Foldable` はいくつかの便利な関数や演算子がついてきて、型クラスを駆使している。
まずは `fold`。`Monoid[A]` が `empty` と `combine` を提供するので、これだけで畳込みをすることができる。

```scala
  /**
   * Fold implemented using the given Monoid[A] instance.
   */
  def fold[A](fa: F[A])(implicit A: Monoid[A]): A =
    foldLeft(fa, A.empty) { (acc, a) =>
      A.combine(acc, a)
    }
```

使ってみる。

```console
scala> Foldable[List].fold(List(1, 2, 3))(Monoid[Int])
```

関数を受け取る変種として `foldMap` もある。

```scala
  /**
   * Fold implemented by mapping `A` values into `B` and then
   * combining them using the given `Monoid[B]` instance.
   */
  def foldMap[A, B](fa: F[A])(f: A => B)(implicit B: Monoid[B]): B =
    foldLeft(fa, B.empty) { (b, a) =>
      B.combine(b, f(a))
    }
```

標準のコレクションライブラリが `foldMap` を実装しないため、演算子として使える。

```console
scala> import cats.syntax.foldable._
scala> List(1, 2, 3).foldMap(identity)(Monoid[Int])
```

もう一つ便利なのは、これで値を newtype に変換することができることだ。

```console
scala> :paste
class Conjunction(val unwrap: Boolean) extends AnyVal
object Conjunction {
  @inline def apply(b: Boolean): Conjunction = new Conjunction(b)
  implicit val conjunctionMonoid: Monoid[Conjunction] = new Monoid[Conjunction] {
    def combine(a1: Conjunction, a2: Conjunction): Conjunction =
      Conjunction(a1.unwrap && a2.unwrap)
    def empty: Conjunction = Conjunction(true)
  }
  implicit val conjunctionEq: Eq[Conjunction] = new Eq[Conjunction] {
    def eqv(a1: Conjunction, a2: Conjunction): Boolean =
      a1.unwrap == a2.unwrap
  }
}
scala> val x = List(true, false, true) foldMap {Conjunction(_)}
scala> x.unwrap
```

`Conjunction(true)` と一つ一つ書きだして `|+|` でつなぐよりずっと楽だ。

続きはまた後で。
