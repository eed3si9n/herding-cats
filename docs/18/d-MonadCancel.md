---
out: MonadCancel.html
---

  [MonadCancelDoc]: https://typelevel.org/cats-effect/docs/typeclasses/monadcancel

### MonadCancel

An interesting thing about Cats Effect is that it is as much a library providing typeclasses for functional effect as much as it is an implementation of `Ref`, `IO` and other datatypes.

`MonadCancel` is a fundamental typeclass extending `MonadError` (Monad extension of `ApplicativeError`) supporting safe cancelation, masking, and finalization. We can think of this as functional construct for try-catch-finally.

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

#### IO as MonadCancel

[MonadCancel documentation][MonadCancelDoc] says:

> One particularly unique aspect of `MonadCancel` is the ability to self-cancel.

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

Here's a less dramatic one:

```scala mdoc
{
  import cats.effect.unsafe.implicits.global
  program.unsafeRunAndForget()
}
```

Either case, the effect is canceled, and `"nope"` action does not take place.

Note that the idea of cancellation is also scripted inside of the IO datatype. This is contrasting to Monix `Task` where the cancellation happens against `CancelableFuture`, after the end of the world so to speak.

#### F.uncancelable

Since cancellation can be inconvenient at certain timings, `MonadCancel` actually provides a `uncancelable` region, which can be used like this:

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

Within `IO.uncancelable { ... }`, cancellation is ignored. To opt back into the cancellation, you can used the passed in `poll` function:

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

`IO.uncancelable { ... }` regions are low-level API, and not likely to be used directly.

#### bracket

[MonadCancel documentation][MonadCancelDoc] says:

> This means that when writing resource-safe code, we have to worry about cancelation as well as exceptions.

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

Using `MonadCancel[IO].bracket` we can guarantee that the cleanup code will run.

That's it for today.
