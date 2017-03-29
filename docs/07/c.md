---
out: Validated.html
---

  [ValidatedSource]: $catsBaseUrl$core/src/main/scala/cats/data/Validated.scala

### Validated datatype

LYAHFGG:

> The `Either e a` type on the other hand, allows us to incorporate a context of possible failure to our values while also being able to attach values to the failure, so that they can describe what went wrong or provide some other useful info regarding the failure.

We know `Either[A, B]` from the standard library, and we've covered that
Cats implements a right-biased functor for it too.

There's another datatype in Cats that we can use in place of `Either` called [Validated][ValidatedSource]:

```scala
sealed abstract class Validated[+E, +A] extends Product with Serializable {

  def fold[B](fe: E => B, fa: A => B): B =
    this match {
      case Invalid(e) => fe(e)
      case Valid(a) => fa(a)
    }

  def isValid: Boolean = fold(_ => false, _ => true)
  def isInvalid: Boolean = fold(_ => true, _ => false)

  ....
}

object Validated extends ValidatedInstances with ValidatedFunctions{
  final case class Valid[+A](a: A) extends Validated[Nothing, A]
  final case class Invalid[+E](e: E) extends Validated[E, Nothing]
}
```

Here's how to create the values:

```console:new
scala> import cats._, cats.data._, cats.implicits._
scala> import Validated.{ valid, invalid }
scala> valid[String, String]("event 1 ok")
scala> invalid[String, String]("event 1 failed!")
```

What's different about `Validation` is that it is does not form a monad,
but forms an applicative functor.
Instead of chaining the result from first event to the next, `Validated` validates all events:

```console
scala> val result = (valid[String, String]("event 1 ok") |@|
        invalid[String, String]("event 2 failed!") |@|
        invalid[String, String]("event 3 failed!")) map {_ + _ + _}
```

The final result is `Invalid(event 3 failed!event 2 failed!)`.
Unlike the `Xor`'s monad, which cuts the calculation short,
`Validated` keeps going to report back all failures.
This would be useful for validating user input on an online bacon shop.

The problem, however, is that the error messages are mushed together into one string.
Shouldn't it be something like a list?

#### Using NonEmptyList to accumulate failures

This is where `NonEmptyList` datatype comes in handy.
For now, think of it as a list that's guaranteed to have at least one element.

```console
scala> import cats.data.{ NonEmptyList => NEL }
scala> NEL.of(1)
```

We can use `NEL[A]` on the invalid side to accumulate the errors:

```console
scala> val result =
         (valid[NEL[String], String]("event 1 ok") |@|
           invalid[NEL[String], String](NEL.of("event 2 failed!")) |@|
           invalid[NEL[String], String](NEL.of("event 3 failed!"))) map {_ + _ + _}
```

Inside `Invalid`, we were able to accumulate all failed messages.

We can use the `fold` method to extract the values:

```console
scala> val errs: NEL[String] = result.fold(
         { l => l },
         { r => sys.error("invalid is expected") }
       )
```
