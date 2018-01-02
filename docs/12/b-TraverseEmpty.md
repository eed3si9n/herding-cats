---
out: TraverseEmpty.html
---

### TraverseEmpty

In Cats 1.0, `traverseFilter` feature moved to the syntax of cats-mtl:

```scala
final class EmptyOps[F[_], A](val fa: F[A]) extends AnyVal {
  def traverseFilter[G[_] : Applicative, B](f: A => G[Option[B]])(implicit F: TraverseEmpty[F]): G[F[B]] = F.traverseFilter(fa)(f)

  def filterA[G[_]: Applicative](f: A => G[Boolean])(implicit F: TraverseEmpty[F]): G[F[A]] = F.filterA(fa)(f)

  def mapFilter[B](f: A => Option[B])(implicit F: FunctorEmpty[F]): F[B] = F.mapFilter(fa)(f)

  def collect[B](f: PartialFunction[A, B])(implicit F: FunctorEmpty[F]): F[B] = F.collect(fa)(f)

  def filter(f: A => Boolean)(implicit F: FunctorEmpty[F]): F[A] = F.filter(fa)(f)
}
```

#### filterA

`filterA` is a more generalized (or weaker) version of `filterM`. Instead of requiring a `Monad[G]` it needs `Applicative[G]`.

Here's how we can use this:

```console
scala> import cats._, cats.data._, cats.implicits._
scala> import cats.mtl._, cats.mtl.implicits._
scala> List(1, 2, 3) filterA { x => List(true, false) }
scala> Vector(1, 2, 3) filterA { x => Vector(true, false) }
```
