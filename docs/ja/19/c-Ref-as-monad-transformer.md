---
out: Ref-as-monad-transformer.html
---

### モナドトランスフォーマーとしての Ref

`Ref` や `Resource` といったデータ型は何か変わっている事がある:

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

`Option` などが受け取るような型パラメータ `A` の他に、`Ref` は `F[_]` でパラメータ化されている。

```scala
scala> :k -v cats.effect.Ref
cats.effect.Ref's kind is X[F[A1],A2]
(* -> *) -> * -> *
This is a type constructor that takes type constructor(s): a higher-kinded type.
```

これらはエフェクト型 `F` を受け取るモナドトランスフォーマーだと言える。`SyncIO` といった別の `F` を渡すことも可能だ:

```scala mdoc
import cats._, cats.syntax.all._
import cats.effect.{ IO, Ref, SyncIO }

lazy val program: SyncIO[Int] = for {
  r <- Ref[SyncIO].of(0)
  x <- r.get
} yield x
```

### モナドトランスフォーマーとしての Resource

ということはリソースも `F[_]` を使ってパラメトリックにすることができる:

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

モナドトランスフォーマーのほとんどは `FunctionK` を受け取る `mapK(...)` というメソッドがあって、別の `G[_]` へと変換することができる。1つのエフェクト型から別のエフェクト型への `~>` を定義できれば、リソースも変換することができる。これはかなり衝撃的だ:

```scala mdoc
lazy val toIO = λ[SyncIO ~> IO](si => IO.blocking { si.unsafeRunSync() })

lazy val r1: Resource[IO, BufferedReader] = r0.mapK(toIO)
```

今日はここまで。
