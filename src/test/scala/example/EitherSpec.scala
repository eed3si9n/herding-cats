package example

import cats._
import cats.laws.discipline.FunctorTests

class EitherSpec extends CatsSpec { def is = s2"""
  Either[Int, ?] forms a functor                           $e1
  """

  def e1 = checkAll("Either[Int, Int]", FunctorTests[Either[Int, ?]].functor[Int, Int, Int])
}
