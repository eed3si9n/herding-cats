package example

import cats._
import cats.kernel.laws.GroupLaws

class IntSpec extends CatsSpec { def is = s2"""
  (Int, +) should
     form a monoid                                         $e1
  """

  def e1 = checkAll("Int", GroupLaws[Int].monoid(Monoid[Int]))
}
