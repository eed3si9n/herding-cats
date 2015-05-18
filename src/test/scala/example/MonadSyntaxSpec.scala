package example

import specs2._
// import cats._, cats.std.all._
import MonadSyntax._

class MonadSyntaxSpec extends CatsSpec { def is = s2"""
  actM { FA.next } should
    expand into flatMap                                   $e1

  actM { val x = FA.next; x } should
    expand into flatMap                                   $e2

  actM { val x = FA.next; val y = FB.next; x + y } should
    expand into flatMaps                                  $e3
  """

  def e1 = (actM[List, Int] { List(1, 2, 3).next }) must_== List(1, 2, 3)
  def e2 = (actM[List, Int] { 
    val x = List(1, 2, 3).next
    x + 1
  }) must_== List(2, 3, 4)
  def e3 = (actM[List, Int] { 
    val x = List(1, 2, 3).next
    val y = List(1, 2).next
    x + y
  }) must_== List(2, 3, 3, 4, 4, 5)
}
