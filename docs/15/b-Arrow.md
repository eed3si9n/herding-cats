---
out: Arrow.html
---

  [Arrow_tutorial]: http://www.haskell.org/haskellwiki/Arrow_tutorial

### Arrow

As we saw, an *arrow* (or *morphism*) is a mapping between a *domain* and a *codomain*.
Another way of thinking about it, is that its an abstract notion for things that behave like functions.

In Cats, an Arrow instance is provided for `Function1[A, B]`, `Kleisli[F[_], A, B]`, and `Cokleisli[F[_], A, B]`.

Here's the typeclass contract for `Arrow`:

```scala
package cats
package arrow

import cats.functor.Strong
import simulacrum.typeclass

@typeclass trait Arrow[F[_, _]] extends Split[F] with Strong[F] with Category[F] { self =>

  /**
   * Lift a function into the context of an Arrow
   */
  def lift[A, B](f: A => B): F[A, B]

  ....
}
```

### Category

Here's the typeclass contract for `Category`:

```scala
package cats
package arrow

import simulacrum.typeclass

/**
 * Must obey the laws defined in cats.laws.CategoryLaws.
 */
@typeclass trait Category[F[_, _]] extends Compose[F] { self =>

  def id[A]: F[A, A]

  ....
}
```

### Compose

Here's the typeclass contract for `Compose`:

```scala
package cats
package arrow

import simulacrum.typeclass

/**
 * Must obey the laws defined in cats.laws.ComposeLaws.
 */
@typeclass trait Compose[F[_, _]] { self =>

  @simulacrum.op("<<<", alias = true)
  def compose[A, B, C](f: F[B, C], g: F[A, B]): F[A, C]

  @simulacrum.op(">>>", alias = true)
  def andThen[A, B, C](f: F[A, B], g: F[B, C]): F[A, C] =
    compose(g, f)

  ....
}
```

This enables two operators `<<<` and `>>>`.

```scala mdoc
import cats._, cats.data._, cats.syntax.all._

lazy val f = (_:Int) + 1

lazy val g = (_:Int) * 100

(f >>> g)(2)

(f <<< g)(2)
```

### Strong

Let's read Haskell's [Arrow tutorial][Arrow_tutorial]:

> First and second make a new arrow out of an existing arrow. They perform a transformation (given by their argument) on either the first or the second item of a pair. These definitions are arrow-specific.

Here's Cat's `Strong`:

```scala
package cats
package functor

import simulacrum.typeclass

/**
 * Must obey the laws defined in cats.laws.StrongLaws.
 */
@typeclass trait Strong[F[_, _]] extends Profunctor[F] {

  /**
   * Create a new `F` that takes two inputs, but only modifies the first input
   */
  def first[A, B, C](fa: F[A, B]): F[(A, C), (B, C)]

  /**
   * Create a new `F` that takes two inputs, but only modifies the second input
   */
  def second[A, B, C](fa: F[A, B]): F[(C, A), (C, B)]
}
```

This enables two methods `first[C]` and `second[C]`.

```scala mdoc
lazy val f_first = f.first[Int]

f_first((1, 1))

lazy val f_second = f.second[Int]

f_second((1, 1))
```

Given that `f` here is a function to add one, I think it's clear what `f_first` and `f_second` are doing.

### Split

> `(***)` combines two arrows into a new arrow by running the two arrows on a pair of values (one arrow on the first item of the pair and one arrow on the second item of the pair).

This is called `split` in Cats.

```
package cats
package arrow

import simulacrum.typeclass

@typeclass trait Split[F[_, _]] extends Compose[F] { self =>

  /**
   * Create a new `F` that splits its input between `f` and `g`
   * and combines the output of each.
   */
  def split[A, B, C, D](f: F[A, B], g: F[C, D]): F[(A, C), (B, D)]
}
```

We can use it as `split` operator:

```scala mdoc
(f split g)((1, 1))
```
