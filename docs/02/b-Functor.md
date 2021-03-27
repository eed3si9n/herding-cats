
  [FunctorSource]: $catsBaseUrl$/core/src/main/scala/cats/Functor.scala
  [@blaisorblade]: https://twitter.com/blaisorblade
  [fafm]: http://learnyouahaskell.com/functors-applicative-functors-and-monoids

### Functor

LYAHFGG:

> And now, we're going to take a look at the `Functor` typeclass, which is basically for things that can be mapped over.

Like the book let's look [how it's implemented][FunctorSource]:

```scala
/**
 * Functor.
 *
 * The name is short for "covariant functor".
 *
 * Must obey the laws defined in cats.laws.FunctorLaws.
 */
@typeclass trait Functor[F[_]] extends functor.Invariant[F] { self =>
  def map[A, B](fa: F[A])(f: A => B): F[B]

  ....
}
```

Here's how we can use this:

```scala mdoc
import cats._, cats.syntax.all._

Functor[List].map(List(1, 2, 3)) { _ + 1 }
```

Let's call the above usage the *function syntax*.

We now know that `@typeclass` annotation will automatically turn a `map` function into a `map` operator.
The `fa` part turns into the `this` of the method, and the second parameter list will now be
the parameter list of `map` operator:

```scala
// Supposed generated code
object Functor {
  trait Ops[F[_], A] {
    def typeClassInstance: Functor[F]
    def self: F[A]
    def map[B](f: A => B): F[B] = typeClassInstance.map(self)(f)
  }
}
```

This looks almost like the `map` method on Scala collection library,
except this `map` doesn't do the `CanBuildFrom` auto conversion.

#### Either as a functor

Cats defines a `Functor` instance for `Either[A, B]`.

```scala mdoc
(Right(1): Either[String, Int]) map { _ + 1 }

(Left("boom!"): Either[String, Int]) map { _ + 1 }
```

Note that the above demonstration only works because `Either[A, B]` at the moment
does not implement its own `map`.
If I used `List(1, 2, 3)` it will call List's implementation of `map` instead of
the `Functor[List]`'s `map`. Therefore, even though the operator syntax looks familiar,
we should either avoid using it unless you're sure that standard library doesn't implement the `map`
or you're using it from a polymorphic function.
One workaround is to opt for the function syntax.

#### Function as a functor

Cats also defines a `Functor` instance for `Function1`.

```scala mdoc
{
  val addOne: Int => Int = (x: Int) => x + 1
  val h: Int => Int = addOne map {_ * 7}
  h(3)
}
```

This is interesting. Basically `map` gives us a way to compose functions, except the order is in reverse from `f compose g`. Another way of looking at `Function1` is that it's an infinite map from the domain to the range. Now let's skip the input and output stuff and go to [Functors, Applicative Functors and Monoids][fafm].

> How are functions functors?
> ...
>
> What does the type `fmap :: (a -> b) -> (r -> a) -> (r -> b)` for this instance tell us? Well, we see that it takes a function from `a` to `b` and a function from `r` to `a` and returns a function from `r` to `b`. Does this remind you of anything? Yes! Function composition!

Oh man, LYAHFGG came to the same conclusion as I did about the function composition. But wait...

```haskell
ghci> fmap (*3) (+100) 1
303
ghci> (*3) . (+100) \$ 1
303
```

In Haskell, the `fmap` seems to be working in the same order as `f compose g`. Let's check in Scala using the same numbers:

```scala mdoc
{
  (((_: Int) * 3) map {_ + 100}) (1)
}
```

Something is not right. Let's compare the declaration of `fmap` and Cats' `map` function:

```haskell
fmap :: (a -> b) -> f a -> f b
```

and here's Cats:

```scala
def map[A, B](fa: F[A])(f: A => B): F[B]
```

So the order is flipped. Here's Paolo Giarrusso ([@blaisorblade][@blaisorblade])'s explanation:

> That's a common Haskell-vs-Scala difference.
>
> In Haskell, to help with point-free programming, the "data" argument usually comes last. For instance, I can write `map f . map g . map h` and get a list transformer, because the argument order is `map f list`. (Incidentally, map is an restriction of fmap to the List functor).
>
> In Scala instead, the "data" argument is usually the receiver. That's often also important to help type inference, so defining map as a method on functions would not bring you very far: think the mess Scala type inference would make of `(x => x + 1) map List(1, 2, 3)`.

This seems to be the popular explanation.

#### Lifting a function

LYAHFGG:

> [We can think of `fmap` as] a function that takes a function and returns a new function that's just like the old one, only it takes a functor as a parameter and returns a functor as the result. It takes an `a -> b` function and returns a function `f a -> f b`. This is called *lifting* a function.

```haskell
ghci> :t fmap (*2)
fmap (*2) :: (Num a, Functor f) => f a -> f a
ghci> :t fmap (replicate 3)
fmap (replicate 3) :: (Functor f) => f a -> f [a]
```

If the parameter order has been flipped, are we going to miss out on this lifting goodness?
Fortunately, Cats implements derived functions under the `Functor` typeclass:

```scala
@typeclass trait Functor[F[_]] extends functor.Invariant[F] { self =>
  def map[A, B](fa: F[A])(f: A => B): F[B]

  ....

  // derived methods

  /**
   * Lift a function f to operate on Functors
   */
  def lift[A, B](f: A => B): F[A] => F[B] = map(_)(f)

  /**
   * Empty the fa of the values, preserving the structure
   */
  def void[A](fa: F[A]): F[Unit] = map(fa)(_ => ())

  /**
   * Tuple the values in fa with the result of applying a function
   * with the value
   */
  def fproduct[A, B](fa: F[A])(f: A => B): F[(A, B)] = map(fa)(a => a -> f(a))

  /**
   * Replaces the `A` value in `F[A]` with the supplied value.
   */
  def as[A, B](fa: F[A], b: B): F[B] = map(fa)(_ => b)
}
```

As you see, we have `lift`!

```scala mdoc
{
  val lifted = Functor[List].lift {(_: Int) * 2}
  lifted(List(1, 2, 3))
}
```

We've just lifted the function `{(_: Int) * 2}` to `List[Int] => List[Int]`. Here the other derived functions using the operator syntax:

```scala mdoc
List(1, 2, 3).void

List(1, 2, 3) fproduct {(_: Int) * 2}

List(1, 2, 3) as "x"
```

### Functor Laws

LYAHFGG:

> In order for something to be a functor, it should satisfy some laws.
> All functors are expected to exhibit certain kinds of functor-like properties and behaviors.
> ...
> The first functor law states that if we map the id function over a functor, the functor that we get back should be the same as the original functor.

We can check this for `Either[A, B]`.

```scala mdoc
val x: Either[String, Int] = Right(1)

assert { (x map identity) === x }
```

> The second law says that composing two functions and then mapping the resulting function over a functor should be the same as first mapping one function over the functor and then mapping the other one.

In other words,

```scala mdoc
val f = {(_: Int) * 3}

val g = {(_: Int) + 1}

assert { (x map (f map g)) === (x map f map g) }
```

These are laws the implementer of the functors must abide, and not something the compiler can check for you.
