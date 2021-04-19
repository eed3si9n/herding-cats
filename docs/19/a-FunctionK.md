---
out: FunctionK.html
---

  [Id]: Id.html
  [SemigroupK]: SemigroupK.html
  [MonadCancel]: MonadCancel.html
  [@runarorama]: https://twitter.com/runarorama
  [higher-rank]: https://apocalisp.wordpress.com/2010/07/02/higher-rank-polymorphism-in-scala/
  [LFST]: http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.144.2237&rep=rep1&type=pdf
  [Regions]: http://okmij.org/ftp/Haskell/regions.html

### FunctionK

Cats provides `FunctionK` that accepts two type constructors `F1[_]` and `F2[_]` as type parameter that can transform all values in `F1[A]` to `F2[A]` for all `A`.

```scala
trait FunctionK[F[_], G[_]] extends Serializable { self =>

  /**
   * Applies this functor transformation from `F` to `G`
   */
  def apply[A](fa: F[A]): G[A]

  def compose[E[_]](f: FunctionK[E, F]): FunctionK[E, G] =
    new FunctionK[E, G] { def apply[A](fa: E[A]): G[A] = self(f(fa)) }

  def andThen[H[_]](f: FunctionK[G, H]): FunctionK[F, H] =
    f.compose(self)

  def or[H[_]](h: FunctionK[H, G]): FunctionK[EitherK[F, H, *], G] =
    new FunctionK[EitherK[F, H, *], G] { def apply[A](fa: EitherK[F, H, A]): G[A] = fa.fold(self, h) }

  ....
}
```

`FunctionK[F1, F2]` is denoted symbolically as `F1 ~> F2`:

```scala mdoc
import cats._, cats.syntax.all._

lazy val first: List ~> Option = ???
```

Because we tend to call `F[_]` as functors, sometimes `FunctionK` is aspirationally called a _natural transformation_, but I think `FunctionK` is a better name for what it does.

Let's try implementing `List ~> Option` that returns the first element.

```scala mdoc:reset:invisible
import cats._, cats.syntax.all._
```

```scala mdoc
val first: List ~> Option = new (List ~> Option) {
  def apply[A](fa: List[A]): Option[A] = fa.headOption
}

first(List("a", "b", "c"))
```

It looks a bit verbose. Depending on how often this shows up in the code, we might want a way to write it shorter like how we're usually able to write:

```scala mdoc
import scala.util.chaining._

List("a", "b", "c").pipe(_.headOption)
```

We can do this using _polymorphic lambda rewrite_ `λ` provided by the kind projector:

```scala mdoc:reset:invisible
import cats._, cats.syntax.all._
```

```scala mdoc
val first = λ[List ~> Option](_.headOption)

first(List("a", "b", "c"))
```

#### Higher-Rank Polymorphism in Scala

In July of 2010, Rúnar ([@runarorama][@runarorama]) wrote a blog post [Higher-Rank Polymorphism in Scala][higher-rank], describing the concept of rank-2 polymorphism. First, here's an ordinary (rank-1) polymorphic function:

```scala mdoc
def pureList[A](a: A): List[A] = List(a)
```

This would work for any `A`:

```scala mdoc
pureList(1)

pureList("a")
```

What Rúnar pointed out in 2010 is that Scala does not have a first-class notion for this.

> Now say we want to take such a function as an argument to another function. With just rank-1 polymorphism, we can’t do this:

```scala mdoc:fail
def usePolyFunc[A, B](f: A => List[A], b: B, s: String): (List[B], List[String]) =
  (f(b), f(s))
```

This is also what Launchbury and SPJ pointed out that Haskell cannot do in [State Threads][LFST] in 1994:

```haskell
runST :: ∀a. (∀s. ST s a) -> a
```

> This is not a Hindley-Milner type, because the quantifiers are not all at the top level; it is an example of rank-2 polymorphism.

Back to Rúnar:

> It’s a type error because, `B` and `String` are not `A`. That is, the type `A` is fixed on the right of the quantifier `[A, B]`. We really want the polymorphism of the argument to be maintained so we can apply it polymorphically in the body of our function. Here's how that might be expressed if Scala had rank-n types:

```scala
def usePolyFunc[B](f: (A => List[A]) forAll { A }, b: B, s: String): (List[B], List[String]) =
  (f(b), f(s))
```

> So what we do is represent a rank-2 polymorphic function with a new trait that accepts a type argument in its `apply` method:

```scala
trait ~>[F[_], G[_]] {
  def apply[A](a: F[A]): G[A]
}
```

This is the same as `FunctionK`, or `FunctionK` is the same as `~>`. Next, in a brilliant move Rúnar lifts `A` to `F[_]` using [Id datatype][Id]:

> We can now model a function that takes a value and puts it in a list, as a natural transformation from the identity functor to the List functor:

```scala mdoc:reset:invisible
import cats._, cats.syntax.all._
```

```scala mdoc
val pureList: Id ~> List = λ[Id ~> List](List(_))

def usePolyFunc[B](f: Id ~> List, b: B, s: String): (List[B], List[String]) =
  (f(b), f(s))

usePolyFunc(pureList, 1, "x")
```

Yes. We now managed to pass polymorphic function around. I am guessing that rank-2 polymorphism was all the rage in part because it was advertised as the foundation to ensure typesafe access to resources in [State Threads][LFST] and other papers that came after it.

#### FunctionK in MonadCancel

If we look at [MonadCancel][MonadCancel] again, there's `FunctionK`:

```scala
trait MonadCancel[F[_], E] extends MonadError[F, E] {
  def rootCancelScope: CancelScope

  def forceR[A, B](fa: F[A])(fb: F[B]): F[B]

  def uncancelable[B](body: Poll[F] => F[B]): F[B]

  ....
}
```

In the above, `Poll[F]` is actually a type alias for `F ~> F`:

```scala
trait Poll[F[_]] extends (F ~> F)
```

In other words, for all `A` `F[A]` would return `F[A]`.

```scala mdoc
import cats.effect.IO

lazy val program = IO.uncancelable { poll =>
  poll(IO.canceled) >> IO.println("nope again")
}
```

In the above, `IO` must give us a function that works for any type `A`, and as we know from Rúnar's post rank-1 polymorphism won't work. Imagine if it were:

```scala
def uncancelable[A, B](body: F[A] => F[A] => F[B]): F[B]
```

This might work one call of `poll(...)`, but within `IO.uncancelable { ... }` you should be able to call `poll(...)` multiple times:

```scala mdoc
lazy val program2: IO[Int] = IO.uncancelable { poll =>
  poll(IO.println("a")) >> poll(IO.pure("b")) >> poll(IO.pure(1))
}
```

So really `poll(...)` is `∀A. IO[A] => IO[A]`, or `IO ~> IO`.
