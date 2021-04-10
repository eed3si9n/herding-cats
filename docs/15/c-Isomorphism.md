---
out: Isomorphism.html
---

### Isomorphism

Lawvere:

> **Definitions**: An arrow *f: A => B* is called an *isomorphism*, or *invertible arrow*, if there is a map g: B => A, for which *g ∘ f = 1<sub>A</sub>* and *f ∘ g = 1<sub>B</sub>*.
> An arrow *g* related to *f* by satisfying these equations is called an *inverse for f*.
> Two objects *A* and *B* are said to be *isomorphic* if there is at least one isomorphism *f: A => B*.

Unfortunately, Cats doesn't seem to have a datatype to represent isomorphisms, so we have to define one.

```scala mdoc
import cats._, cats.data._, cats.syntax.all._, cats.arrow.Arrow

object Isomorphisms {
  trait Isomorphism[Arrow[_, _], A, B] { self =>
    def to: Arrow[A, B]
    def from: Arrow[B, A]
  }
  type IsoSet[A, B] = Isomorphism[Function1, A, B]
  type <=>[A, B] = IsoSet[A, B]
}
import Isomorphisms._
```

This is now we can define an isomorphism from `Family` to `Relic`.

```scala mdoc
sealed trait Family {}
case object Mother extends Family {}
case object Father extends Family {}
case object Child extends Family {}

sealed trait Relic {}
case object Feather extends Relic {}
case object Stone extends Relic {}
case object Flower extends Relic {}

lazy val isoFamilyRelic = new (Family <=> Relic) {
  val to: Family => Relic = {
    case Mother => Feather
    case Father => Stone
    case Child  => Flower
  }
  val from: Relic => Family = {
    case Feather => Mother
    case Stone   => Father
    case Flower  => Child
  }
}
```

### Equality of arrows

To test this, we could first implement a test for comparing two functions. Two arrows are equal when they have the same three ingredients:

- domain *A*
- codomain *B*
- a rule that assigns *f ∘ a*

We can express this using ScalaCheck as follows:

```scala
scala> import org.scalacheck.{Prop, Arbitrary, Gen}
import org.scalacheck.{Prop, Arbitrary, Gen}

scala> import cats._, cats.data._, cats.syntax.all._
import cats._
import cats.data._
import cats.syntax.all._

scala> def func1EqualsProp[A, B](f: A => B, g: A => B)
         (implicit ev1: Eq[B], ev2: Arbitrary[A]): Prop =
         Prop.forAll { a: A =>
           f(a) === g(a)
         }
func1EqualsProp: [A, B](f: A => B, g: A => B)(implicit ev1: cats.Eq[B], implicit ev2: org.scalacheck.Arbitrary[A])org.scalacheck.Prop

scala> val p1 = func1EqualsProp((_: Int) + 2, 1 + (_: Int))
p1: org.scalacheck.Prop = Prop

scala> p1.check
! Falsified after 0 passed tests.
> ARG_0: 0

scala> val p2 = func1EqualsProp((_: Int) + 2, 2 + (_: Int))
p2: org.scalacheck.Prop = Prop

scala> p2.check
+ OK, passed 100 tests.
```

### Testing for isomorphism

```scala
scala> :paste
implicit val familyEqual = Eq.fromUniversalEquals[Family]
implicit val relicEqual = Eq.fromUniversalEquals[Relic]
implicit val arbFamily: Arbitrary[Family] = Arbitrary {
  Gen.oneOf(Mother, Father, Child)
}
implicit val arbRelic: Arbitrary[Relic] = Arbitrary {
  Gen.oneOf(Feather, Stone, Flower)
}

// Exiting paste mode, now interpreting.

familyEqual: cats.kernel.Eq[Family] = cats.kernel.Eq\$\$anon\$116@99f2e3d
relicEqual: cats.kernel.Eq[Relic] = cats.kernel.Eq\$\$anon\$116@159bd786
arbFamily: org.scalacheck.Arbitrary[Family] = org.scalacheck.ArbitraryLowPriority\$\$anon\$1@799b3915
arbRelic: org.scalacheck.Arbitrary[Relic] = org.scalacheck.ArbitraryLowPriority\$\$anon\$1@36c230c0

scala> func1EqualsProp(isoFamilyRelic.from compose isoFamilyRelic.to, identity[Family] _).check
+ OK, passed 100 tests.

scala> func1EqualsProp(isoFamilyRelic.to compose isoFamilyRelic.from, identity[Relic] _).check
+ OK, passed 100 tests.
```

This shows that the test was successful. That's it for today.
