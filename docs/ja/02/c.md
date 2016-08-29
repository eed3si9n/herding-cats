---
out: checking-laws-with-discipline.html
---

  [FunctorLawsSource]: $catsBaseUrl$/laws/src/main/scala/cats/laws/FunctorLaws.scala
  [kindProjector]: https://github.com/non/kind-projector
  [Discipline]: http://typelevel.org/blog/2013/11/17/discipline.html

### Discipline を用いた法則のチェック

コンパイラはチェックしてくれないけども、Cats は Functor則を[コード][FunctorLawsSource]で表現した
`FunctorLaws` trait を含む:

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

#### REPL からの法則のチェック

これは ScalaCheck のラッパーである [Discipline][Discipline] というライブラリに基いている。
ScalaCheck を使って REPL からテストを実行することができる。

```scala
scala> import cats._, cats.instances.all._
import cats._
import cats.instances.all._

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

`rs.all` は `org.scalacheck.Properties` を返し、これは `check` メソッドを実装する。

#### Discipline + Specs2 を用いた法則のチェック

好みのテストフレームワークに組み込むために自分でケーキを焼かなければならない。
以下は Specs2 用だ:

```scala
package example

import org.specs2.Specification
import org.typelevel.discipline.specs2.Discipline
import cats.instances.AllInstances
import cats.syntax.AllSyntax

trait CatsSpec extends Specification with Discipline with AllInstances with AllSyntax
```

Cats のソースには ScalaTest 用も含まれている。
`Either[Int, Int]` の Functor則をチェックする spec はこのようになる:

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

上の `Either[Int, ?]` という表記は [non/kind-projector][kindProjector] を使っている。
テストを実行すると、以下のように表示される:

```
s> test
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

### 法則を破る

LYAHFGG:

> ここで、`Functor` のインスタンスなのに、ファンクター則を満たしていないような病的な例を考えてみましょう。

法則を破ってみよう:

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

使ってみる:

```console:new
scala> import cats._, cats.syntax.functor._
scala> import example._
scala> (CSome(0, "ho"): COption[String]) map {identity}
```

これは最初の法則を破っている。検知するには `COption[A]` の「任意」の値を暗黙に提供する:

```scala
package example

import cats._
import cats.laws.discipline.{ FunctorTests }
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

  def is = s2"""
  COption[Int] forms a functor                             \$e1
  """

  def e1 = checkAll("COption[Int]", FunctorTests[COption].functor[Int, Int, Int])
}
```

以下のように表示される:

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

期待通りテストは失敗した。
