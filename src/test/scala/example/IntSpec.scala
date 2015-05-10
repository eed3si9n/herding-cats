package example

import cats._
import algebra.laws.GroupLaws

class IntSpec extends CatsSpec { def is = s2"""
  (Int, +) should
     form a monoid                                         $e1
  
  (Int, *) should
     from a monoid                                         $e2
  """

  def e1 = checkAll("Int", GroupLaws[Int].monoid(Monoid.additive[Int]))
  def e2 = checkAll("Int", GroupLaws[Int].monoid(Monoid.multiplicative[Int]))
}
