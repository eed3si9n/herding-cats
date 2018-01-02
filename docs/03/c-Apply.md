
  [fafm]: http://learnyouahaskell.com/functors-applicative-functors-and-monoids
  [mootws]: making-our-own-typeclass-with-simulacrum.html

### Apply

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

Cats splits `Applicative` into `Cartesian`, `Apply`, and `Applicative`. Here's the contract for `Apply`:

```scala
/**
 * Weaker version of Applicative[F]; has apply but not pure.
 *
 * Must obey the laws defined in cats.laws.ApplyLaws.
 */
@typeclass(excludeParents = List("ApplyArityFunctions"))
trait Apply[F[_]] extends Functor[F] with Cartesian[F] with ApplyArityFunctions[F] { self =>

  /**
   * Given a value and a function in the Apply context, applies the
   * function to the value.
   */
  def ap[A, B](ff: F[A => B])(fa: F[A]): F[B]

  ....
}
```

Note that `Apply` extends `Functor`, `Cartesian`, and `ApplyArityFunctions`.
The `<*>` function is called `ap` in Cats' `Apply`. (This was originally called `apply`, but was renamed to `ap`. +1)

LYAHFGG:

> You can think of `<*>` as a sort of a beefed-up `fmap`. Whereas `fmap` takes a function and a functor and applies the function inside the functor value, `<*>` takes a functor that has a function in it and another functor and extracts that function from the first functor and then maps it over the second one.

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

Cats comes with the apply syntax.

```console
scala> (3.some, 5.some) mapN { _ - _ }
scala> (none[Int], 5.some) mapN { _ - _ }
scala> (3.some, none[Int]) mapN { _ - _ }
```

This shows that `Option` forms `Cartesian`.

#### List as a Apply

LYAHFGG:

> Lists (actually the list type constructor, `[]`) are applicative functors. What a surprise!

Let's see if we can use the `apply` sytax:

```console
scala> (List("ha", "heh", "hmm"), List("?", "!", ".")) mapN {_ + _}
```

#### `*>` and `<*` operators

`Apply` enables two operators, `<*` and `*>`, which are special cases of `Apply[F].map2`。

The definition looks simple enough, but the effect is cool:

```console
scala> 1.some <* 2.some
scala> none[Int] <* 2.some
scala> 1.some *> 2.some
scala> none[Int] *> 2.some
```

If either side fails, we get `None`.

#### Option syntax

Before we move on, let's look at the syntax that Cats adds to create an `Option` value.

```console
scala> 9.some
scala> none[Int]
```

We can write `(Some(9): Option[Int])` as `9.some`.

#### Option as an Apply

Here's how we can use it with `Apply[Option].ap`:

```console:new
scala> import cats._, cats.data._, cats.implicits._
scala> Apply[Option].ap({{(_: Int) + 3}.some })(9.some)
scala> Apply[Option].ap({{(_: Int) + 3}.some })(10.some)
scala> Apply[Option].ap({{(_: String) + "hahah"}.some })(none[String])
scala> Apply[Option].ap({ none[String => String] })("woot".some)
```

If either side fails, we get `None`.

If you remember [Making our own typeclass with simulacrum][mootws] from yesterday,
simulacrum will automatically transpose the function defined on
the typeclass contract into an operator, magically.

```console
scala> ({(_: Int) + 3}.some) ap 9.some
scala> ({(_: Int) + 3}.some) ap 10.some
scala> ({(_: String) + "hahah"}.some) ap none[String]
scala> (none[String => String]) ap "woot".some
```

#### Useful functions for Apply

LYAHFGG:

> `Control.Applicative` defines a function that's called `liftA2`, which has a type of

```haskell
liftA2 :: (Applicative f) => (a -> b -> c) -> f a -> f b -> f c .
```

Remember parameters are flipped around in Scala.
What we have is a function that takes `F[B]` and `F[A]`, then a function `(A, B) => C`.
This is called `map2` on `Apply`.


```scala
@typeclass(excludeParents = List("ApplyArityFunctions"))
trait Apply[F[_]] extends Functor[F] with Cartesian[F] with ApplyArityFunctions[F] { self =>
  def ap[A, B](ff: F[A => B])(fa: F[A]): F[B]

  def productR[A, B](fa: F[A])(fb: F[B]): F[B] =
    map2(fa, fb)((_, b) => b)

  def productL[A, B](fa: F[A])(fb: F[B]): F[A] =
    map2(fa, fb)((a, _) => a)

  override def product[A, B](fa: F[A], fb: F[B]): F[(A, B)] =
    ap(map(fa)(a => (b: B) => (a, b)))(fb)

  /** Alias for [[ap]]. */
  @inline final def <*>[A, B](ff: F[A => B])(fa: F[A]): F[B] =
    ap(ff)(fa)

  /** Alias for [[productR]]. */
  @inline final def *>[A, B](fa: F[A])(fb: F[B]): F[B] =
    productR(fa)(fb)

  /** Alias for [[productL]]. */
  @inline final def <*[A, B](fa: F[A])(fb: F[B]): F[A] =
    productL(fa)(fb)

  /**
   * ap2 is a binary version of ap, defined in terms of ap.
   */
  def ap2[A, B, Z](ff: F[(A, B) => Z])(fa: F[A], fb: F[B]): F[Z] =
    map(product(fa, product(fb, ff))) { case (a, (b, f)) => f(a, b) }

  def map2[A, B, Z](fa: F[A], fb: F[B])(f: (A, B) => Z): F[Z] =
    map(product(fa, fb))(f.tupled)

  def map2Eval[A, B, Z](fa: F[A], fb: Eval[F[B]])(f: (A, B) => Z): Eval[F[Z]] =
    fb.map(fb => map2(fa, fb)(f))

  ....
}
```

For binary operators, `map2` can be used to hide the applicative style.
Here we can write the same thing in two different ways:

```console
scala> (3.some, List(4).some) mapN { _ :: _ }
scala> Apply[Option].map2(3.some, List(4).some) { _ :: _ }
```

The results match up.

The 2-parameter version of `Apply[F].ap` is called `Apply[F].ap2`:

```console
scala> Apply[Option].ap2({{ (_: Int) :: (_: List[Int]) }.some })(3.some, List(4).some)
```

There's a special case of `map2` called `tuple2`, which works like this:


```console
scala> Apply[Option].tuple2(1.some, 2.some)
scala> Apply[Option].tuple2(1.some, none[Int])
```

If you are wondering what happens when you have a function with more than two
parameters, note that `Apply[F[_]]` extends `ApplyArityFunctions[F]`.
This is auto-generated code that defines `ap3`, `map3`, `tuple3`, ... up to
`ap22`, `map22`, `tuple22`.

#### Apply law

`Apply` has a single law called composition:

```scala
trait ApplyLaws[F[_]] extends FunctorLaws[F] {
  implicit override def F: Apply[F]

  def applyComposition[A, B, C](fa: F[A], fab: F[A => B], fbc: F[B => C]): IsEq[F[C]] = {
    val compose: (B => C) => (A => B) => (A => C) = _.compose
    fa.ap(fab).ap(fbc) <-> fa.ap(fab.ap(fbc.map(compose)))
  }
}
```
