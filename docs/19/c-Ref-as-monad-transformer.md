---
out: Ref-as-monad-transformer.html
---

### Ref as monad transformer

There's something difference about the datatypes like `Ref` and `Resource`:

```scala
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

In addition to the type parameter `A` like `Option` would take, `Ref` is also parameterized by `F[_]`.

```scala
scala> :k -v cats.effect.Ref
cats.effect.Ref's kind is X[F[A1],A2]
(* -> *) -> * -> *
This is a type constructor that takes type constructor(s): a higher-kinded type.
```

These are monad transformers that take the effects type `F` as parameter. We can actually put another `F` like `SyncIO`:

```scala mdoc
import cats._, cats.syntax.all._
import cats.effect.{ IO, Ref, SyncIO }

lazy val program: SyncIO[Int] = for {
  r <- Ref[SyncIO].of(0)
  x <- r.get
} yield x
```

### Resource as monad transformer

We could probably make resource parametric on `F[_]`:

```scala mdoc
import cats.effect.{ IO, MonadCancel, Resource, Sync }
import java.io.BufferedReader
import java.nio.charset.{ Charset, StandardCharsets }
import java.nio.file.{ Files, Path, Paths }

def bufferedReader[F[_]: Sync](path: Path, charset: Charset): Resource[F, BufferedReader] =
  Resource.fromAutoCloseable(Sync[F].blocking {
    Files.newBufferedReader(path, charset)
  })

lazy val r0: Resource[SyncIO, BufferedReader] = bufferedReader[SyncIO](Paths.get("/tmp/foo"), StandardCharsets.UTF_8)
```

On most of the monad transformers there is `mapK(...)` method that takes `FunctionK` to transform it to another `G[_]`. If we can define a `~>` from one effect type to another, we can transform the resource, which is radical:

```scala mdoc
lazy val toIO = λ[SyncIO ~> IO](si => IO.blocking { si.unsafeRunSync() })

lazy val r1: Resource[IO, BufferedReader] = r0.mapK(toIO)
```

That's it for today.
