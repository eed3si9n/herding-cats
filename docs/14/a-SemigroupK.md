---
out: SemigroupK.html
---

  [Semigroup]: Semigroup.html

### SemigroupK

[Semigroup][Semigroup] we saw on day 4 is a bread and butter of functional programming that shows up in many places.

```scala mdoc
import cats._, cats.syntax.all._

List(1, 2, 3) |+| List(4, 5, 6)

"one" |+| "two"
```

There's a similar typeclass called `SemigroupK` for type constructors `F[_]`.


```scala
@typeclass trait SemigroupK[F[_]] { self =>

  /**
   * Combine two F[A] values.
   */
  @simulacrum.op("<+>", alias = true)
  def combineK[A](x: F[A], y: F[A]): F[A]

  /**
   * Given a type A, create a concrete Semigroup[F[A]].
   */
  def algebra[A]: Semigroup[F[A]] =
    new Semigroup[F[A]] {
      def combine(x: F[A], y: F[A]): F[A] = self.combineK(x, y)
    }
}
```


This enables `combineK` operator and its symbolic alias `<+>`. Let's try using this.

```scala mdoc
List(1, 2, 3) <+> List(4, 5, 6)
```

Unlike `Semigroup`, `SemigroupK` works regardless of the type parameter of `F[_]`.

#### Option as SemigroupK

`Option[A]` forms a `Semigroup` only when the type parameter `A` forms a `Semigroup`. Let's disrupt that by creating a datatype does not form a `Semigroup`:

```scala mdoc
case class Foo(x: String)
```

So this won't work:

```scala mdoc:fail
Foo("x").some |+| Foo("y").some
```

But this works fine:

```scala mdoc
Foo("x").some <+> Foo("y").some
```

There's also a subtle difference in the behaviors of two typeclasses.

```scala mdoc
1.some |+| 2.some

1.some <+> 2.some
```

The `Semigroup` will combine the inner value of the `Option` whereas `SemigroupK` will just pick the first one.

#### SemigroupK laws

```
trait SemigroupKLaws[F[_]] {
  implicit def F: SemigroupK[F]

  def semigroupKAssociative[A](a: F[A], b: F[A], c: F[A]): IsEq[F[A]] =
    F.combineK(F.combineK(a, b), c) <-> F.combineK(a, F.combineK(b, c))
}
```
