---
out: combining-applicative.html
---

  [388]: https://github.com/typelevel/cats/pull/388

### Combining applicative functors

EIP:

> Like monads, applicative functors are closed under products; so two independent
> idiomatic effects can generally be fused into one, their product.

Cats seems to be missing the functor products altogether.

#### Product of functors

<s>Let's try implementing one.</s> (The impelementation I wrote here got merged into Cats in [#388][388])

```scala
/**
 * [[Prod]] is a product to two independent functor values.
 *
 * See: [[https://www.cs.ox.ac.uk/jeremy.gibbons/publications/iterator.pdf The Essence of the Iterator Pattern]]
 */
sealed trait Prod[F[_], G[_], A] {
  def first: F[A]
  def second: G[A]
}
object Prod extends ProdInstances {
  def apply[F[_], G[_], A](first0: => F[A], second0: => G[A]): Prod[F, G, A] = new Prod[F, G, A] {
    val firstThunk: Eval[F[A]] = Later(first0)
    val secondThunk: Eval[G[A]] = Later(second0)
    def first: F[A] = firstThunk.value
    def second: G[A] = secondThunk.value
  }
  def unapply[F[_], G[_], A](x: Prod[F, G, A]): Option[(F[A], G[A])] =
    Some((x.first, x.second))
}
```

First we start with the product of `Functor`:

```scala
private[data] sealed abstract class ProdInstances4 {
  implicit def prodFunctor[F[_], G[_]](implicit FF: Functor[F], GG: Functor[G]): Functor[Lambda[X => Prod[F, G, X]]] = new ProdFunctor[F, G] {
    def F: Functor[F] = FF
    def G: Functor[G] = GG
  }
}

sealed trait ProdFunctor[F[_], G[_]] extends Functor[Lambda[X => Prod[F, G, X]]] {
  def F: Functor[F]
  def G: Functor[G]
  def map[A, B](fa: Prod[F, G, A])(f: A => B): Prod[F, G, B] = Prod(F.map(fa.first)(f), G.map(fa.second)(f))
}
```

Here's how to use it:

```console:new
scala> import cats._, cats.std.all._
scala> import cats.data.Prod
scala> val x = Prod(List(1), (Some(1): Option[Int]))
scala> Functor[Lambda[X => Prod[List, Option, X]]].map(x) { _ + 1 }
```

First, we are defining a pair-like datatype called `Prod`, which prepresents a product of typeclass instances.
By simply passing the function `f` to both the sides, we can form `Functor` for `Prod[F, G]`
where `F` and `G` are `Functor`.

To see if it worked, we are mapping over `x` and adding `1`.
We could make the usage code a bit nicer if we wanted,
but it's ok for now.

#### Product of apply functors

Next up is `Apply`:

```scala
private[data] sealed abstract class ProdInstances3 extends ProdInstances4 {
  implicit def prodApply[F[_], G[_]](implicit FF: Apply[F], GG: Apply[G]): Apply[Lambda[X => Prod[F, G, X]]] = new ProdApply[F, G] {
    def F: Apply[F] = FF
    def G: Apply[G] = GG
  }
}

sealed trait ProdApply[F[_], G[_]] extends Apply[Lambda[X => Prod[F, G, X]]] with ProdFunctor[F, G] {
  def F: Apply[F]
  def G: Apply[G]
  def ap[A, B](fa: Prod[F, G, A])(f: Prod[F, G, A => B]): Prod[F, G, B] =
    Prod(F.ap(fa.first)(f.first), G.ap(fa.second)(f.second))
  def product[A, B](fa: Prod[F, G, A], fb: Prod[F, G, B]): Prod[F, G, (A, B)] =
    Prod(F.product(fa.first, fb.first), G.product(fa.second, fb.second))
}
```

Here's the usage:

```console
scala> val x = Prod(List(1), (Some(1): Option[Int]))
scala> val f = Prod(List((_: Int) + 1), (Some((_: Int) * 3): Option[Int => Int]))
scala> Apply[Lambda[X => Prod[List, Option, X]]].ap(x)(f)
```

The product of `Apply` passed in separate functions to each side.

#### Product of applicative functors

Finally we can implement the product of `Applicative`:

```scala
private[data] sealed abstract class ProdInstances2 extends ProdInstances3 {
  implicit def prodApplicative[F[_], G[_]](implicit FF: Applicative[F], GG: Applicative[G]): Applicative[Lambda[X => Prod[F, G, X]]] = new ProdApplicative[F, G] {
    def F: Applicative[F] = FF
    def G: Applicative[G] = GG
  }
}

sealed trait ProdApplicative[F[_], G[_]] extends Applicative[Lambda[X => Prod[F, G, X]]] with ProdApply[F, G] {
  def F: Applicative[F]
  def G: Applicative[G]
  def pure[A](a: A): Prod[F, G, A] = Prod(F.pure(a), G.pure(a))
}
```

Here's a simple usage:

```console
scala> Applicative[Lambda[X => Prod[List, Option, X]]].pure(1)
```

We were able to create `Prod(List(1), Some(1))` by calling `pure(1)`.

#### Composition of Applicative

> Unlike monads in general, applicative functors are also closed under composition; so two sequentially-dependent
> idiomatic effects can generally be fused into one, their composition.

Thankfully Cats ships with the composition of `Applicatives`.
There's `compose` method in the typeclass instance:

```scala
@typeclass trait Applicative[F[_]] extends Apply[F] { self =>
  /**
   * `pure` lifts any value into the Applicative Functor
   *
   * Applicative[Option].pure(10) = Some(10)
   */
  def pure[A](x: A): F[A]

  /**
   * Two sequentially dependent Applicatives can be composed.
   *
   * The composition of Applicatives `F` and `G`, `F[G[x]]`, is also an Applicative
   *
   * Applicative[Option].compose[List].pure(10) = Some(List(10))
   */
  def compose[G[_]](implicit GG : Applicative[G]): Applicative[λ[α => F[G[α]]]] =
    new CompositeApplicative[F,G] {
      implicit def F: Applicative[F] = self
      implicit def G: Applicative[G] = GG
    }

  ....
}
```

Let's try this out.


```console
scala> Applicative[List].compose[Option].pure(1)
```

So much nicer.

#### Product of applicative functions

For some reason people seem to overlook is that Gibbons also introduces
applicative function composition operators.
An applicative function is a function in the form of `A => F[B]` where `F` forms an `Applicative`.
This is similar to `Kleisli` composition of monadic functions, but *better*.

Here's why.
`Kliesli` composition will let you compose `A => F[B]` and `B => F[C]` using `andThen`,
but note that `F` stays the same.
On the other hand, `AppFunc` composes `A => F[B]` and `B => G[C]`.

```scala
/**
 * [[Func]] is a function `A => F[B]`.
 *
 * See: [[https://www.cs.ox.ac.uk/jeremy.gibbons/publications/iterator.pdf The Essence of the Iterator Pattern]]
 */
sealed abstract class Func[F[_], A, B] { self =>
  def run: A => F[B]
  def map[C](f: B => C)(implicit FF: Functor[F]): Func[F, A, C] =
    Func.func(a => FF.map(self.run(a))(f))
}

object Func extends FuncInstances {
  /** function `A => F[B]. */
  def func[F[_], A, B](run0: A => F[B]): Func[F, A, B] =
    new Func[F, A, B] {
      def run: A => F[B] = run0
    }

  /** applicative function. */
  def appFunc[F[_], A, B](run0: A => F[B])(implicit FF: Applicative[F]): AppFunc[F, A, B] =
    new AppFunc[F, A, B] {
      def F: Applicative[F] = FF
      def run: A => F[B] = run0
    }

  /** applicative function using [[Unapply]]. */
  def appFuncU[A, R](f: A => R)(implicit RR: Unapply[Applicative, R]): AppFunc[RR.M, A, RR.A] =
    appFunc({ a: A => RR.subst(f(a)) })(RR.TC)
}

....

/**
 * An implementation of [[Func]] that's specialized to [[Applicative]].
 */
sealed abstract class AppFunc[F[_], A, B] extends Func[F, A, B] { self =>
  def F: Applicative[F]

  def product[G[_]](g: AppFunc[G, A, B]): AppFunc[Lambda[X => Prod[F, G, X]], A, B] =
    {
      implicit val FF: Applicative[F] = self.F
      implicit val GG: Applicative[G] = g.F
      Func.appFunc[Lambda[X => Prod[F, G, X]], A, B]{
        a: A => Prod(self.run(a), g.run(a))
      }
    }

  ....
}
```

Here's how we can use it:

```console
scala> import cats.data.Func
scala> val f = Func.appFunc { x: Int => List(x.toString + "!") }
scala> val g = Func.appFunc { x: Int => (Some(x.toString + "?"): Option[String]) }
scala> val h = f product g
scala> h.run(1)
```

As you can see two applicative functions are running side by side.

#### Composition of applicative functions

Here's `andThen` and `compose`:

```scala
  def compose[G[_], C](g: AppFunc[G, C, A]): AppFunc[Lambda[X => G[F[X]]], C, B] =
    {
      implicit val FF: Applicative[F] = self.F
      implicit val GG: Applicative[G] = g.F
      implicit val GGFF: Applicative[Lambda[X => G[F[X]]]] = GG.compose(FF)
      Func.appFunc[Lambda[X => G[F[X]]], C, B]({
        c: C => GG.map(g.run(c))(self.run)
      })
    }

  def andThen[G[_], C](g: AppFunc[G, B, C]): AppFunc[Lambda[X => F[G[X]]], A, C] =
    g.compose(self)
```

```console
scala> val f = Func.appFunc { x: Int => List(x.toString + "!") }
scala> val g = Func.appFunc { x: String => (Some(x + "?"): Option[String]) }
scala> val h = f andThen g
scala> h.run(1)
```

EIP:

> The two operators [snip] allow us to combine idiomatic computations in two different ways;
> we call them *parallel* and *sequential* composition, respectively.

Even though we had to introduce a new datatype `Prod`,
the combining applicative computation is an abstract concept for all `Applicative`.

We'll continue from here.
