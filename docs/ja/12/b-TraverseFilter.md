---
out: TraverseFilter.html
---

### TraverseFilter

Cats 0.7.0 より TraverseFilter という新しい型クラスができた。

```scala
/**
 * `TraverseFilter`, also known as `Witherable`, represents list-like structures
 * that can essentially have a [[traverse]] and a [[filter]] applied as a single
 * combined operation ([[traverseFilter]]).
 *
 * Must obey the laws defined in cats.laws.TraverseFilterLaws.
 *
 * Based on Haskell's [[https://hackage.haskell.org/package/witherable-0.1.3.3/docs/Data-Witherable.html Data.Witherable]]
 */
@typeclass trait TraverseFilter[F[_]] extends Traverse[F] with FunctorFilter[F] { self =>

  /**
   * A combined [[traverse]] and [[filter]]. Filtering is handled via `Option`
   * instead of `Boolean` such that the output type `B` can be different than
   * the input type `A`.
   */
  def traverseFilter[G[_]: Applicative, A, B](fa: F[A])(f: A => G[Option[B]]): G[F[B]]


  /**
   *
   * Filter values inside a `G` context.
   *
   * This is a generalized version of Haskell's [[http://hackage.haskell.org/package/base-4.9.0.0/docs/Control-Monad.html#v:filterM filterM]].
   * [[http://stackoverflow.com/questions/28872396/haskells-filterm-with-filterm-x-true-false-1-2-3 This StackOverflow question]] about `filterM` may be helpful in understanding how it behaves.
   */
  def filterA[G[_], A](fa: F[A])(f: A => G[Boolean])(implicit G: Applicative[G]): G[F[A]] =
    traverseFilter(fa)(a => G.map(f(a))(if (_) Some(a) else None))

}
```

#### filterA

コメントに書いてある通り `filterA` は `filterM` をより一般化 (もしくは弱く) したバージョンで、`Monad[G]` の代わりに `Applicative[G]` を要求する。

以下のように使うことができる:

```console
scala> import cats._, cats.data._, cats.implicits._
scala> List(1, 2, 3) filterA { x => List(true, false) }
scala> Vector(1, 2, 3) filterA { x => Vector(true, false) }
```
