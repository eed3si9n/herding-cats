---
out: method-injection.html
---

### Method injection (enrich my library)

> If we were to write a function that sums two types using the `Monoid`, we need to call it like this.

```scala mdoc:invisible
trait Monoid[A] {
  def mappend(a1: A, a2: A): A
  def mzero: A
}
object Monoid {
  implicit val IntMonoid: Monoid[Int] = new Monoid[Int] {
    def mappend(a: Int, b: Int): Int = a + b
    def mzero: Int = 0
  }
  implicit val StringMonoid: Monoid[String] = new Monoid[String] {
    def mappend(a: String, b: String): String = a + b
    def mzero: String = ""
  }
}
```

```scala mdoc
def plus[A: Monoid](a: A, b: A): A = implicitly[Monoid[A]].mappend(a, b)
plus(3, 4)
```

We would like to provide an operator. But we don't want to enrich just one type,
but enrich all types that has an instance for `Monoid`.

```scala mdoc:reset
trait Monoid[A] {
  def mappend(a: A, b: A): A
  def mzero: A
}
object Monoid {
  object syntax extends MonoidSyntax

  implicit val IntMonoid: Monoid[Int] = new Monoid[Int] {
    def mappend(a: Int, b: Int): Int = a + b
    def mzero: Int = 0
  }
  implicit val StringMonoid: Monoid[String] = new Monoid[String] {
    def mappend(a: String, b: String): String = a + b
    def mzero: String = ""
  }
}
trait MonoidSyntax {
  implicit final def syntaxMonoid[A: Monoid](a: A): MonoidOps[A] =
    new MonoidOps[A](a)
}
final class MonoidOps[A: Monoid](lhs: A) {
  def |+|(rhs: A): A = implicitly[Monoid[A]].mappend(lhs, rhs)
}

import Monoid.syntax._
3 |+| 4

"a" |+| "b"
```

We were able to inject `|+|` to both `Int` and `String` with just one definition.

### Operator syntax for the standard datatypes

Using the same technique, Cats _occasionally_ provides method injections for standard library datatypes like `Option` and `Vector`:

```scala mdoc:reset
import cats._, cats.data._, cats.implicits._

1.some

1.some.orEmpty
```

But most operators in Cats are associated with typeclasses.

I hope you could get some feel on where Cats is coming from.
