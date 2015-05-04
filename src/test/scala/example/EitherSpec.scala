package example

import cats._
import cats.laws.discipline.FunctorTests

class EitherSpec extends CatsSpec { def is = s2"""
  ${checkAll("Either[Int, Int]", FunctorTests[Either[Int, ?]].functor[Int, Int, Int])}
  """
}
