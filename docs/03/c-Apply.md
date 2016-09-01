
  [fafm]: http://learnyouahaskell.com/functors-applicative-functors-and-monoids
  [mootws]: making-our-own-typeclass-with-simulacrum.html

### Apply

[Functors, Applicative Functors and Monoids][fafm]:

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

#### Option as an Apply

Here's how we can use it with `Apply[Option].ap`:

```console
scala> :paste
object Catnip {
  implicit class IdOp[A](val a: A) extends AnyVal {
    def some: Option[A] = Some(a)
  }
  def none[A]: Option[A] = None
}
import Catnip._
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
scala> import cats.syntax.apply._
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

  /**
   * Given a value and a function in the Apply context, applies the
   * function to the value.
   */
  def ap[A, B](ff: F[A => B])(fa: F[A]): F[B]

  override def product[A, B](fa: F[A], fb: F[B]): F[(A, B)] =
    ap(map(fa)(a => (b: B) => (a, b)))(fb)

  /**
   * ap2 is a binary version of ap, defined in terms of ap.
   */
  def ap2[A, B, Z](ff: F[(A, B) => Z])(fa: F[A], fb: F[B]): F[Z] =
    map(product(fa, product(fb, ff))) { case (a, (b, f)) => f(a, b) }

  /**
   * Applies the pure (binary) function f to the effectful values fa and fb.
   *
   * map2 can be seen as a binary version of [[cats.Functor]]#map.
   */
  def map2[A, B, Z](fa: F[A], fb: F[B])(f: (A, B) => Z): F[Z] =
    map(product(fa, fb)) { case (a, b) => f(a, b) }

  ....
}
```

For binary operators, `map2` can be used to hide the applicative style.
Here we can write the same thing in two different ways:

```console
scala> import cats.syntax.cartesian._
scala> (3.some |@| List(4).some) map { _ :: _ }
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
