---
out: FlatMap.html
---

  [fom]: http://learnyouahaskell.com/a-fistful-of-monads
  [FlatMapSource]: $catsBaseUrl$/core/src/main/scala/cats/FlatMap.scala
  [FlatMapSyntaxSource]: $catsBaseUrl$/core/src/main/scala/cats/syntax/flatMap.scala

### FlatMap

We get to start a new chapter today on [Learn You a Haskell for Great Good][fom].

> Monads are a natural extension applicative functors, and they provide a solution to the following problem: If we have a value with context, `m a`, how do we apply it to a function that takes a normal `a` and returns a value with a context.

Cats breaks down the Monad typeclass into two typeclasses: `FlatMap` and `Monad`.
Here's the typeclass [contract for FlatMap][FlatMapSource]:

```scala
@typeclass trait FlatMap[F[_]] extends Apply[F] {
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  ....
}
```

Note that `FlatMap` extends `Apply`, the weaker version of `Applicative`. And here are [the operators][FlatMapSyntaxSource]:

```scala
class FlatMapOps[F[_], A](fa: F[A])(implicit F: FlatMap[F]) {
  def flatMap[B](f: A => F[B]): F[B] = F.flatMap(fa)(f)
  def mproduct[B](f: A => F[B]): F[(A, B)] = F.mproduct(fa)(f)
  def >>=[B](f: A => F[B]): F[B] = F.flatMap(fa)(f)
  def >>[B](fb: F[B]): F[B] = F.flatMap(fa)(_ => fb)
}
```

It introduces the `flatMap` operator and its symbolic alias `>>=`. We'll worry about the other operators later. We are used to `flapMap` from the standard library:

```console:new
scala> import cats._, cats.std.all._, cats.syntax.flatMap._
scala> (Right(3): Either[String, Int]) flatMap { x => Right(x + 1) }
```

#### Getting our feet wet with Option

Following the book, let's explore `Option`. In this section I'll be less fussy about whether it's using Cats' typeclass or standard library's implementation. Here's `Option` as a functor:

```console
scala> :paste
object Catnip {
  implicit class IdOp[A](val a: A) extends AnyVal {
    def some: Option[A] = Some(a)
  }
  def none[A]: Option[A] = None
}
import Catnip._
scala> "wisdom".some map { _ + "!" }
scala> none[String] map { _ + "!" }
```

Here's `Option` as an `Apply`:

```console
scala> import cats.syntax.apply._
scala> ({(_: Int) + 3}.some) ap 3.some
scala> none[String => String] ap "greed".some
scala> ({(_: String).toInt}.some) ap none[String]
```

Here's `Option` as a `FlatMap`:


```console
scala> 3.some flatMap { (x: Int) => (x + 1).some }
scala> "smile".some flatMap { (x: String) =>  (x + " :)").some }
scala> none[Int] flatMap { (x: Int) => (x + 1).some }
scala> none[String] flatMap { (x: String) =>  (x + " :)").some }
```

Just as expected, we get `None` if the monadic value is `None`.

#### FlatMap laws

FlatMap has a single law called associativity:

- associativity: `(m flatMap f) flatMap g === m flatMap { x => f(x) flatMap {g} }`

Cats defines two more laws in `FlatMapLaws`:

```scala
trait FlatMapLaws[F[_]] extends ApplyLaws[F] {
  implicit override def F: FlatMap[F]

  def flatMapAssociativity[A, B, C](fa: F[A], f: A => F[B], g: B => F[C]): IsEq[F[C]] =
    fa.flatMap(f).flatMap(g) <-> fa.flatMap(a => f(a).flatMap(g))

  def flatMapConsistentApply[A, B](fa: F[A], fab: F[A => B]): IsEq[F[B]] =
    fab.ap(fa) <-> fab.flatMap(f => fa.map(f))

  /**
   * The composition of `cats.data.Kleisli` arrows is associative. This is
   * analogous to [[flatMapAssociativity]].
   */
  def kleisliAssociativity[A, B, C, D](f: A => F[B], g: B => F[C], h: C => F[D], a: A): IsEq[F[D]] = {
    val (kf, kg, kh) = (Kleisli(f), Kleisli(g), Kleisli(h))
    ((kf andThen kg) andThen kh).run(a) <-> (kf andThen (kg andThen kh)).run(a)
  }
}
```

