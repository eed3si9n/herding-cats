---
out: stackless-scala-with-free-monads.html
---

  [@runarorama]: https://twitter.com/runarorama
  [dsdi]: http://functionaltalks.org/2013/06/17/runar-oli-bjarnason-dead-simple-dependency-injection/
  [ssfmvid]: http://skillsmatter.com/podcast/scala/stackless-scala-free-monads
  [ssfmpaper]: http://days2012.scala-lang.org/sites/days2012/files/bjarnason_trampolines.pdf
  [322]: https://github.com/non/cats/pull/322

### Stackless Scala with Free Monads

The notion of free monad goes beyond just the interpreter pattern.
I think people are still discovering new ways of harnessing its power.

Rúnar ([@runarorama][@runarorama]) has been a proponent of using `Free` in Scala.
His talk we've covered on day 6, [Dead-Simple Dependency Injection][dsdi], uses `Free` to implement
a mini language to implement key-value store.
The same year, Rúnar also gave a talk at Scala Days 2012 called
[Stackless Scala With Free Monads][ssfmvid].
I recommend watching the talk before reading the paper, but it's easier to quote the paper version
[Stackless Scala With Free Monads][ssfmpaper].

Rúnar starts out with a code that uses an implementation of `State` monad to zip a list with index.
It blows the stack when the list is larger than the stack limit.
Then he introduces tranpoline, which is a single loop that drives the entire program.

```scala
sealed trait Trampoline [+ A] {
  final def runT : A =
    this match {
      case More (k) => k().runT
      case Done (v) => v
    }
}
case class More[+A](k: () => Trampoline[A])
  extends Trampoline[A]
case class Done [+A](result: A)
  extends Trampoline [A]
```

In the above code, `Function0` `k` is used as a thunk for the next step.

To extend its usage for State monad, he then reifies `flatMap` into a data structure called `FlatMap`:

```scala
case class FlatMap [A,+B](
  sub: Trampoline [A],
  k: A => Trampoline[B]) extends Trampoline[B]
```

Next, it is revealed that `Trampoline` is a free monad of `Function0`. Here's how it's defined in Cats:

```scala
  type Trampoline[A] = Free[Function0, A]
```

#### Trampoline

Using Trampoline any program can be transformed into a stackless one.
`Trampoline` object defines a few useful functions for tramplining:

```scala
object Trampoline {
  def done[A](a: A): Trampoline[A] =
    Free.Pure[Function0,A](a)

  def suspend[A](a: => Trampoline[A]): Trampoline[A] =
    Free.Suspend[Function0, A](() => a)

  def delay[A](a: => A): Trampoline[A] =
    suspend(done(a))
}
```

Let's try implementing `even` and `odd` from the talk:

```console:new
scala> import cats._, cats.std.all._, cats.free.{ Free, Trampoline }
scala> import Trampoline._
scala> :paste
def even[A](ns: List[A]): Trampoline[Boolean] =
  ns match {
    case Nil => done(true)
    case x :: xs => suspend(odd(xs))
  }
def odd[A](ns: List[A]): Trampoline[Boolean] =
  ns match {
    case Nil => done(false)
    case x :: xs => suspend(even(xs))
  }
scala> even(List(1, 2, 3)).run
scala> even((0 to 3000).toList).run
```

While implementing the above I ran into SI-7139 again, so I had to tweak the Cats' code. [#322][322]

#### Free monads

In addition, Rúnar introduces several datatypes that can be derived using `Free`:

```scala
type Pair[+A] = (A, A)
type BinTree[+A] = Free[Pair, A]

type Tree[+A] = Free[List, A]

type FreeMonoid[+A] = Free[({type λ[+α] = (A,α)})#λ, Unit]

type Trivial[+A] = Unit
type Option[+A] = Free[Trivial, A]
```

There's also iteratees implementation based on free monads.
Finally, he summarizes free monads in nice bullet points:

> - A model for any recursive data type with data at the leaves.
> - A free monad is an expression tree with variables at the leaves and flatMap is variable substitution.

#### Free monoid using Free

Let's try defining "List" using `Free`.

```console
scala> type FreeMonoid[A] = Free[(A, +?), Unit]
scala> def cons[A](a: A): FreeMonoid[A] =
         Free.Suspend[(A, +?), Unit]((a, Free.Pure[(A, +?), Unit](())))
scala> val x = cons(1)
scala> val xs = cons(1) flatMap {_ => cons(2)}
```

As a way of interpretting the result, let's try converting this to a standard `List`:

```console
scala> implicit def tuple2Functor[A]: Functor[(A, ?)] =
         new Functor[(A, ?)] {
           def map[B, C](fa: (A, B))(f: B => C) =
             (fa._1, f(fa._2))
         }
scala> def toList[A](list: FreeMonoid[A]): List[A] =
         list.fold(
           { _ => Nil },
           { case (x: A @unchecked, xs: FreeMonoid[A]) => x :: toList(xs) })
scala> toList(xs)
```

That's it for today.
