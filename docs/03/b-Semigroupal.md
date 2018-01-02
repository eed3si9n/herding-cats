---
out: Semigroupal.html
---

  [fafm]: http://learnyouahaskell.com/functors-applicative-functors-and-monoids
  [mootws]: making-our-own-typeclass-with-simulacrum.html

### Semigroupal

[Functors, Applicative Functors and Monoids][fafm]:

> So far, when we were mapping functions over functors, we usually mapped functions that take only one parameter. But what happens when we map a function like `*`, which takes two parameters, over a functor?

```console
scala> import cats._, cats.data._, cats.implicits._
scala> val hs = Functor[List].map(List(1, 2, 3, 4)) ({(_: Int) * (_:Int)}.curried)
scala> Functor[List].map(hs) {_(9)}
```

LYAHFGG:

> But what if we have a functor value of `Just (3 *)` and a functor value of `Just 5`, and we want to take out the function from `Just(3 *)` and map it over `Just 5`?
>
> Meet the `Applicative` typeclass. It lies in the `Control.Applicative` module and it defines two methods, `pure` and `<*>`.

Cats splits this into `Semigroupal`, `Apply`, and `Applicative`. Here's the contract for `Cartesian`:

```scala
/**
 * [[Semigroupal]] captures the idea of composing independent effectful values.
 * It is of particular interest when taken together with [[Functor]] - where [[Functor]]
 * captures the idea of applying a unary pure function to an effectful value,
 * calling `product` with `map` allows one to apply a function of arbitrary arity to multiple
 * independent effectful values.
 *
 * That same idea is also manifested in the form of [[Apply]], and indeed [[Apply]] extends both
 * [[Semigroupal]] and [[Functor]] to illustrate this.
 */
@typeclass trait Semigroupal[F[_]] {
  def product[A, B](fa: F[A], fb: F[B]): F[(A, B)]
}
```

Semigroupal defines `product` function, which produces a pair of `(A, B)` wrapped in effect `F[_]` out of `F[A]` and `F[B]`.

#### Cartesian law

`Cartesian` has a single law called associativity:

```scala
trait CartesianLaws[F[_]] {
  implicit def F: Cartesian[F]

  def cartesianAssociativity[A, B, C](fa: F[A], fb: F[B], fc: F[C]): (F[(A, (B, C))], F[((A, B), C)]) =
    (F.product(fa, F.product(fb, fc)), F.product(F.product(fa, fb), fc))
}
```
