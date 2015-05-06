---
out: Kinds.html
---

  [moott]: http://learnyouahaskell.com/making-our-own-types-and-typeclasses
  [cheatsheet]: scalaz-cheatsheet.html
  [scala2340]: https://github.com/scala/scala/pull/2340

### Kinds and some type-foo

[Learn You a Haskell For Great Good][moott] says:

> Types are little labels that values carry so that we can reason about the values. But types have their own little labels, called kinds. A kind is more or less the type of a type. 
> ...
> What are kinds and what are they good for? Well, let's examine the kind of a type by using the :k command in GHCI.

Scala 2.10 didn't have `:k` command, so I wrote [kind.scala](https://gist.github.com/eed3si9n/3610635).
Thanks to George Leontiev ([@folone](https://twitter.com/folone)) and others, `:kind` command is now part of Scala 2.11 ([scala/scala#2340][scala2340]). Let's try using it:

```scala
scala> :k Int
scala.Int's kind is A

scala> :k -v Int
scala.Int's kind is A
*
This is a proper type.
```

`Int` and every other types that you can make a value out of is called a proper type and denoted with a symbol `*` (read "type"). This is analogous to value `1` at value-level. Using Scala's type variable notation this could be written as `A`.

```scala
scala> :k -v Option
scala.Option's kind is F[+A]
* -(+)-> *
This is a type constructor: a 1st-order-kinded type.

scala> :k -v Either
scala.util.Either's kind is F[+A1,+A2]
* -(+)-> * -(+)-> *
This is a type constructor: a 1st-order-kinded type.
```

These are normally called *type constructors*. Another way of looking at it is that it's one step removed from a proper type. So we can call it a first-order-kinded type. This is analogous to a first-order value `(_: Int) + 3`, which we would normally call a function at the value level.

The curried notation uses arrows like `* -> *` and `* -> * -> *`. Note, `Option[Int]` is `*`; `Option` is `* -> *`. Using Scala's type variable notation they could be written as `F[+A]` and `F[+A1,+A2]`.

```scala
scala> :k -v Eq
algebra.Eq's kind is F[A]
* -> *
This is a type constructor: a 1st-order-kinded type.
```

Scala encodes (or complects) the notion of typeclasses using type constructors.
When looking at this, think of it as `Eq` is a typeclass for `A`, a proper type.
This should make sense because you would pass in `Int` into `Eq`.

```scala
scala> :k -v Functor
cats.Functor's kind is X[F[A]]
(* -> *) -> *
This is a type constructor that takes type constructor(s): a higher-kinded type.
```

Again, Scala encodes typeclasses using type constructors,
so when looking at this, think of it as `Functor` is a typeclass for `F[A]`, a type constructor.
This should also make sense because you would pass in `List` into `Functor`.

In other words, this is a type constructor that accepts another type constructor.
This is analogous to a higher-order function, and thus called *higher-kinded type*.
These are denoted as `(* -> *) -> *`. Using Scala's type variable notation this could be written as `X[F[A]]`.

### forms-a vs is-a

The terminology around typeclasses tends to get jumbled up.
For example, The pair `(Int, +)` forms a typeclass called monoid. 
Colloquially, we say things like "is X a monoid?" to mean "can X form a monoid under some operation?"

An example of this is `Either[A, B]`, which we implied that it "is-a" functor yesterday.
This is not completely accurate, because even though it might not be useful, we *could have* defined another left biased functor.
