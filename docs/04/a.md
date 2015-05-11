---
out: Semigroup.html
---

  [clwd]: checking-laws-with-discipline.html
  [fafm]: http://learnyouahaskell.com/functors-applicative-functors-and-monoids

### `Semigroup`

If you have the book _Learn You a Haskell for Great Good_ you get to start a new chapter: "Monoids." For the website, it's still [Functors, Applicative Functors and Monoids][fafm].

First, it seems like Cats is missing `newtype`/tagged type facility.
We'll implement our own later.

Haskell's `Monoid` is split into `Semigroup` and `Monoid` in Cats. They are also type aliases of `algebra.Semigroup` and `algebra.Monoid`. As with `Apply` and `Applicative`, `Semigroup` is a weaker version of `Monoid`. If you can solve the same problem, weaker is cooler because you're making fewer assumptions.

LYAHFGG:

> It doesn't matter if we do `(3 * 4) * 5` or `3 * (4 * 5)`. Either way, the result is `60`. The same goes for `++`.
> ...
>
> We call this property *associativity*. `*` is associative, and so is `++`, but `-`, for example, is not.

Let's check this:

```console:new
scala> import cats._, cats.std.all._, cats.syntax.eq._
scala> assert { (3 * 2) * (8 * 5) === 3 * (2 * (8 * 5)) }
scala> assert { List("la") ++ (List("di") ++ List("da")) === (List("la") ++ List("di")) ++ List("da") }
```

No error means, they are equal.

#### The `Semigroup` typeclass

Here's the typeclass contract for `algebra.Semigroup`.

```scala
/**
 * A semigroup is any set `A` with an associative operation (`combine`).
 */
trait Semigroup[@sp(Int, Long, Float, Double) A] extends Any with Serializable {

  /**
   * Associative operation taking which combines two values.
   */
  def combine(x: A, y: A): A

  ....
}
```

This enables `combine` operator and its symbolic alias `|+|`. Let's try using this.

```console
scala> import cats._, cats.std.all._, cats.syntax.semigroup._
scala> List(1, 2, 3) |+| List(4, 5, 6)
scala> "one" |+| "two"
```

#### The `Semigroup` Laws

Associativity is the only law for `Semigroup`.

- associativity `(x |+| y) |+| z = x |+| (y |+| z)`

Here's how we can check the Semigroup laws from the REPL.
Review [Checking laws with discipline][clwd] for the details:

```scala
scala> import cats._, cats.std.all._
import cats._
import cats.std.all._

scala> import algebra.laws.GroupLaws
import algebra.laws.GroupLaws

scala> val rs1 = GroupLaws[Int].semigroup(Semigroup.additive[Int])
rs1: algebra.laws.GroupLaws[Int]#GroupProperties = algebra.laws.GroupLaws\$GroupProperties@3f8dc1c5

scala> rs1.all.check
+ semigroup.associativity: OK, passed 100 tests.
+ semigroup.combineN(a, 1) == a: OK, passed 100 tests.
+ semigroup.combineN(a, 2) == a |+| a: OK, passed 100 tests.
+ semigroup.serializable: OK, proved property.

scala> val rs2 = GroupLaws[Int].semigroup(Semigroup.multiplicative[Int])
rs2: algebra.laws.GroupLaws[Int]#GroupProperties = algebra.laws.GroupLaws\$GroupProperties@699dd73d

scala> rs2.all.check
+ semigroup.associativity: OK, passed 100 tests.
+ semigroup.combineN(a, 1) == a: OK, passed 100 tests.
+ semigroup.combineN(a, 2) == a |+| a: OK, passed 100 tests.
+ semigroup.serializable: OK, proved property.
```

#### `List`s are `Semigroup`s

```console
scala> List(1, 2, 3) |+| List(4, 5, 6)
```

#### Product and Sum

For `Int` a semigroup can be formed under both `+` and `*`.
Instead of tagged types, non/algebra provides two instances of
semigroup instances for `Int`: additive and multiplicative.

Trying to use operator syntax here is tricky.

```console
scala> def doSomething[A: Semigroup](a1: A, a2: A): A =
         a1 |+| a2
scala> doSomething(3, 5)(Semigroup.additive[Int])
scala> doSomething(3, 5)(Semigroup.multiplicative[Int])
```

I might as well stick to function syntax:

```console
scala> Semigroup.additive[Int].combine(3, 5)
scala> Semigroup.multiplicative[Int].combine(3, 5)
```
