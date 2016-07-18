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

```console:new
scala> 4 * 1
scala> 1 * 9
scala> List(1, 2, 3) ++ Nil
scala> Nil ++ List(0.5, 2.5)
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
scala> import cats._, cats.std.all._
import cats._
import cats.std.all._

scala> import cats.kernel.laws.GroupLaws
import cats.kernel.laws.GroupLaws

scala> val rs1 = GroupLaws[Int].monoid(Monoid[Int])
rs1: cats.kernel.laws.GroupLaws[Int]#GroupProperties = cats.kernel.laws.GroupLaws\$GroupProperties@17a695f0

scala> rs1.all.check
+ monoid.associativity: OK, passed 100 tests.
+ monoid.combineAll(Nil) == id: OK, passed 100 tests.
+ monoid.combineN(a, 0) == id: OK, passed 100 tests.
+ monoid.combineN(a, 1) == a: OK, passed 100 tests.
+ monoid.combineN(a, 2) == a |+| a: OK, passed 100 tests.
+ monoid.isEmpty: OK, passed 100 tests.
+ monoid.leftIdentity: OK, passed 100 tests.
+ monoid.rightIdentity: OK, passed 100 tests.
+ monoid.serializable: OK, proved property.
```

Here's the spec2 specification of the above:

```scala
package example

import cats._
import algebra.laws.GroupLaws

class IntSpec extends CatsSpec { def is = s2"""
  (Int, +) should
     form a monoid                                         \$e1
  """

  def e1 = checkAll("Int", GroupLaws[Int].monoid(Monoid[Int]))
}
```

#### Value classes

LYAHFGG:

> The *newtype* keyword in Haskell is made exactly for these cases when we want to just take one type and wrap it in something to present it as another type.

Cats does not ship with a tagged-type facility, but Scala now has [value classes][value-classes-overview]. This will remain unboxed under certain conditions, so it should work for simple examples.

```console:new
scala> :paste
class Wrapper(val unwrap: Int) extends AnyVal
```

#### Disjunction and Conjunction

LYAHFGG:

> Another type which can act like a monoid in two distinct but equally valid ways is `Bool`. The first way is to have the or function `||` act as the binary function along with `False` as the identity value.
> ...
> The other way for `Bool` to be an instance of `Monoid` is to kind of do the opposite: have `&&` be the binary function and then make `True` the identity value.

Cats does not provide this, but we can implement it ourselves.

```console:new
scala> import cats._, cats.std.all._, cats.syntax.semigroup._
scala> :paste
class Disjunction(val unwrap: Boolean) extends AnyVal
object Disjunction {
  @inline def apply(b: Boolean): Disjunction = new Disjunction(b)
  implicit val disjunctionMonoid: Monoid[Disjunction] = new Monoid[Disjunction] {
    def combine(a1: Disjunction, a2: Disjunction): Disjunction =
      Disjunction(a1.unwrap && a2.unwrap)
    def empty: Disjunction = Disjunction(true)
  }
  implicit val disjunctionEq: Eq[Disjunction] = new Eq[Disjunction] {
    def eqv(a1: Disjunction, a2: Disjunction): Boolean =
      a1.unwrap == a2.unwrap
  }
}
scala> val x1 = Disjunction(true) |+| Disjunction(false)
scala> x1.unwrap
scala> val x2 = Monoid[Disjunction].empty |+| Disjunction(true)
scala> x2.unwrap
```

Here's conjunction:

```console
scala> :paste
class Conjunction(val unwrap: Boolean) extends AnyVal
object Conjunction {
  @inline def apply(b: Boolean): Conjunction = new Conjunction(b)
  implicit val conjunctionMonoid: Monoid[Conjunction] = new Monoid[Conjunction] {
    def combine(a1: Conjunction, a2: Conjunction): Conjunction =
      Conjunction(a1.unwrap || a2.unwrap)
    def empty: Conjunction = Conjunction(false)    
  }
  implicit val conjunctionEq: Eq[Conjunction] = new Eq[Conjunction] {
    def eqv(a1: Conjunction, a2: Conjunction): Boolean =
      a1.unwrap == a2.unwrap
  }
}
scala> val x3 = Conjunction(true) |+| Conjunction(false)
scala> x3.unwrap
scala> val x4 = Monoid[Conjunction].empty |+| Conjunction(true)
scala> x4.unwrap
```

We should check if our custom new types satisfy the the monoid laws.

```scala
scala> import algebra.laws.GroupLaws
import algebra.laws.GroupLaws

scala> import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.{Arbitrary, Gen}

scala> implicit def arbDisjunction(implicit ev: Arbitrary[Boolean]): Arbitrary[Disjunction] =
         Arbitrary { ev.arbitrary map { Disjunction(_) } }
arbDisjunction: (implicit ev: org.scalacheck.Arbitrary[Boolean])org.scalacheck.Arbitrary[Disjunction]

scala> val rs1 = GroupLaws[Disjunction].monoid
rs1: algebra.laws.GroupLaws[Disjunction]#GroupProperties = algebra.laws.GroupLaws\$GroupProperties@77663edf

scala> rs1.all.check
+ monoid.associativity: OK, passed 100 tests.
+ monoid.combineAll(Nil) == id: OK, passed 100 tests.
+ monoid.combineN(a, 0) == id: OK, passed 100 tests.
+ monoid.combineN(a, 1) == a: OK, passed 100 tests.
+ monoid.combineN(a, 2) == a |+| a: OK, passed 100 tests.
+ monoid.isEmpty: OK, passed 100 tests.
+ monoid.leftIdentity: OK, passed 100 tests.
+ monoid.rightIdentity: OK, passed 100 tests.
! monoid.serializable: Falsified after 0 passed tests.
```

The test failed because our monoid is not `Serializable`.
I'm confused as to why the monoid law is checking for `Serializable`.
[non/algebra#13][algebra13] says it's convenient for Spark. I feel like this should be a separate thing.

> **Update**: It turns out, the failure is due to the fact I'm using REPL to define the typeclass instances!

```scala
scala> implicit def arbConjunction(implicit ev: Arbitrary[Boolean]): Arbitrary[Conjunction] =
         Arbitrary { ev.arbitrary map { Conjunction(_) } }
arbConjunction: (implicit ev: org.scalacheck.Arbitrary[Boolean])org.scalacheck.Arbitrary[Conjunction]

scala> val rs2 = GroupLaws[Conjunction].monoid
rs2: algebra.laws.GroupLaws[Conjunction]#GroupProperties = algebra.laws.GroupLaws\$GroupProperties@15f279d

scala> rs2.all.check
+ monoid.associativity: OK, passed 100 tests.
+ monoid.combineAll(Nil) == id: OK, passed 100 tests.
+ monoid.combineN(a, 0) == id: OK, passed 100 tests.
+ monoid.combineN(a, 1) == a: OK, passed 100 tests.
+ monoid.combineN(a, 2) == a |+| a: OK, passed 100 tests.
+ monoid.isEmpty: OK, passed 100 tests.
+ monoid.leftIdentity: OK, passed 100 tests.
+ monoid.rightIdentity: OK, passed 100 tests.
! monoid.serializable: Falsified after 0 passed tests.
```

Apart from the serializable rule, `Conjunction` looks ok.

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

```console
scala> :paste
object Catnip {
  implicit class IdOp[A](val a: A) extends AnyVal {
    def some: Option[A] = Some(a)
  }
  def none[A]: Option[A] = None
}
import Catnip._
scala> none[String] |+| "andy".some
scala> 1.some |+| none[Int]
```

It works.

LYAHFGG:

> But if we don't know if the contents are monoids, we can't use `mappend` between them, so what are we to do? Well, one thing we can do is to just discard the second value and keep the first one. For this, the `First a` type exists.

Haskell is using `newtype` to implement `First` type constructor. Since we can't prevent allocation for generic value class, we can just make a normal case class.

```console
scala> :paste
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
scala> First('a'.some) |+| First('b'.some)
scala> First(none[Char]) |+| First('b'.some)
```

Let's check the laws:

```scala
scala> implicit def arbFirst[A: Eq](implicit ev: Arbitrary[Option[A]]): Arbitrary[First[A]] =
         Arbitrary { ev.arbitrary map { First(_) } }
arbFirst: [A](implicit evidence\$1: cats.Eq[A], implicit ev: org.scalacheck.Arbitrary[Option[A]])org.scalacheck.Arbitrary[First[A]]

scala> val rs3 = GroupLaws[First[Int]].monoid
rs3: algebra.laws.GroupLaws[First[Int]]#GroupProperties = algebra.laws.GroupLaws\$GroupProperties@44736530

scala> rs3.all.check
+ monoid.associativity: OK, passed 100 tests.
+ monoid.combineAll(Nil) == id: OK, passed 100 tests.
+ monoid.combineN(a, 0) == id: OK, passed 100 tests.
+ monoid.combineN(a, 1) == a: OK, passed 100 tests.
+ monoid.combineN(a, 2) == a |+| a: OK, passed 100 tests.
+ monoid.isEmpty: OK, passed 100 tests.
+ monoid.leftIdentity: OK, passed 100 tests.
+ monoid.rightIdentity: OK, passed 100 tests.
! monoid.serializable: Falsified after 0 passed tests.
```

It thinks `First` is not serializable either.

LYAHFGG:

> If we want a monoid on `Maybe a` such that the second parameter is kept if both parameters of `mappend` are `Just` values, `Data.Monoid` provides a the `Last a` type.

```console
scala> :paste
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
scala> Last('a'.some) |+| Last('b'.some)
scala> Last('a'.some) |+| Last(none[Char])
```

More law checking:

```scala
scala> implicit def arbLast[A: Eq](implicit ev: Arbitrary[Option[A]]): Arbitrary[Last[A]] =
         Arbitrary { ev.arbitrary map { Last(_) } }
arbLast: [A](implicit evidence\$1: cats.Eq[A], implicit ev: org.scalacheck.Arbitrary[Option[A]])org.scalacheck.Arbitrary[Last[A]]

scala> val rs4 = GroupLaws[Last[Int]].monoid
rs4: algebra.laws.GroupLaws[Last[Int]]#GroupProperties = algebra.laws.GroupLaws\$GroupProperties@121fd6d9

scala> rs4.all.check
+ monoid.associativity: OK, passed 100 tests.
+ monoid.combineAll(Nil) == id: OK, passed 100 tests.
+ monoid.combineN(a, 0) == id: OK, passed 100 tests.
+ monoid.combineN(a, 1) == a: OK, passed 100 tests.
+ monoid.combineN(a, 2) == a |+| a: OK, passed 100 tests.
+ monoid.isEmpty: OK, passed 100 tests.
+ monoid.leftIdentity: OK, passed 100 tests.
+ monoid.rightIdentity: OK, passed 100 tests.
! monoid.serializable: Falsified after 0 passed tests.
```

I think we got a pretty good feel for monoids.
