---
out: Validated.html
---

  [ValidatedSource]: $catsBaseUrl$core/src/main/scala/cats/data/Validated.scala

### Validated datatype

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
scala> import cats._, cats.data.Validated, cats.std.all._
scala> import Validated.{ valid, invalid }
scala> valid[String, String]("event 1 ok")
scala> invalid[String, String]("event 1 failed!")
```

What's different about `Validation` is that it is does not form a monad,
but forms an applicative functor.
Instead of chaining the result from first event to the next, `Validated` validates all events:

```console
scala> import cats.syntax.apply._
scala> val result = (valid[String, String]("event 1 ok") |@|
        invalid[String, String]("event 2 failed!") |@|
        invalid[String, String]("event 3 failed!")) map {_ + _ + _}
```

The final result is `Invalid(event 3 failed!event 2 failed!)`.
Unlike the `Xor`'s monad, which cuts the calculation short,
`Validated` keeps going to report back all failures.
This would be useful for validating user's inputs on an online bacon shop.

The problem, however, is that the error messages are mushed together into one string.
Shouldn't it be something like a list?

#### Using NonEmptyList to accumulate failures

This is where `NonEmptyList` datatype comes in handy.
For now, think of it as a list that's guaranteed to have at least one element.

```console
scala> import cats.data.{ NonEmptyList => NEL }
scala> NEL(1)
```

A semigroup should be formed for `NEL[A]` under `++` operation,
but it's not there by default, so we need to derive it off of `SemigroupK` as follows:

```console
scala> SemigroupK[NEL].algebra[String]
```

We can now use `NEL[A]` on the invalid side to accumulate the errors:

```console
scala> val result = {
         implicit val nelSemigroup: Semigroup[NEL[String]] = SemigroupK[NEL].algebra[String]
         (valid[NEL[String], String]("event 1 ok") |@|
           invalid[NEL[String], String](NEL("event 2 failed!")) |@|
           invalid[NEL[String], String](NEL("event 3 failed!"))) map {_ + _ + _}
       }
```

Inside `Invalid`, we were able to accumulate all failed messages.

We can use the `fold` method to extract the values:

```console
scala> val errs: NEL[String] = result.fold(
         { l => l },
         { r => sys.error("invalid is expected") }
       )
```

That's it for today! We'll pick it up from here later.
