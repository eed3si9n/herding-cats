package example

import cats._
import cats.laws.discipline.FunctorTests

class EitherTest extends munit.DisciplineSuite {
  checkAll("Either[Int, Int]", FunctorTests[Either[Int, *]].functor[Int, Int, Int])
}
