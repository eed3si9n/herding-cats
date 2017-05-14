---
out: method-injection.html
---

### Method injection (enrich my library)

> If we were to write a function that sums two types using the `Monoid`, we need to call it like this.

```console
scala> def plus[A: Monoid](a: A, b: A): A = implicitly[Monoid[A]].mappend(a, b)
scala> plus(3, 4)
```

We would like to provide an operator. But we don't want to enrich just one type,
but enrich all types that has an instance for `Monoid`.
Let me do this in Cats style using Simulacrum.

```console:new
scala> import simulacrum._
scala> :paste
@typeclass trait Monoid[A] {
  @op("|+|") def mappend(a: A, b: A): A
  def mzero: A
}
object Monoid {
  // "ops" gets generated
  val syntax = ops
  implicit val IntMonoid: Monoid[Int] = new Monoid[Int] {
    def mappend(a: Int, b: Int): Int = a + b
    def mzero: Int = 0
  }
  implicit val StringMonoid: Monoid[String] = new Monoid[String] {
    def mappend(a: String, b: String): String = a + b
    def mzero: String = ""
  }
}
scala> import Monoid.syntax._
scala> 3 |+| 4
scala> "a" |+| "b"
```

We were able to inject `|+|` to both `Int` and `String` with just one definition.

### Operator syntax for the standard datatypes

Using the same technique, Cats _occasionally_ provides method injections for standard library datatypes like `Option` and `Vector`:

```console
scala> import cats._, cats.data._, cats.implicits._
scala> 1.some
scala> 1.some.orEmpty
```

But most operators in Cats are associated with typeclasses.

I hope you could get some feel on where Cats is coming from.
