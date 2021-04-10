---
out: TraverseFilter.html
---

### TraverseFilter

```scala
/**
 * `TraverseFilter`, also known as `Witherable`, represents list-like structures
 * that can essentially have a `traverse` and a `filter` applied as a single
 * combined operation (`traverseFilter`).
 *
 * Based on Haskell's [[https://hackage.haskell.org/package/witherable-0.1.3.3/docs/Data-Witherable.html Data.Witherable]]
 */

@implicitNotFound("Could not find an instance of TraverseFilter for ${F}")
@typeclass
trait TraverseFilter[F[_]] extends FunctorFilter[F] {
  def traverse: Traverse[F]

  final override def functor: Functor[F] = traverse

  /**
   * A combined [[traverse]] and [[filter]]. Filtering is handled via `Option`
   * instead of `Boolean` such that the output type `B` can be different than
   * the input type `A`.
   *
   * Example:
   * {{{
   * scala> import cats.implicits._
   * scala> val m: Map[Int, String] = Map(1 -> "one", 3 -> "three")
   * scala> val l: List[Int] = List(1, 2, 3, 4)
   * scala> def asString(i: Int): Eval[Option[String]] = Now(m.get(i))
   * scala> val result: Eval[List[String]] = l.traverseFilter(asString)
   * scala> result.value
   * res0: List[String] = List(one, three)
   * }}}
   */
  def traverseFilter[G[_], A, B](fa: F[A])(f: A => G[Option[B]])(implicit G: Applicative[G]): G[F[B]]

  def sequenceFilter[G[_], A](fgoa: F[G[Option[A]]])(implicit G: Applicative[G]): G[F[A]] =
    traverseFilter(fgoa)(identity)

  def filterA[G[_], A](fa: F[A])(f: A => G[Boolean])(implicit G: Applicative[G]): G[F[A]] =
    traverseFilter(fa)(a => G.map(f(a))(if (_) Some(a) else None))

  def traverseEither[G[_], A, B, E](
    fa: F[A]
  )(f: A => G[Either[E, B]])(g: (A, E) => G[Unit])(implicit G: Monad[G]): G[F[B]] =
    traverseFilter(fa)(a =>
      G.flatMap(f(a)) {
        case Left(e)  => G.as(g(a, e), Option.empty[B])
        case Right(b) => G.pure(Some(b))
      }
    )

  override def mapFilter[A, B](fa: F[A])(f: A => Option[B]): F[B] =
    traverseFilter[Id, A, B](fa)(f)

  /**
   * Removes duplicate elements from a list, keeping only the first occurrence.
   */
  def ordDistinct[A](fa: F[A])(implicit O: Order[A]): F[A] = {
    implicit val ord: Ordering[A] = O.toOrdering

    traverseFilter[State[TreeSet[A], *], A, A](fa)(a =>
      State(alreadyIn => if (alreadyIn(a)) (alreadyIn, None) else (alreadyIn + a, Some(a)))
    )
      .run(TreeSet.empty)
      .value
      ._2
  }

  /**
   * Removes duplicate elements from a list, keeping only the first occurrence.
   * This is usually faster than ordDistinct, especially for things that have a slow comparion (like String).
   */
  def hashDistinct[A](fa: F[A])(implicit H: Hash[A]): F[A] =
    traverseFilter[State[HashSet[A], *], A, A](fa)(a =>
      State(alreadyIn => if (alreadyIn(a)) (alreadyIn, None) else (alreadyIn + a, Some(a)))
    )
      .run(HashSet.empty)
      .value
      ._2
}
```

#### filterA

`filterA` は `filterM` をより一般化 (もしくは弱く) したバージョンで、`Monad[G]` の代わりに `Applicative[G]` を要求する。

以下のように使うことができる:

```scala mdoc
import cats._, cats.syntax.all._

List(1, 2, 3) filterA { x => List(true, false) }

Vector(1, 2, 3) filterA { x => Vector(true, false) }
```
