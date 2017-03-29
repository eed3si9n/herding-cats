---
out: Cartesian.html
---

  [fafm]: http://learnyouahaskell.com/functors-applicative-functors-and-monoids
  [mootws]: making-our-own-typeclass-with-simulacrum.html

### Cartesian

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

Cats splits this into `Cartesian`, `Apply`, and `Applicative`. Here's the contract for `Cartesian`:

```scala
/**
 * [[Cartesian]] captures the idea of composing independent effectful values.
 * It is of particular interest when taken together with [[Functor]] - where [[Functor]]
 * captures the idea of applying a unary pure function to an effectful value,
 * calling `product` with `map` allows one to apply a function of arbitrary arity to multiple
 * independent effectful values.
 *
 * That same idea is also manifested in the form of [[Apply]], and indeed [[Apply]] extends both
 * [[Cartesian]] and [[Functor]] to illustrate this.
 */
@typeclass trait Cartesian[F[_]] {
  def product[A, B](fa: F[A], fb: F[B]): F[(A, B)]
}
```

Cartesian defines `product` function, which produces a pair of `(A, B)` wrapped in effect `F[_]` out of `F[A]` and `F[B]`. The symbolic alias for `product` is `|@|` also known as the applicative style.

#### Option syntax

Before we move on, let's look at the syntax that Cats adds to create an `Option` value.

```console
scala> 9.some
scala> none[Int]
```

We can write `(Some(9): Option[Int])` as `9.some`.

#### The Applicative Style

LYAHFGG:

> With the `Applicative` type class, we can chain the use of the
> `<*>` function, thus enabling us to seamlessly operate on several applicative
> values instead of just one.

Here's an example in Haskell:

```haskell
ghci> pure (-) <*> Just 3 <*> Just 5
Just (-2)
```

Cats comes with the `CartesianBuilder` syntax.

```console
scala> (3.some |@| 5.some) map { _ - _ }
scala> (none[Int] |@| 5.some) map { _ - _ }
scala> (3.some |@| none[Int]) map { _ - _ }
```

This shows that `Option` forms `Cartesian`.

#### List as a Cartesian

LYAHFGG:

> Lists (actually the list type constructor, `[]`) are applicative functors. What a surprise!

Let's see if we can use the `CartesianBuilder` sytax:

```console
scala> (List("ha", "heh", "hmm") |@| List("?", "!", ".")) map {_ + _}
```

#### *> and <* operators

`Cartesian` enables two operators, `<*` and `*>`, which are special cases of `Apply[F].product`:

```scala
abstract class CartesianOps[F[_], A] extends Cartesian.Ops[F, A] {
  def |@|[B](fb: F[B]): CartesianBuilder[F]#CartesianBuilder2[A, B] =
    new CartesianBuilder[F] |@| self |@| fb

  def *>[B](fb: F[B])(implicit F: Functor[F]): F[B] = F.map(typeClassInstance.product(self, fb)) { case (a, b) => b }

  def <*[B](fb: F[B])(implicit F: Functor[F]): F[A] = F.map(typeClassInstance.product(self, fb)) { case (a, b) => a }
}
```

The definition looks simple enough, but the effect is cool:

```console
scala> 1.some <* 2.some
scala> none[Int] <* 2.some
scala> 1.some *> 2.some
scala> none[Int] *> 2.some
```

If either side fails, we get `None`.

#### Cartesian law

`Cartesian` has a single law called associativity:

```scala
trait CartesianLaws[F[_]] {
  implicit def F: Cartesian[F]

  def cartesianAssociativity[A, B, C](fa: F[A], fb: F[B], fc: F[C]): (F[(A, (B, C))], F[((A, B), C)]) =
    (F.product(fa, F.product(fb, fc)), F.product(F.product(fa, fb), fc))
}
```
