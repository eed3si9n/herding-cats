---
out: MonadCancel.html
---

  [MonadCancelDoc]: https://typelevel.org/cats-effect/docs/typeclasses/monadcancel

### MonadCancel

Cats Effect の興味深いところは、それが `Ref` や `IO` などのデータ型を提供するライブラリであることと同時に、それは関数型エフェクトは何を意味するのかという型クラスを提供するライブラリでもあることだ。

`MonadCancel` は基盤となる型クラスで、`MonadError` (`ApplicativeError` のモナド版) を拡張し、キャンセル、マスキング (キャンセルの抑制)、ファイナライズをサポートする。関数型的な try-catch-finally だと考えることができる。

```scala
trait MonadCancel[F[_], E] extends MonadError[F, E] {
  def rootCancelScope: CancelScope

  def forceR[A, B](fa: F[A])(fb: F[B]): F[B]

  def uncancelable[A](body: Poll[F] => F[A]): F[A]

  def canceled: F[Unit]

  def onCancel[A](fa: F[A], fin: F[Unit]): F[A]

  def bracket[A, B](acquire: F[A])(use: A => F[B])(release: A => F[Unit]): F[B] =
    bracketCase(acquire)(use)((a, _) => release(a))

  def bracketCase[A, B](acquire: F[A])(use: A => F[B])(
      release: (A, Outcome[F, E, B]) => F[Unit]): F[B] =
    bracketFull(_ => acquire)(use)(release)

  def bracketFull[A, B](acquire: Poll[F] => F[A])(use: A => F[B])(
      release: (A, Outcome[F, E, B]) => F[Unit]): F[B]
}
```

#### MonadCancel としての IO

[MonadCancel documentation][MonadCancelDoc] 曰く:

> `MonadCancel` の非常にユニークな点は、自己キャンセルできることだ。

```scala mdoc
import cats._, cats.syntax.all._
import cats.effect.IO

lazy val program = IO.canceled >> IO.println("nope")
```

```scala
scala> {
         import cats.effect.unsafe.implicits.global
         program.unsafeRunSync()
       }
java.util.concurrent.CancellationException: Main fiber was canceled
  at cats.effect.IO.\$anonfun\$unsafeRunAsync\$1(IO.scala:640)
  at cats.effect.IO.\$anonfun\$unsafeRunFiber\$2(IO.scala:702)
  at scala.runtime.java8.JFunction0\$mcV\$sp.apply(JFunction0\$mcV\$sp.scala:18)
  at cats.effect.kernel.Outcome.fold(Outcome.scala:37)
  at cats.effect.kernel.Outcome.fold\$(Outcome.scala:35)
  at cats.effect.kernel.Outcome\$Canceled.fold(Outcome.scala:181)
  at cats.effect.IO.\$anonfun\$unsafeRunFiber\$1(IO.scala:708)
  at cats.effect.IO.\$anonfun\$unsafeRunFiber\$1\$adapted(IO.scala:698)
  at cats.effect.CallbackStack.apply(CallbackStack.scala:45)
  at cats.effect.IOFiber.done(IOFiber.scala:894)
  at cats.effect.IOFiber.asyncCancel(IOFiber.scala:941)
  at cats.effect.IOFiber.runLoop(IOFiber.scala:458)
  at cats.effect.IOFiber.execR(IOFiber.scala:1117)
  at cats.effect.IOFiber.run(IOFiber.scala:125)
  at cats.effect.unsafe.WorkerThread.run(WorkerThread.scala:358)
```

もう少し落ち着いたバージョン:

```scala mdoc
{
  import cats.effect.unsafe.implicits.global
  program.unsafeRunAndForget()
}
```

いずれにせよ、エフェクトはキャンセルされ、`"nope"` というアクションは起きなかった。

キャンセルという概念そのものも IO データ型の中にスクリプト化されていることに注目してほしい。これは、Monix の `Task` が、`CancelableFuture` に対して行われ、いわゆる「世界の最後」の後に起こるのと対照的だ。

#### F.uncancelable

タイミング的に突然キャンセルされると不便なこともあるので、`MonadCancel` は `uncancelable` リージョンを提供し、このように使うことができる:

```scala mdoc
lazy val program2 = IO.uncancelable { _ =>
  IO.canceled >> IO.println("important")
}
```

```scala
scala> {
         import cats.effect.unsafe.implicits.global
         program2.unsafeRunSync()
       }
important
```

`IO.uncancelable { ... }` 内部では、キャンセルは無視される。再びキャンセルを有効にするためには渡された `poll` 関数を使う:

```scala mdoc
lazy val program3 = IO.uncancelable { poll =>
  poll(IO.canceled) >> IO.println("nope again")
}
```

```scala
scala> {
         import cats.effect.unsafe.implicits.global
         program3.unsafeRunSync()
       }
java.util.concurrent.CancellationException: Main fiber was canceled
  ....
```

`IO.uncancelable { ... }` リージョンは低レベルAPI で直接使うことは少ないと思う。

#### bracket

[MonadCancel documentation][MonadCancelDoc] 曰く:

> リソース安全なコード書くためには、キャンセルと例外の両方の対応をする必要がある。

```scala mdoc
import cats.effect.MonadCancel

lazy val program4 = MonadCancel[IO].bracket(IO.pure(0))(x =>
  IO.raiseError(new RuntimeException("boom")))(_ =>
    IO.println("cleanup"))
```

```scala
scala> {
         import cats.effect.unsafe.implicits.global
         program4.unsafeRunSync()
       }
cleanup
java.lang.RuntimeException: boom
  ....
```

`MonadCancel[IO].bracket` を使うことで、cleanup コードが走ることが保証される。

今日はここまで。
