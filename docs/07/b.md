---
out: Xor.html
---

  [XorSource]: $catsBaseUrl$core/src/main/scala/cats/data/Xor.scala

### Xor datatype

LYAHFGG:

> The `Either e a` type on the other hand, allows us to incorporate a context of possible failure to our values while also being able to attach values to the failure, so that they can describe what went wrong or provide some other useful info regarding the failure.

We know `Either[A, B]` from the standard library, and we've covered that
Cats implements a right-biased functor for it too.
Cats also implements its own `Either` equivalent datatype named [Xor][XorSource]:

```scala
/** Represents a right-biased disjunction that is either an `A` or a `B`.
 *
 * An instance of `A [[Xor]] B` is either a `[[Xor.Left Left]][A]` or a `[[Xor.Right Right]][B]`.
 *
 * A common use of [[Xor]] is to explicitly represent the possibility of failure in a result as opposed to
 * throwing an exception.  By convention, [[Xor.Left Left]] is used for errors and [[Xor.Right Right]] is reserved for successes.
 * For example, a function that attempts to parse an integer from a string may have a return type of
 * `NumberFormatException [[Xor]] Int`. However, since there is no need to actually throw an exception, the type (`A`)
 * chosen for the "left" could be any type representing an error and has no need to actually extend `Exception`.
 *
 * `A [[Xor]] B` is isomorphic to `scala.Either[A, B]`, but [[Xor]] is right-biased, so methods such as `map` and
 * `flatMap` apply only in the context of the "right" case. This right bias makes [[Xor]] more convenient to use
 * than `scala.Either` in a monadic context. Methods such as `swap`, and `leftMap` provide functionality
 * that `scala.Either` exposes through left projections.
 */
sealed abstract class Xor[+A, +B] extends Product with Serializable {

  def fold[C](fa: A => C, fb: B => C): C = this match {
    case Xor.Left(a) => fa(a)
    case Xor.Right(b) => fb(b)
  }

  def isLeft: Boolean = fold(_ => true, _ => false)

  def isRight: Boolean = fold(_ => false, _ => true)

  def swap: B Xor A = fold(Xor.right, Xor.left)

  ....
}

object Xor extends XorInstances with XorFunctions {
  final case class Left[+A](a: A) extends (A Xor Nothing)
  final case class Right[+B](b: B) extends (Nothing Xor B)
}
```

These values are created using the `right` and `left` methods on `Xor`:

```console:new
scala> import cats._, cats.data.Xor, cats.std.all._
scala> Xor.right[String, Int](1)
scala> Xor.left[String, Int]("error")
```

Unlike standard library's `Either[A, B]`, Cats' `Xor` assumes
that you'd want to mostly want a right projection:

```console
scala> import cats.syntax.flatMap._
scala> Xor.left[String, Int]("boom") >>=
         { x => Xor.right[String, Int](x + 1) }
```

Let's try using it in `for` comprehension:

```console
scala> import cats.syntax.semigroup._
scala> for {
         e1 <- Xor.right[String, String]("event 1 ok")
         e2 <- Xor.left[String, String]("event 2 failed!")
         e3 <- Xor.left[String, String]("event 3 failed!")
       } yield (e1 |+| e2 |+| e3)
```

As you can see, the first failure rolls up as the final result.
How do we get the value out of `Xor`?

First there's `fold`:

```console
scala> val e1 = Xor.right[String, String]("event 1 ok")
scala> e1.fold(
         { l => l },
         { r => r + "!" })
```

We can also use `isRight` and `isLeft` method to check which side we are on:

```console
scala> e1.isRight
scala> e1.isLeft
```

To extract the value from the right, use `getOrElse(x)`:

```console
scala> e1.getOrElse("something good")
```

To extract the value from the left, use `swap` to make it right first:

```console
scala> val e2 = Xor.left[String, String]("event 2 failed!")
scala> e2.swap.getOrElse("something good")
```

As expected, `map` is also right-biased:

```console
scala> e1 map { _ + "!" }
scala> e2 map { _ + "!" }
```

To chain on the left side, there's `orElse`, which accepts `=> AA Xor BB` where `[AA >: A, BB >: B]`:

```console
scala> e2 orElse Xor.right[String, String]("event 2 retry ok")
```

The `Xor` datatype has more methods like `toEither` etc.
