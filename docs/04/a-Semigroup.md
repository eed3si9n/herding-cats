---
out: Semigroup.html
---

  [clwd]: checking-laws-with-discipline.html
  [fafm]: http://learnyouahaskell.com/functors-applicative-functors-and-monoids

### Semigroup

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

```scala mdoc
import cats._, cats.syntax.all._

assert { (3 * 2) * (8 * 5) === 3 * (2 * (8 * 5)) }

assert { List("la") ++ (List("di") ++ List("da")) === (List("la") ++ List("di")) ++ List("da") }
```

No error means, they are equal.

#### The Semigroup typeclass

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

```scala mdoc
List(1, 2, 3) |+| List(4, 5, 6)

"one" |+| "two"
```

#### The Semigroup Laws

Associativity is the only law for `Semigroup`.

- associativity `(x |+| y) |+| z = x |+| (y |+| z)`

Here's how we can check the Semigroup laws from the REPL.
Review [Checking laws with discipline][clwd] for the details:

```scala
scala> import cats._, cats.data._, cats.implicits._
import cats._
import cats.data._
import cats.implicits._

scala> import cats.kernel.laws.GroupLaws
import cats.kernel.laws.GroupLaws

scala> val rs1 = GroupLaws[Int].semigroup(Semigroup[Int])
rs1: cats.kernel.laws.GroupLaws[Int]#GroupProperties = cats.kernel.laws.GroupLaws\$GroupProperties@5a077d1d

scala> rs1.all.check
+ semigroup.associativity: OK, passed 100 tests.
+ semigroup.combineN(a, 1) == a: OK, passed 100 tests.
+ semigroup.combineN(a, 2) == a |+| a: OK, passed 100 tests.
+ semigroup.serializable: OK, proved property.
```

#### Lists are Semigroups

```scala mdoc
List(1, 2, 3) |+| List(4, 5, 6)
```

#### Product and Sum

For `Int` a semigroup can be formed under both `+` and `*`.
Instead of tagged types, cats provides only the instance  additive.

Trying to use operator syntax here is tricky.

```scala mdoc
def doSomething[A: Semigroup](a1: A, a2: A): A =
  a1 |+| a2

doSomething(3, 5)(Semigroup[Int])
```

I might as well stick to function syntax:

```scala mdoc
Semigroup[Int].combine(3, 5)
```
