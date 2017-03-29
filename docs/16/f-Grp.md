
### Grp

Awodey:

> **Definition 1.4** A *group* G is a monoid with an inverse g<sup>-1</sup> for every element g. Thus, G is a category with one object, in which every arrow is an isomorphism.

Here's the typeclass contract of `cats.kernel.Monoid`:

```scala
/**
 * A group is a monoid where each element has an inverse.
 */
trait Group[@sp(Int, Long, Float, Double) A] extends Any with Monoid[A] {

  /**
   * Find the inverse of `a`.
   *
   * `combine(a, inverse(a))` = `combine(inverse(a), a)` = `empty`.
   */
  def inverse(a: A): A
}
```

This enables `inverse` method if the syntax is imported.

```console:new
scala> import cats._, cats.data._, cats.implicits._
scala> 1.inverse
scala> assert((1 |+| 1.inverse) === Monoid[Int].empty)
```

The category of groups and group homomorphism (functions that preserve the monoid structure) is denoted by **Grp**.

### Forgetful functor

We've seen the term homomorphism a few times, but it's possible to think of a function that doesn't preserve the structure.
Because every group *G* is also a monoid we can think of a function *f: G => M* where *f* loses the inverse ability from *G* and returns underlying monoid as *M*. Since both groups and monoids are categories, *f* is a functor.

We can extend this to the entire **Grp**, and think of a functor *F: Grp => Mon*. These kinds of functors that strips the structure is called *forgetful functors*. If we try to express this using Scala, you would start with `A: Group`, and somehow downgrade to `A: Monoid` as the ruturn value.

That's it for today.
