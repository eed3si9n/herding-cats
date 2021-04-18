package example

import cats._
import cats.laws.discipline.{ FunctorTests }
import org.scalacheck.{ Arbitrary, Gen }

class COptionTest extends munit.DisciplineSuite {
  // checkAll("COption[Int]", FunctorTests[COption].functor[Int, Int, Int])

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
