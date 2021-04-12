---
out: ApplicativeError.html
---

### ApplicativeError

Scala にはエラー状態を表す方法が複数ある。Cats は、エラーの発生とエラーからのリカバリーを表す `ApplicativeError` という型クラスを提供する。

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

#### ApplicativeError としての Either

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

エラー型の `Throwable` とハッピーな状態の型 `A` が入れ替わる try-catch と違って、`ApplicativeError` は `E` も `A` もデータとして保持しなければいけないことに注目してほしい。

#### ApplicativeError としての scala.util.Try

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

#### ApplicativeError としての IO

`IO` はファイバー内で走る必要があるので、`scala.util.Try` と `Future` のようにエラー状態を捕捉することができる。

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
