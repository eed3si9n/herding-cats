package example

import cats._
import cats.kernel.laws.discipline.MonoidTests

class IntTest extends munit.DisciplineSuite {
  checkAll("Int", MonoidTests[Int].monoid)
}
