  [Apply]: Apply.html
  [Cartesian]: Cartesian.html
  [fafm]: http://learnyouahaskell.com/functors-applicative-functors-and-monoids

### Applicative

**Note**: If you jumped to this page because you're interested in applicative functors,
you should definitely read [Cartesian][Cartesian] and [Apply][Apply] first.

[Functors, Applicative Functors and Monoids][fafm]:

> Meet the `Applicative` typeclass. It lies in the `Control.Applicative` module and it defines two methods, `pure` and `<*>`.

Let's see Cats' `Applicative`:

```scala
@typeclass trait Applicative[F[_]] extends Apply[F] { self =>
  /**
   * `pure` lifts any value into the Applicative Functor
   *
   * Applicative[Option].pure(10) = Some(10)
   */
  def pure[A](x: A): F[A]

  ....
}
```

It's an extension of `Apply` with `pure`.

LYAHFGG:

> `pure` should take a value of any type and return an applicative value with that value inside it. ... A better way of thinking about `pure` would be to say that it takes a value and puts it in some sort of default (or pure) context—a minimal context that still yields that value.

It seems like it's basically a constructor that takes value `A` and returns `F[A]`.

```console:new
scala> import cats._, cats.data._, cats.implicits._
scala> Applicative[List].pure(1)
scala> Applicative[Option].pure(1)
```

This actually comes in handy using `Apply[F].ap` so we can avoid calling `{{...}.some}`.

```console
scala> val F = Applicative[Option]
scala> F.ap({ F.pure((_: Int) + 3) })(F.pure(9))
```

We've abstracted `Option` away from the code.

#### Useful functions for Applicative

LYAHFGG:

> Let's try implementing a function that takes a list of applicatives and returns an applicative that has a list as its result value. We'll call it `sequenceA`.

```haskell
sequenceA :: (Applicative f) => [f a] -> f [a]  
sequenceA [] = pure []  
sequenceA (x:xs) = (:) <\$> x <*> sequenceA xs  
```

Let's try implementing this with Cats!

```console
scala> def sequenceA[F[_]: Applicative, A](list: List[F[A]]): F[List[A]] = list match {
         case Nil     => Applicative[F].pure(Nil: List[A])
         case x :: xs => (x, sequenceA(xs)) mapN {_ :: _} 
       }
```

Let's test it:

```console
scala> sequenceA(List(1.some, 2.some))
scala> sequenceA(List(3.some, none[Int], 1.some))
scala> sequenceA(List(List(1, 2, 3), List(4, 5, 6)))
```

We got the right answers. What's interesting here is that we did end up needing
`Applicative` after all, and `sequenceA` is generic in a typeclassy way.

> Using `sequenceA` is useful when we have a list of functions and we want
> to feed the same input to all of them and then view the list of results.

For `Function1` with `Int` fixed example, we need some type annotation:

```console
scala> val f = sequenceA[Function1[Int, ?], Int](List((_: Int) + 3, (_: Int) + 2, (_: Int) + 1))
scala> f(3)
```

#### Applicative Laws

Here are the laws for `Applicative`:

- identity: `pure id <*> v = v`
- homomorphism: `pure f <*> pure x = pure (f x)`
- interchange: `u <*> pure y = pure (\$ y) <*> u`

Cats defines another law

```scala
  def applicativeMap[A, B](fa: F[A], f: A => B): IsEq[F[B]] =
    fa.map(f) <-> fa.ap(F.pure(f))
```

This seem to say that if you combine `F.ap` and `F.pure`, you should get the same effect as `F.map`.

It took us a while, but I am glad we got this far. We'll pick it up from here later.
