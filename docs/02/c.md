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
scala> import cats._, cats.syntax.all._
import cats._
import cats.syntax.all._

scala> import cats.laws.discipline.FunctorTests
import cats.laws.discipline.FunctorTests

scala> val rs = FunctorTests[Either[Int, *]].functor[Int, Int, Int]
val rs: cats.laws.discipline.FunctorTests[[?\$0\$]scala.util.Either[Int,?\$0\$]]#RuleSet = org.typelevel.discipline.Laws\$DefaultRuleSet@2b1a2a1d

scala> import org.scalacheck.Test.Parameters
import org.scalacheck.Test.Parameters

scala> rs.all.check(Parameters.default)
+ functor.covariant composition: OK, passed 100 tests.
+ functor.covariant identity: OK, passed 100 tests.
+ functor.invariant composition: OK, passed 100 tests.
+ functor.invariant identity: OK, passed 100 tests.
```

`rs.all` returns `org.scalacheck.Properties`, which implements `check` method.

#### Checking laws with Discipline + MUnit

In addition to ScalaCheck, you can call these tests from ScalaTest, Specs2, or MUnit. An MUnit test to check the functor law for `Either[Int, Int]` looks like this:

```scala
package example

import cats._
import cats.laws.discipline.FunctorTests

class EitherTest extends munit.DisciplineSuite {
  checkAll("Either[Int, Int]", FunctorTests[Either[Int, *]].functor[Int, Int, Int])
}
```

The `Either[Int, *]` is using [non/kind-projector][kindProjector].
Running the test from sbt displays the following output:

```
sbt:herding-cats> Test/testOnly example.EitherTest
example.EitherTest:
  + Either[Int, Int]: functor.covariant composition 0.096s
  + Either[Int, Int]: functor.covariant identity 0.017s
  + Either[Int, Int]: functor.invariant composition 0.041s
  + Either[Int, Int]: functor.invariant identity 0.011s
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

```scala mdoc
import cats._, cats.syntax.all._
import example._

(CSome(0, "hi"): COption[String]) map {identity}
```

This breaks the first law because the result of the `identity` function is not equal to the input.
To catch this we need to supply an "arbitrary" `COption[A]` implicitly:

```scala
package example

import cats._
import cats.laws.discipline.{ FunctorTests }
import org.scalacheck.{ Arbitrary, Gen }

class COptionTest extends munit.DisciplineSuite {
  checkAll("COption[Int]", FunctorTests[COption].functor[Int, Int, Int])

  implicit def coptionArbiterary[A](implicit arbA: Arbitrary[A]): Arbitrary[COption[A]] =
    Arbitrary {
      val arbSome = for {
        i <- implicitly[Arbitrary[Int]].arbitrary
        a <- arbA.arbitrary
      } yield (CSome(i, a): COption[A])
      val arbNone = Gen.const(CNone: COption[Nothing])
      Gen.oneOf(arbSome, arbNone)
    }
}
```

Here's the output:

```
example.COptionTest:
failing seed for functor.covariant composition is 43LA3KHokN6KnEAzbkXi6IijQU91ran9-zsO2JeIyIP=
==> X example.COptionTest.COption[Int]: functor.covariant composition  0.058s munit.FailException: /Users/eed3si9n/work/herding-cats/src/test/scala/example/COptionTest.scala:8
7:class COptionTest extends munit.DisciplineSuite {
8:  checkAll("COption[Int]", FunctorTests[COption].functor[Int, Int, Int])
9:

Failing seed: 43LA3KHokN6KnEAzbkXi6IijQU91ran9-zsO2JeIyIP=
You can reproduce this failure by adding the following override to your suite:

  override val scalaCheckInitialSeed = "43LA3KHokN6KnEAzbkXi6IijQU91ran9-zsO2JeIyIP="

Falsified after 0 passed tests.
> Labels of failing property:
Expected: CSome(2,-1)
Received: CSome(3,-1)
> ARG_0: CSome(1,0)
> ARG_1: org.scalacheck.GenArities\$\$Lambda\$36505/1702985322@62d7d97c
> ARG_2: org.scalacheck.GenArities\$\$Lambda\$36505/1702985322@18bdc9d7
    ....
failing seed for functor.covariant identity is a4C-NCiCQEn0lU6F_TXdy5-IZ-XhMYDrC0vipJ3O_tG=
==> X example.COptionTest.COption[Int]: functor.covariant identity  0.003s munit.FailException: /Users/eed3si9n/work/herding-cats/src/test/scala/example/COptionTest.scala:8
7:class COptionTest extends munit.DisciplineSuite {
8:  checkAll("COption[Int]", FunctorTests[COption].functor[Int, Int, Int])
9:

Failing seed: RhjRyflmRS-5CYveyf0uAFHuX6mWNm-Z98FVIs2aIVC=
You can reproduce this failure by adding the following override to your suite:

  override val scalaCheckInitialSeed = "RhjRyflmRS-5CYveyf0uAFHuX6mWNm-Z98FVIs2aIVC="

Falsified after 1 passed tests.
> Labels of failing property:
Expected: CSome(-1486306630,-1498342842)
Received: CSome(-1486306629,-1498342842)
> ARG_0: CSome(-1486306630,-1498342842)
    ....
failing seed for functor.invariant composition is 9uQIZNNK_uZksfWg5pRb0VJUIgUtkv9vG9ckZ4UlRwD=
==> X example.COptionTest.COption[Int]: functor.invariant composition  0.005s munit.FailException: /Users/eed3si9n/work/herding-cats/src/test/scala/example/COptionTest.scala:8
7:class COptionTest extends munit.DisciplineSuite {
8:  checkAll("COption[Int]", FunctorTests[COption].functor[Int, Int, Int])
9:

Failing seed: 9uQIZNNK_uZksfWg5pRb0VJUIgUtkv9vG9ckZ4UlRwD=
You can reproduce this failure by adding the following override to your suite:

  override val scalaCheckInitialSeed = "9uQIZNNK_uZksfWg5pRb0VJUIgUtkv9vG9ckZ4UlRwD="

Falsified after 0 passed tests.
> Labels of failing property:
Expected: CSome(1,2147483647)
Received: CSome(2,2147483647)
> ARG_0: CSome(0,1095768235)
> ARG_1: org.scalacheck.GenArities\$\$Lambda\$36505/1702985322@431263ab
> ARG_2: org.scalacheck.GenArities\$\$Lambda\$36505/1702985322@5afe6566
> ARG_3: org.scalacheck.GenArities\$\$Lambda\$36505/1702985322@ca0deda
> ARG_4: org.scalacheck.GenArities\$\$Lambda\$36505/1702985322@1d7dde37
    ....
failing seed for functor.invariant identity is RcktTeI0rbpoUfuI3FHdvZtVGXGMoAjB6JkNBcTNTVK=
==> X example.COptionTest.COption[Int]: functor.invariant identity  0.002s munit.FailException: /Users/eed3si9n/work/herding-cats/src/test/scala/example/COptionTest.scala:8
7:class COptionTest extends munit.DisciplineSuite {
8:  checkAll("COption[Int]", FunctorTests[COption].functor[Int, Int, Int])
9:

Failing seed: RcktTeI0rbpoUfuI3FHdvZtVGXGMoAjB6JkNBcTNTVK=
You can reproduce this failure by adding the following override to your suite:

  override val scalaCheckInitialSeed = "RcktTeI0rbpoUfuI3FHdvZtVGXGMoAjB6JkNBcTNTVK="

Falsified after 0 passed tests.
> Labels of failing property:
Expected: CSome(2147483647,1054398067)
Received: CSome(-2147483648,1054398067)
> ARG_0: CSome(2147483647,1054398067)
    ....
[error] Failed: Total 4, Failed 4, Errors 0, Passed 0
[error] Failed tests:
[error]   example.COptionTest
[error] (Test / testOnly) sbt.TestsFailedException: Tests unsuccessful
```

The tests failed as expected.
