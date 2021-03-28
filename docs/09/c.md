---
out: composing-monadic-functions.html
---

  [KleisliSource]: $catsBaseUrl$/core/src/main/scala/cats/data/Kleisli.scala
  [354]: https://github.com/typelevel/cats/pull/354

### Composing monadic functions

LYAHFGG:

> When we were learning about the monad laws, we said that the `<=<` function is just like composition, only instead of working for normal functions like `a -> b`, it works for monadic functions like `a -> m b`.

In Cats there's a special wrapper for a function of type `A => F[B]` called [Kleisli][KleisliSource]:

```scala
/**
 * Represents a function `A => F[B]`.
 */
final case class Kleisli[F[_], A, B](run: A => F[B]) { self =>

  ....
}

object Kleisli extends KleisliInstances with KleisliFunctions

private[data] sealed trait KleisliFunctions {

  def pure[F[_], A, B](x: B)(implicit F: Applicative[F]): Kleisli[F, A, B] =
    Kleisli(_ => F.pure(x))

  def ask[F[_], A](implicit F: Applicative[F]): Kleisli[F, A, A] =
    Kleisli(F.pure)

  def local[M[_], A, R](f: R => R)(fa: Kleisli[M, R, A]): Kleisli[M, R, A] =
    Kleisli(f andThen fa.run)
}
```

We can use the `Kleisli()` constructor to construct a `Kliesli` value:

```scala mdoc
import cats._, cats.data._, cats.syntax.all._

val f = Kleisli { (x: Int) => (x + 1).some }

val g = Kleisli { (x: Int) => (x * 100).some }
```

We can then compose the functions using `compose`, which runs the right-hand side first:

```scala mdoc
4.some >>= (f compose g).run
```

There's also `andThen`, which runs the left-hand side first:

```scala mdoc
4.some >>= (f andThen g).run
```

Both `compose` and `andThen` work like function composition
but note that they retain the monadic context.

#### lift method

Kleisli also has some interesting methods like `lift`,
which allows you to lift a monadic function into another applicative functor.
When I tried using it, I realized it's broken, so here's the fixed version [#354][354]:

```scala
  def lift[G[_]](implicit G: Applicative[G]): Kleisli[λ[α => G[F[α]]], A, B] =
    Kleisli[λ[α => G[F[α]]], A, B](a => Applicative[G].pure(run(a)))
```

Here's how we can use it:

```scala mdoc
{
  val l = f.lift[List]

  List(1, 2, 3) >>= l.run
}
```
