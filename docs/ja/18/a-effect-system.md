---
out: effect-system.html
---

  [LFST]: http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.144.2237&rep=rep1&type=pdf
  [CE]: https://typelevel.org/cats-effect/
  [Monix]: https://monix.io/
  [Ref]: https://typelevel.org/cats-effect/docs/std/ref
  [RefSource]: $catsEffectBaseUrl$/kernel/shared/src/main/scala/cats/effect/kernel/Ref.scala

<div class="floatingimage">
<img src="../files/openphoto-6987bw.jpg">
<div class="credit">Daniel Steger for openphoto.net</div>
</div>

### エフェクトシステム

[Lazy Functional State Threads][LFST] において John Launchbury さんと Simon Peyton-Jones さん曰く::

> Based on earlier work on monads, we present a way of securely encapsulating stateful computations that manipulate multiple, named, mutable objects, in the context of a non-strict purely-functional language.

Scala には `var` があるので、可変性をカプセル化するのは一見すると無意味に思えるかもしれないが、stateful な計算を抽象化すると役に立つこともある。並列に実行される計算など特殊な状況下では、状態が共有されないかもしくは慎重に共有されているかどうかが正誤を分ける

Cats のエコシステムでは [Cats Effect][CE] と [Monix][Monix] の両者がエフェクトシステムを提供する。State Threads を流しつつ Cats Effect をみていこう。

#### Cats Effect sbt セットアップ

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

`Ref` は Cats Effect の `IO` モナドのコンテキストの内部で使われる、スレッドセーフな可変変数だ。

[Ref][Ref] 曰く:

> Ref は、そのコンテンツの安全な並列アクセスと変更を提供するが、同期機能は持たない。

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

このように使うことができる:

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

`e1` は `0` で初期化した新しい `Ref` を作成して、`1` を加算して変更する。`e2` は `e1` と合成して、内部値を取得する。最後に、エフェクトを実行するために `unsafeRunSync()` を呼ぶ。
