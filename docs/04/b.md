---
out: Monoid.html
---

  [value-classes-overview]: http://docs.scala-lang.org/overviews/core/value-classes.html
  [algebra13]: https://github.com/non/algebra/issues/13

### Monoid

LYAHFGG:

> It seems that both `*` together with `1` and `++` along with `[]` share some common properties:
>
> - The function takes two parameters.
> - The parameters and the returned value have the same type.
> - There exists such a value that doesn't change other values when used with the binary function.

Let's check it out in Scala:

```scala mdoc
4 * 1

1 * 9

List(1, 2, 3) ++ Nil

Nil ++ List(0.5, 2.5)
```

Looks right.

#### Monoid typeclass

Here's the typeclass contract of `algebra.Monoid`:

```scala
/**
 * A monoid is a semigroup with an identity. A monoid is a specialization of a
 * semigroup, so its operation must be associative. Additionally,
 * `combine(x, empty) == combine(empty, x) == x`. For example, if we have `Monoid[String]`,
 * with `combine` as string concatenation, then `empty = ""`.
 */
trait Monoid[@sp(Int, Long, Float, Double) A] extends Any with Semigroup[A] {

  /**
   * Return the identity element for this monoid.
   */
  def empty: A

  ...
}
```

#### Monoid laws

In addition to the semigroup law, monoid must satify two more laws:

- associativity `(x |+| y) |+| z = x |+| (y |+| z)`
- left identity `Monoid[A].empty |+| x = x`
- right identity `x |+| Monoid[A].empty = x`

Here's how we can check monoid laws from the REPL:

```scala
scala> import cats._, cats.syntax.all._
import cats._
import cats.syntax.all._

scala> import cats.kernel.laws.discipline.MonoidTests
import cats.kernel.laws.discipline.MonoidTests

scala> import org.scalacheck.Test.Parameters
import org.scalacheck.Test.Parameters

scala> val rs1 = MonoidTests[Int].monoid
val rs1: cats.kernel.laws.discipline.MonoidTests[Int]#RuleSet = org.typelevel.discipline.Laws\$DefaultRuleSet@108684fb

scala> rs1.all.check(Parameters.default)
+ monoid.associative: OK, passed 100 tests.
+ monoid.collect0: OK, passed 100 tests.
+ monoid.combine all: OK, passed 100 tests.
+ monoid.combineAllOption: OK, passed 100 tests.
+ monoid.intercalateCombineAllOption: OK, passed 100 tests.
+ monoid.intercalateIntercalates: OK, passed 100 tests.
+ monoid.intercalateRepeat1: OK, passed 100 tests.
+ monoid.intercalateRepeat2: OK, passed 100 tests.
+ monoid.is id: OK, passed 100 tests.
+ monoid.left identity: OK, passed 100 tests.
+ monoid.repeat0: OK, passed 100 tests.
+ monoid.repeat1: OK, passed 100 tests.
+ monoid.repeat2: OK, passed 100 tests.
+ monoid.reverseCombineAllOption: OK, passed 100 tests.
+ monoid.reverseRepeat1: OK, passed 100 tests.
+ monoid.reverseRepeat2: OK, passed 100 tests.
+ monoid.reverseReverses: OK, passed 100 tests.
+ monoid.right identity: OK, passed 100 tests.
```

Here's the MUnit test of the above:

```scala
package example

import cats._
import cats.kernel.laws.discipline.MonoidTests

class IntTest extends munit.DisciplineSuite {
  checkAll("Int", MonoidTests[Int].monoid)
}
```

#### Value classes

LYAHFGG:

> The *newtype* keyword in Haskell is made exactly for these cases when we want to just take one type and wrap it in something to present it as another type.

Cats does not ship with a tagged-type facility, but Scala now has [value classes][value-classes-overview]. This will remain unboxed under certain conditions, so it should work for simple examples.

```scala
class Wrapper(val unwrap: Int) extends AnyVal
```

#### Disjunction and Conjunction

LYAHFGG:

> Another type which can act like a monoid in two distinct but equally valid ways is `Bool`. The first way is to have the or function `||` act as the binary function along with `False` as the identity value.
> ...
> The other way for `Bool` to be an instance of `Monoid` is to kind of do the opposite: have `&&` be the binary function and then make `True` the identity value.

Cats does not provide this, but we can implement it ourselves.

```scala mdoc
import cats._, cats.syntax.all._

// `class Disjunction(val unwrap: Boolean) extends AnyVal` doesn't work on mdoc
class Disjunction(val unwrap: Boolean)

object Disjunction {
  @inline def apply(b: Boolean): Disjunction = new Disjunction(b)
  implicit val disjunctionMonoid: Monoid[Disjunction] = new Monoid[Disjunction] {
    def combine(a1: Disjunction, a2: Disjunction): Disjunction =
      Disjunction(a1.unwrap || a2.unwrap)
    def empty: Disjunction = Disjunction(false)
  }
  implicit val disjunctionEq: Eq[Disjunction] = new Eq[Disjunction] {
    def eqv(a1: Disjunction, a2: Disjunction): Boolean =
      a1.unwrap == a2.unwrap
  }
}

val x1 = Disjunction(true) |+| Disjunction(false)

x1.unwrap

val x2 = Monoid[Disjunction].empty |+| Disjunction(true)

x2.unwrap
```

Here's conjunction:

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

val x3 = Conjunction(true) |+| Conjunction(false)

x3.unwrap

val x4 = Monoid[Conjunction].empty |+| Conjunction(true)

x4.unwrap
```

We should check if our custom new types satisfy the the monoid laws.

```scala
scala> import cats._, cats.syntax.all._
import cats._
import cats.syntax.all._

scala> import cats.kernel.laws.discipline.MonoidTests
import cats.kernel.laws.discipline.MonoidTests

scala> import org.scalacheck.Test.Parameters
import org.scalacheck.Test.Parameters

scala> import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.{Arbitrary, Gen}

scala> implicit def arbDisjunction(implicit ev: Arbitrary[Boolean]): Arbitrary[Disjunction] =
         Arbitrary { ev.arbitrary map { Disjunction(_) } }
def arbDisjunction(implicit ev: org.scalacheck.Arbitrary[Boolean]): org.scalacheck.Arbitrary[Disjunction]

scala> val rs1 = MonoidTests[Disjunction].monoid
val rs1: cats.kernel.laws.discipline.MonoidTests[Disjunction]#RuleSet = org.typelevel.discipline.Laws\$DefaultRuleSet@464d134

scala> rs1.all.check(Parameters.default)
+ monoid.associative: OK, passed 100 tests.
+ monoid.collect0: OK, passed 100 tests.
+ monoid.combine all: OK, passed 100 tests.
+ monoid.combineAllOption: OK, passed 100 tests.
....
```

`Disjunction` looks ok.

```scala
scala> implicit def arbConjunction(implicit ev: Arbitrary[Boolean]): Arbitrary[Conjunction] =
         Arbitrary { ev.arbitrary map { Conjunction(_) } }
def arbConjunction(implicit ev: org.scalacheck.Arbitrary[Boolean]): org.scalacheck.Arbitrary[Conjunction]

scala> val rs2 = MonoidTests[Conjunction].monoid
val rs2: cats.kernel.laws.discipline.MonoidTests[Conjunction]#RuleSet = org.typelevel.discipline.Laws\$DefaultRuleSet@71a4f643

scala> rs2.all.check(Parameters.default)
+ monoid.associative: OK, passed 100 tests.
+ monoid.collect0: OK, passed 100 tests.
+ monoid.combine all: OK, passed 100 tests.
+ monoid.combineAllOption: OK, passed 100 tests.
....
```

`Conjunction` looks ok too.

#### Option as Monoids

LYAHFGG:

> One way is to treat `Maybe a` as a monoid only if its type parameter a is a monoid as well and then implement mappend in such a way that it uses the mappend operation of the values that are wrapped with `Just`.

Let's see if this is how Cats does it.

```scala
  implicit def optionMonoid[A](implicit ev: Semigroup[A]): Monoid[Option[A]] =
    new Monoid[Option[A]] {
      def empty: Option[A] = None
      def combine(x: Option[A], y: Option[A]): Option[A] =
        x match {
          case None => y
          case Some(xx) => y match {
            case None => x
            case Some(yy) => Some(ev.combine(xx,yy))
          }
        }
    }
```

If we replace `mappend` with the equivalent `combine`, the rest is just pattern matching.
Let's try using it.

```scala mdoc
none[String] |+| "andy".some

1.some |+| none[Int]
```

It works.

LYAHFGG:

> But if we don't know if the contents are monoids, we can't use `mappend` between them, so what are we to do? Well, one thing we can do is to just discard the second value and keep the first one. For this, the `First a` type exists.

Haskell is using `newtype` to implement `First` type constructor. Since we can't prevent allocation for generic value class, we can just make a normal case class.

```scala mdoc
case class First[A: Eq](val unwrap: Option[A])

object First {
  implicit def firstMonoid[A: Eq]: Monoid[First[A]] = new Monoid[First[A]] {
    def combine(a1: First[A], a2: First[A]): First[A] =
      First((a1.unwrap, a2.unwrap) match {
        case (Some(x), _) => Some(x)
        case (None, y)    => y
      })
    def empty: First[A] = First(None: Option[A])
  }
  implicit def firstEq[A: Eq]: Eq[First[A]] = new Eq[First[A]] {
    def eqv(a1: First[A], a2: First[A]): Boolean =
      Eq[Option[A]].eqv(a1.unwrap, a2.unwrap)
  }
}

First('a'.some) |+| First('b'.some)

First(none[Char]) |+| First('b'.some)
```

Let's check the laws:

```scala
scala> implicit def arbFirst[A: Eq](implicit ev: Arbitrary[Option[A]]): Arbitrary[First[A]] =
         Arbitrary { ev.arbitrary map { First(_) } }
def arbFirst[A](implicit evidence\$1: cats.Eq[A], ev: org.scalacheck.Arbitrary[Option[A]]): org.scalacheck.Arbitrary[First[A]]

scala> val rs3 = MonoidTests[First[Int]].monoid
val rs3: cats.kernel.laws.discipline.MonoidTests[First[Int]]#RuleSet = org.typelevel.discipline.Laws\$DefaultRuleSet@17d3711d

scala> rs3.all.check(Parameters.default)
+ monoid.associative: OK, passed 100 tests.
+ monoid.collect0: OK, passed 100 tests.
+ monoid.combine all: OK, passed 100 tests.
+ monoid.combineAllOption: OK, passed 100 tests.
....
```

It thinks `First` is not serializable either.

LYAHFGG:

> If we want a monoid on `Maybe a` such that the second parameter is kept if both parameters of `mappend` are `Just` values, `Data.Monoid` provides a the `Last a` type.

```scala mdoc
case class Last[A: Eq](val unwrap: Option[A])

object Last {
  implicit def lastMonoid[A: Eq]: Monoid[Last[A]] = new Monoid[Last[A]] {
    def combine(a1: Last[A], a2: Last[A]): Last[A] =
      Last((a1.unwrap, a2.unwrap) match {
        case (_, Some(y)) => Some(y)
        case (x, None)    => x
      })
    def empty: Last[A] = Last(None: Option[A])
  }
  implicit def lastEq[A: Eq]: Eq[Last[A]] = new Eq[Last[A]] {
    def eqv(a1: Last[A], a2: Last[A]): Boolean =
      Eq[Option[A]].eqv(a1.unwrap, a2.unwrap)
  }
}

Last('a'.some) |+| Last('b'.some)

Last('a'.some) |+| Last(none[Char])
```

More law checking:

```scala
scala> implicit def arbLast[A: Eq](implicit ev: Arbitrary[Option[A]]): Arbitrary[Last[A]] =
         Arbitrary { ev.arbitrary map { Last(_) } }
def arbLast[A](implicit evidence\$1: cats.Eq[A], ev: org.scalacheck.Arbitrary[Option[A]]): org.scalacheck.Arbitrary[Last[A]]

scala> val rs4 = MonoidTests[Last[Int]].monoid
val rs4: cats.kernel.laws.discipline.MonoidTests[Last[Int]]#RuleSet = org.typelevel.discipline.Laws\$DefaultRuleSet@7b28ea53

scala> rs4.all.check(Parameters.default)
+ monoid.associative: OK, passed 100 tests.
+ monoid.collect0: OK, passed 100 tests.
+ monoid.combine all: OK, passed 100 tests.
+ monoid.combineAllOption: OK, passed 100 tests.
....
```

I think we got a pretty good feel for monoids.
