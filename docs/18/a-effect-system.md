---
out: effect-system.html
---

  [LFST]: http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.144.2237&rep=rep1&type=pdf
  [CE]: https://typelevel.org/cats-effect/
  [Monix]: https://monix.io/
  [Ref]: https://typelevel.org/cats-effect/docs/std/ref
  [RefSource]: $catsEffectBaseUrl$/kernel/shared/src/main/scala/cats/effect/kernel/Ref.scala

<div class="floatingimage">
<img src="files/openphoto-6987bw.jpg">
<div class="credit">Daniel Steger for openphoto.net</div>
</div>

### Effect system

In [Lazy Functional State Threads][LFST], John Launchbury and Simon Peyton-Jones write:

> Based on earlier work on monads, we present a way of securely encapsulating stateful computations that manipulate multiple, named, mutable objects, in the context of a non-strict purely-functional language.

Because Scala has `var`s, at first it seems pointless to encapusulate mutation, but the concept of abstracting over stateful computation can be useful. Under some circumstances like parallel execution, it's critical that states are either not shared or shared with care.

[Cats Effect][CE] and [Monix][Monix] both provide effect system for the Cats ecosystem. Let's try looking at Cats Effect using "State Threads" paper as the guide.

#### Cats Effect sbt setup

```scala
val catsEffectVersion = "3.0.2"
val http4sVersion = "1.0.0-M21"

val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion
val http4sBlazeClient = "org.http4s" %% "http4s-blaze-client" % http4sVersion
val http4sCirce = "org.http4s" %% "http4s-circe" % http4sVersion
```

### Ref

LFST:

> What is a "state"? Part of every state is a finite mapping from *reference* to values. ... A reference can be thought of as the name of (or address of) a *variable*, an updatable location in the state capable of holding a value.

`Ref` is a thread-safe mutable variable that's used in the context of Cats Effect's IO monad.

[Ref][Ref] says:

> Ref provides safe concurrent access and modification of its content, but no functionality for synchronisation.

```scala
trait RefSource[F[_], A] {

  /**
   * Obtains the current value.
   *
   * Since `Ref` is always guaranteed to have a value, the returned action
   * completes immediately after being bound.
   */
  def get: F[A]
}

trait RefSink[F[_], A] {

  /**
   * Sets the current value to `a`.
   *
   * The returned action completes after the reference has been successfully set.
   *
   * Satisfies:
   *   `r.set(fa) *> r.get == fa`
   */
  def set(a: A): F[Unit]
}

abstract class Ref[F[_], A] extends RefSource[F, A] with RefSink[F, A] {
  /**
   * Modifies the current value using the supplied update function. If another modification
   * occurs between the time the current value is read and subsequently updated, the modification
   * is retried using the new value. Hence, `f` may be invoked multiple times.
   *
   * Satisfies:
   *   `r.update(_ => a) == r.set(a)`
   */
  def update(f: A => A): F[Unit]

  def modify[B](f: A => (A, B)): F[B]

  ....
}
```

Here's how we can use this:

```scala mdoc
import cats._, cats.syntax.all._
import cats.effect.{ IO, Ref }

def e1: IO[Ref[IO, Int]] = for {
  r <- Ref[IO].of(0)
  _ <- r.update(_ + 1)
} yield r

def e2: IO[Int] = for {
  r <- e1
  x <- r.get
} yield x

{
  import cats.effect.unsafe.implicits._
  e2.unsafeRunSync()
}
```

`e1` creates a new reference with `0`, and updates it by adding `1`. `e2` composes `e1` and retrieves the internal value. Finally, `unsafeRunSync()` is called to run the effect.
