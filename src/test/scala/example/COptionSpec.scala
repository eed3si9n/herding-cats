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
  COption[Int] forms a functor                             $e1
  """

  def e1 = checkAll("COption[Int]", FunctorTests[COption].functor[Int, Int, Int])
}
