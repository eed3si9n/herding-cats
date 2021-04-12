---
out: ApplicativeError.html
---

### ApplicativeError

In Scala there are multiple ways to reprent the error state. Cats provides `ApplicativeError` typeclass to represent raising and recovering from an error.

```scala
trait ApplicativeError[F[_], E] extends Applicative[F] {
  def raiseError[A](e: E): F[A]

  def handleErrorWith[A](fa: F[A])(f: E => F[A]): F[A]


  def recover[A](fa: F[A])(pf: PartialFunction[E, A]): F[A] =
    handleErrorWith(fa)(e => (pf.andThen(pure(_))).applyOrElse(e, raiseError[A](_)))
  def recoverWith[A](fa: F[A])(pf: PartialFunction[E, F[A]]): F[A] =
    handleErrorWith(fa)(e => pf.applyOrElse(e, raiseError))
}
```

#### Either as ApplicativeError

```scala mdoc
import cats._, cats.syntax.all._

{
  val F = ApplicativeError[Either[String, *], String]
  F.raiseError("boom")
}
```

```scala mdoc
{
  val F = ApplicativeError[Either[String, *], String]
  val e = F.raiseError("boom")
  F.recover(e) {
    case "boom" => 1
  }
}
```

An interesting thing to note is that unlike try-catch, where the type switches between the error type `Throwable` and the happy type `A`, `ApplicativeError` needs to hold on to both `E` and `A` as data.

#### scala.util.Try as ApplicativeError

```scala mdoc
import scala.util.Try

{
  val F = ApplicativeError[Try, Throwable]
  F.raiseError(new RuntimeException("boom"))
}
```

```scala mdoc
{
  val F = ApplicativeError[Try, Throwable]
  val e = F.raiseError(new RuntimeException("boom"))
  F.recover(e) {
    case _: Throwable => 1
  }
}
```

#### IO as ApplicativeError

Given that `IO` needs to run inside of a fiber, it has the ability to capture the error state similar to `scala.util.Try` and `Future`.

```scala mdoc
import cats.effect.IO

{
  val F = ApplicativeError[IO, Throwable]
  F.raiseError(new RuntimeException("boom"))
}
```

```scala mdoc
{
  val F = ApplicativeError[IO, Throwable]
  val e = F.raiseError(new RuntimeException("boom"))
  val io: IO[Int] = F.recover(e) {
    case _: Throwable => 1
  }
}
```
