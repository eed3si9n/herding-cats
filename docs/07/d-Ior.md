---
out: Ior.html
---

  [IorSource]: $catsBaseUrl$core/src/main/scala/cats/data/Ior.scala

### Ior datatype

In Cats there is yet another datatype that represents an `A`-`B` pair called [Ior][IorSource].

```scala
/** Represents a right-biased disjunction that is either an `A`, or a `B`, or both an `A` and a `B`.
 *
 * An instance of `A [[Ior]] B` is one of:
 *  - `[[Ior.Left Left]][A]`
 *  - `[[Ior.Right Right]][B]`
 *  - `[[Ior.Both Both]][A, B]`
 *
 * `A [[Ior]] B` is similar to `A [[Xor]] B`, except that it can represent the simultaneous presence of
 * an `A` and a `B`. It is right-biased like [[Xor]], so methods such as `map` and `flatMap` operate on the
 * `B` value. Some methods, like `flatMap`, handle the presence of two [[Ior.Both Both]] values using a
 * `[[Semigroup]][A]`, while other methods, like [[toXor]], ignore the `A` value in a [[Ior.Both Both]].
 *
 * `A [[Ior]] B` is isomorphic to `(A [[Xor]] B) [[Xor]] (A, B)`, but provides methods biased toward `B`
 * values, regardless of whether the `B` values appear in a [[Ior.Right Right]] or a [[Ior.Both Both]].
 * The isomorphic [[Xor]] form can be accessed via the [[unwrap]] method.
 */
sealed abstract class Ior[+A, +B] extends Product with Serializable {

  final def fold[C](fa: A => C, fb: B => C, fab: (A, B) => C): C = this match {
    case Ior.Left(a) => fa(a)
    case Ior.Right(b) => fb(b)
    case Ior.Both(a, b) => fab(a, b)
  }

  final def isLeft: Boolean = fold(_ => true, _ => false, (_, _) => false)
  final def isRight: Boolean = fold(_ => false, _ => true, (_, _) => false)
  final def isBoth: Boolean = fold(_ => false, _ => false, (_, _) => true)

  ....
}

object Ior extends IorInstances with IorFunctions {
  final case class Left[+A](a: A) extends (A Ior Nothing)
  final case class Right[+B](b: B) extends (Nothing Ior B)
  final case class Both[+A, +B](a: A, b: B) extends (A Ior B)
}
```

These values are created using the `left`, `right`, and `both` methods on `Ior`:

```scala mdoc
import cats._, cats.data._, cats.syntax.all._

import cats.data.{ NonEmptyList => NEL }

Ior.right[NEL[String], Int](1)

Ior.left[NEL[String], Int](NEL.of("error"))

Ior.both[NEL[String], Int](NEL.of("warning"), 1)
```

As noted in the scaladoc comment, `Ior`'s `flatMap` uses `Semigroup[A]` to accumulate
failures when it sees an `Ior.both(...)` value.
So we could probably use this as a hybrid of `Xor` and `Validated`.

Here's how `flatMap` behaves for all nine combinations:

```scala mdoc
Ior.right[NEL[String], Int](1) >>=
  { x => Ior.right[NEL[String], Int](x + 1) }

Ior.left[NEL[String], Int](NEL.of("error 1")) >>=
  { x => Ior.right[NEL[String], Int](x + 1) }

Ior.both[NEL[String], Int](NEL.of("warning 1"), 1) >>=
  { x => Ior.right[NEL[String], Int](x + 1) }

Ior.right[NEL[String], Int](1) >>=
  { x => Ior.left[NEL[String], Int](NEL.of("error 2")) }

Ior.left[NEL[String], Int](NEL.of("error 1")) >>=
  { x => Ior.left[NEL[String], Int](NEL.of("error 2")) }

Ior.both[NEL[String], Int](NEL.of("warning 1"), 1) >>=
  { x => Ior.left[NEL[String], Int](NEL.of("error 2")) }

Ior.right[NEL[String], Int](1) >>=
  { x => Ior.both[NEL[String], Int](NEL.of("warning 2"), x + 1) }

Ior.left[NEL[String], Int](NEL.of("error 1")) >>=
  { x => Ior.both[NEL[String], Int](NEL.of("warning 2"), x + 1) }

Ior.both[NEL[String], Int](NEL.of("warning 1"), 1) >>=
  { x => Ior.both[NEL[String], Int](NEL.of("warning 2"), x + 1) }
```

Let's try using it in `for` comprehension:

```scala mdoc
for {
  e1 <- Ior.right[NEL[String], Int](1)
  e2 <- Ior.both[NEL[String], Int](NEL.of("event 2 warning"), e1 + 1)
  e3 <- Ior.both[NEL[String], Int](NEL.of("event 3 warning"), e2 + 1)
} yield (e1 |+| e2 |+| e3)
```

So `Ior.left` short curcuits like the failure values in `Xor[A, B]` and `Either[A, B]`,
but `Ior.both` accumulates the failure values like `Validated[A, B]`.

That's it for today! We'll pick it up from here later.
