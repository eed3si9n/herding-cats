---
out: checking-laws-with-discipline.html
---

  [FunctorLawsSource]: $catsBaseUrl$/laws/src/main/scala/cats/laws/FunctorLaws.scala
  [kindProjector]: https://github.com/non/kind-projector
  [Discipline]: http://typelevel.org/blog/2013/11/17/discipline.html

### Checking laws with Discipline

The compiler can't check for the laws, but Cats ships with a `FunctorLaws` trait that describes this in [code][FunctorLawsSource]:

```scala
/**
 * Laws that must be obeyed by any [[Functor]].
 */
trait FunctorLaws[F[_]] extends InvariantLaws[F] {
  implicit override def F: Functor[F]

  def covariantIdentity[A](fa: F[A]): IsEq[F[A]] =
    fa.map(identity) <-> fa

  def covariantComposition[A, B, C](fa: F[A], f: A => B, g: B => C): IsEq[F[C]] =
    fa.map(f).map(g) <-> fa.map(f andThen g)
}
```

#### Checking laws from the REPL

This is based on a library called [Discipline][Discipline], which is a wrapper around ScalaCheck.
We can run these tests from the REPL with ScalaCheck.

```scala
scala> import cats._, cats.std.all._
import cats._
import cats.std.all._

scala> import cats.laws.discipline.FunctorTests
import cats.laws.discipline.FunctorTests

scala> val rs = FunctorTests[Either[Int, ?]].functor[Int, Int, Int]
rs: cats.laws.discipline.FunctorTests[[X_kp1]scala.util.Either[Int,X_kp1]]#RuleSet = cats.laws.discipline.FunctorTests\$\$anon\$2@7993373d

scala> rs.all.check
+ functor.covariant composition: OK, passed 100 tests.
+ functor.covariant identity: OK, passed 100 tests.
+ functor.invariant composition: OK, passed 100 tests.
+ functor.invariant identity: OK, passed 100 tests.
```

`rs.all` returns `org.scalacheck.Properties`, which implements `check` method.

#### Checking laws with Discipline + Specs2

You can also bake your own cake pattern into a test framework of choice.
Here's for specs2:

```scala
package example

import org.specs2.Specification
import org.typelevel.discipline.specs2.Discipline
import cats.std.AllInstances
import cats.syntax.AllSyntax

trait CatsSpec extends Specification with Discipline with AllInstances with AllSyntax
```

Cats' source include one for ScalaTest.

The spec to check the functor law for `Either[Int, Int]` looks like this:

```scala
package example

import cats._
import cats.laws.discipline.FunctorTests

class EitherSpec extends CatsSpec { def is = s2"""
  Either[Int, ?] forms a functor                           \$e1
  """

  def e1 = checkAll("Either[Int, Int]", FunctorTests[Either[Int, ?]].functor[Int, Int, Int])
}
```

The `Either[Int, ?]` is using [non/kind-projector][kindProjector].
Running the test from sbt displays the following output:

```
> test
[info] EitherSpec
[info]   
[info] 
[info] functor laws must hold for Either[Int, Int]
[info] 
[info]  + functor.covariant composition
[info]  + functor.covariant identity
[info]  + functor.invariant composition
[info]  + functor.invariant identity
[info] 
[info]   
[info] Total for specification EitherSpec
[info] Finished in 14 ms
[info] 4 examples, 400 expectations, 0 failure, 0 error
[info] Passed: Total 4, Failed 0, Errors 0, Passed 4
```

#### Breaking the law

LYAHFGG:

> Let's take a look at a pathological example of a type constructor being an instance of the Functor typeclass but not really being a functor, because it doesn't satisfy the laws. 

Let's try breaking the law.

```scala
package example

import cats._

sealed trait COption[+A]
case class CSome[A](counter: Int, a: A) extends COption[A]
case object CNone extends COption[Nothing]

object COption {
  implicit def coptionEq[A]: Eq[COption[A]] = new Eq[COption[A]] {
    def eqv(a1: COption[A], a2: COption[A]): Boolean = a1 == a2
  }
  implicit val coptionFunctor = new Functor[COption] {
    def map[A, B](fa: COption[A])(f: A => B): COption[B] =
      fa match {
        case CNone => CNone
        case CSome(c, a) => CSome(c + 1, f(a))
      }
  }
}
```

Here's how we can use this:

```console:new
scala> import cats._, cats.syntax.functor._
scala> import example._
scala> (CSome(0, "ho"): COption[String]) map {identity}
```

This breaks the first law because the result of the `identity` function is not equal to the input.
To catch this we need to supply an "arbitrary" `COption[A]` implicitly:

```scala
package example

import cats._
import cats.laws.discipline.{ FunctorTests, ArbitraryK }
import org.scalacheck.{ Arbitrary, Gen }

class COptionSpec extends CatsSpec { 
  implicit def coptionArbiterary[A](implicit arbA: Arbitrary[A]): Arbitrary[COption[A]] =
    Arbitrary {
      val arbSome = for {
        i <- implicitly[Arbitrary[Int]].arbitrary
        a <- arbA.arbitrary
      } yield (CSome(i, a): COption[A])
      val arbNone = Gen.const(CNone: COption[Nothing])
      Gen.oneOf(arbSome, arbNone)
    }
  implicit def coptionArbiteraryK: ArbitraryK[COption] =
    new ArbitraryK[COption] { def synthesize[A: Arbitrary]: Arbitrary[COption[A]] = implicitly }
  
  def is = s2"""
  COption[Int] forms a functor                             \$e1
  """

  def e1 = checkAll("COption[Int]", FunctorTests[COption].functor[Int, Int, Int])
}
```

Here's the output:

```
[info] COptionSpec
[info]   
[info] 
[info] functor laws must hold for COption[Int]
[info] 
[info]  x functor.covariant composition
[error]    A counter-example is [CSome(-1,-1), <function1>, <function1>] (after 0 try)
[error]    (CSome(1,1358703086) ?== CSome(0,1358703086)) failed
[info] 
[info]  x functor.covariant identity
[error]    A counter-example is 'CSome(1781926821,82888113)' (after 0 try)
[error]    (CSome(1781926822,82888113) ?== CSome(1781926821,82888113)) failed
[info] 
[info]  x functor.invariant composition
[error]    A counter-example is [CSome(-17878015,0), <function1>, <function1>, <function1>, <function1>] (after 1 try)
[error]    (CSome(-17878013,-1351608161) ?== CSome(-17878014,-1351608161)) failed
[info] 
[info]  x functor.invariant identity
[error]    A counter-example is 'CSome(-1699259031,1)' (after 0 try)
[error]    (CSome(-1699259030,1) ?== CSome(-1699259031,1)) failed
[info] 
[info] 
[info]   
[info] Total for specification COptionSpec
[info] Finished in 13 ms
[info] 4 examples, 4 failures, 0 error
```

The tests failed as expected.
