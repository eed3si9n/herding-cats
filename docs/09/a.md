---
out: monadic-functions.html
---

  [fafmm]: http://learnyouahaskell.com/for-a-few-monads-more
  [925]: https://github.com/typelevel/cats/pull/925
  [TraverseFilter]: TraverseFilter.html

### Some useful monadic functions

[Learn You a Haskell for Great Good][fafmm] says:

> In this section, we're going to explore a few functions that either operate on monadic values or return monadic values as their results (or both!). Such functions are usually referred to as *monadic functions*.

Unlike Haskell's standard `Monad`, Cats' `Monad`
is more granularly designed with the hindsight of
weaker typeclasses.

- `Monad`
- extends `FlatMap` and `Applicative`
- extends `Apply`
- extends `Functor`

Here there's no question that all monads are applicative functors
as well as functors. This means we can use `ap` or `map` operator
on the datatypes that form a monad.


#### flatten method

LYAHFGG:

> It turns out that any nested monadic value can be flattened and that this is actually a property unique to monads. For this, the `join` function exists.

In Cats, the equivalent function called `flatten` on `FlatMap`.
Thanks to simulacrum, `flatten` can also be injected as a method.

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

Since `Option[A]` already implements `flatten` we need to
make an abtract function to turn it into an abtract type.

```console:new
scala> import cats._, cats.data._, cats.implicits._
scala> def join[F[_]: FlatMap, A](fa: F[F[A]]): F[A] =
         fa.flatten
scala> join(1.some.some)
```

If I'm going to make it into a function,
I could've used the function syntax:

```console
scala> FlatMap[Option].flatten(1.some.some)
```

I tried using `flatten` method for an `Xor` of an `Xor` value,
but it didn't seem to work:

```console:error
scala> val xorOfXor = Xor.right[String, Xor[String, Int]](Xor.right[String, Int](1))
scala> xorOfXor.flatten
```

#### filterM method

LYAHFGG:

> The `filterM` function from `Control.Monad` does just what we want!
> ...
> The predicate returns a monadic value whose result is a `Bool`.

Cats does not have `filterM`, but there's `filterA` on [TraverseFilter][TraverseFilter].

#### foldM function

LYAHFGG:

> The monadic counterpart to `foldl` is `foldM`.

I did not find `foldM` in Cats, so implemented it myself, but it wasn't stack-safe. Tomas Mikula added a better implementation and that got merged as [#925][925].

:

```scala
  /**
   * Left associative monadic folding on `F`.
   */
  def foldM[G[_], A, B](fa: F[A], z: B)(f: (B, A) => G[B])(implicit G: Monad[G]): G[B] =
    foldLeft(fa, G.pure(z))((gb, a) => G.flatMap(gb)(f(_, a)))
```

Let's try using this.

```console
scala> def binSmalls(acc: Int, x: Int): Option[Int] =
         if (x > 9) none[Int] else (acc + x).some
scala> (Foldable[List].foldM(List(2, 8, 3, 1), 0) {binSmalls})
scala> (Foldable[List].foldM(List(2, 11, 3, 1), 0) {binSmalls})
```

In the above, `binSmalls` returns `None` when it finds a number larger than 9.
