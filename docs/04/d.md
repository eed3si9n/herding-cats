---
out: using-monoids-to-fold.html
---

  [FoldableSource]: $catsBaseUrl$/core/src/main/scala/cats/Foldable.scala

### Using monoids to fold data structures

LYAHFGG:

> Because there are so many data structures that work nicely with folds, the `Foldable` type class was introduced. Much like `Functor` is for things that can be mapped over, Foldable is for things that can be folded up!

The equivalent in Cats is also called `Foldable`. Here's the [typeclass contract][FoldableSource]:

```scala
/**
 * Data structures that can be folded to a summary value.
 *
 * In the case of a collection (such as `List` or `Set`), these
 * methods will fold together (combine) the values contained in the
 * collection to produce a single result. Most collection types have
 * `foldLeft` methods, which will usually be used by the associationed
 * `Fold[_]` instance.
 *
 * Foldable[F] is implemented in terms of two basic methods:
 *
 *  - `foldLeft(fa, b)(f)` eagerly folds `fa` from left-to-right.
 *  - `foldLazy(fa, b)(f)` lazily folds `fa` from right-to-left.
 *
 * Beyond these it provides many other useful methods related to
 * folding over F[A] values.
 *
 * See: [[https://www.cs.nott.ac.uk/~gmh/fold.pdf A tutorial on the universality and expressiveness of fold]]
 */
@typeclass trait Foldable[F[_]] extends Serializable { self =>

  /**
   * Left associative fold on 'F' using the function 'f'.
   */
  def foldLeft[A, B](fa: F[A], b: B)(f: (B, A) => B): B

  /**
   * Right associative lazy fold on `F` using the folding function 'f'.
   *
   * This method evaluates `b` lazily (in some cases it will not be
   * needed), and returns a lazy value. We are using `A => Fold[B]` to
   * support laziness in a stack-safe way.
   *
   * For more detailed information about how this method works see the
   * documentation for `Fold[_]`.
   */
  def foldLazy[A, B](fa: F[A], lb: Lazy[B])(f: A => Fold[B]): Lazy[B] =
    Lazy(partialFold[A, B](fa)(f).complete(lb))

  /**
   * Low-level method that powers `foldLazy`.
   */
  def partialFold[A, B](fa: F[A])(f: A => Fold[B]): Fold[B]
  ....
}
```

We can use this as follows:

```scala mdoc
import cats._, cats.syntax.all._

Foldable[List].foldLeft(List(1, 2, 3), 1) {_ * _}
```

`Foldable` comes with some useful functions/operators,
many of them taking advantage of the typeclasses.
Let's try the `fold`. `Monoid[A]` gives us `empty` and `combine`, so that's enough information to fold over things.

```scala
  /**
   * Fold implemented using the given Monoid[A] instance.
   */
  def fold[A](fa: F[A])(implicit A: Monoid[A]): A =
    foldLeft(fa, A.empty) { (acc, a) =>
      A.combine(acc, a)
    }
```

Let's try this out.

```scala mdoc
Foldable[List].fold(List(1, 2, 3))(Monoid[Int])
```

There's a variant called `foldMap` that accepts a function.

```scala
  /**
   * Fold implemented by mapping `A` values into `B` and then
   * combining them using the given `Monoid[B]` instance.
   */
  def foldMap[A, B](fa: F[A])(f: A => B)(implicit B: Monoid[B]): B =
    foldLeft(fa, B.empty) { (b, a) =>
      B.combine(b, f(a))
    }
```

Since the standard collection library doesn't implement `foldMap`,
we can now use this as an operator.

```scala mdoc
List(1, 2, 3).foldMap(identity)(Monoid[Int])
```

Another useful thing is that we can use this to convert
the values into newtype.

```scala mdoc
// `class Conjunction(val unwrap: Boolean) extends AnyVal` doesn't work on mdoc
class Conjunction(val unwrap: Boolean)

object Conjunction {
  @inline def apply(b: Boolean): Conjunction = new Conjunction(b)
  implicit val conjunctionMonoid: Monoid[Conjunction] = new Monoid[Conjunction] {
    def combine(a1: Conjunction, a2: Conjunction): Conjunction =
      Conjunction(a1.unwrap && a2.unwrap)
    def empty: Conjunction = Conjunction(true)
  }
  implicit val conjunctionEq: Eq[Conjunction] = new Eq[Conjunction] {
    def eqv(a1: Conjunction, a2: Conjunction): Boolean =
      a1.unwrap == a2.unwrap
  }
}

val x = List(true, false, true) foldMap {Conjunction(_)}
x.unwrap
```

This surely beats writing `Conjunction(true)` for each of them and connecting them with `|+|`.

We will pick it up from here later.
