---
out: TraverseEmpty.html
---

### TraverseEmpty

Cats 1.0 では、`traverseFilter` 機能は cats-mtl の syntax へ移行した。

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

`filterA` は `filterM` をより一般化 (もしくは弱く) したバージョンで、`Monad[G]` の代わりに `Applicative[G]` を要求する。

以下のように使うことができる:

```console
scala> import cats._, cats.data._, cats.implicits._
scala> import cats.mtl._, cats.mtl.implicits._
scala> List(1, 2, 3) filterA { x => List(true, false) }
scala> Vector(1, 2, 3) filterA { x => Vector(true, false) }
```
