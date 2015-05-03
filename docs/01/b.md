
  [algebra]: https://github.com/non/algebra
  [Equivalence]: http://en.wikipedia.org/wiki/Equivalence_relation

## Eq

LYAHFGG:

> `Eq` is used for types that support equality testing. The functions its members implement are `==` and `/=`.

Cats equivalent for the `Eq` typeclass is also called `Eq`.
Technically speaking, `cats.Eq` is actually a type alias of `algebra.Eq` from [non/algebra][algebra].
Not sure if it matters, but it's probably a good idea that it's being reused:

```console
scala> import cats._, cats.std.all._, cats.syntax.eq._
scala> 1 === 1
scala> 1 === "foo"
scala> 1 == "foo"
scala> (Some(1): Option[Int]) =!= (Some(2): Option[Int])
```

Instead of the standard `==`, `Eq` enables `===` and `=!=` syntax by declaring `eqv` method. The main difference is that `===` would fail compilation if you tried to compare `Int` and `String`.

In algebra, `neqv` is implemented based on `eqv`.

```scala
/**
 * A type class used to determine equality between 2 instances of the same
 * type. Any 2 instances `x` and `y` are equal if `eqv(x, y)` is `true`.
 * Moreover, `eqv` should form an equivalence relation.
 */
trait Eq[@sp A] extends Any with Serializable { self =>

  /**
   * Returns `true` if `x` and `y` are equivalent, `false` otherwise.
   */
  def eqv(x: A, y: A): Boolean

  /**
   * Returns `false` if `x` and `y` are equivalent, `true` otherwise.
   */
  def neqv(x: A, y: A): Boolean = !eqv(x, y)

  ....
}
```

This is an example of polymorphism. Whatever equality means for the type `A`,
`neqv` is the opposite of it. Does not matter if it's `String`, `Int`, or whatever.
Another way of looking at it is that given `Eq[A]`, `===` is universally the opposite of `=!=`.

I'm a bit concerned that `Eq` seems to be using the term equal and equivalent
interchangably. [Equivalence relationship][Equivalence] could include "having the same birthday"
whereas equality also requires substitution property.
