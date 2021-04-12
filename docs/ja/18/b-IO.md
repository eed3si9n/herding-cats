---
out: IO.html
---

### IO データ型

Launchbury と SPJ が State Thread を用いたように、Cats Effect はライトウェイトなスレッド的概念である**ファイバー**と呼ばれるものを使ってエフェクトをモデル化する。

```scala
sealed abstract class IO[+A] private () extends IOPlatform[A] {

  def flatMap[B](f: A => IO[B]): IO[B] = IO.FlatMap(this, f)

  ....


  // from IOPlatform
  final def unsafeRunSync()(implicit runtime: unsafe.IORuntime): A
}

object IO extends IOCompanionPlatform with IOLowPriorityImplicits {
  /**
   * Suspends a synchronous side effect in `IO`.
   *
   * Alias for `IO.delay(body)`.
   */
  def apply[A](thunk: => A): IO[A] = Delay(() => thunk)

  def delay[A](thunk: => A): IO[A] = apply(thunk)

  def async[A](k: (Either[Throwable, A] => Unit) => IO[Option[IO[Unit]]]): IO[A] =
    asyncForIO.async(k)

  def async_[A](k: (Either[Throwable, A] => Unit) => Unit): IO[A] =
    asyncForIO.async_(k)

  def canceled: IO[Unit] = Canceled

  def cede: IO[Unit] = Cede

  def sleep(delay: FiniteDuration): IO[Unit] =
    Sleep(delay)

  def race[A, B](left: IO[A], right: IO[B]): IO[Either[A, B]] =
    asyncForIO.race(left, right)

  def readLine: IO[String] =
    Console[IO].readLine

  def print[A](a: A)(implicit S: Show[A] = Show.fromToString[A]): IO[Unit] =
    Console[IO].print(a)

  def println[A](a: A)(implicit S: Show[A] = Show.fromToString[A]): IO[Unit] =
    Console[IO].println(a)

  def blocking[A](thunk: => A): IO[A] =
    Blocking(TypeBlocking, () => thunk)

  def interruptible[A](many: Boolean)(thunk: => A): IO[A] =
    Blocking(if (many) TypeInterruptibleMany else TypeInterruptibleOnce, () => thunk)

  def suspend[A](hint: Sync.Type)(thunk: => A): IO[A] =
    if (hint eq TypeDelay)
      apply(thunk)
    else
      Blocking(hint, () => thunk)

  ....
}
```

#### Hello world

以下は Cats Effect IO を用いた hello world のプログラムだ。

```scala
package example

import cats._, cats.syntax.all._
import cats.effect.IO

object Hello extends App {
  val program = for {
    _ <- IO.print("What's your name? ")
    x <- IO.readLine
    _ <- IO.println(s"Hello, \$x")
  } yield ()
}
```

実行するとこのようになる:

```
> run
[info] running example.Hello
[success] Total time: 1 s, completed Apr 11, 2021 12:51:44 PM
```

何も起こらなかったはずだ。標準ライブラリの `scala.concurrent.Future` + 普通の `ExecutionContext` と違って、IO データ型は停止状態のエフェクトを表し、明示的に実行するまで実行されない。

以下のように走らせることができる:

```scala
package example

import cats._, cats.syntax.all._
import cats.effect.IO

object Hello extends App {
  val program = for {
    _ <- IO.print("What's your name? ")
    x <- IO.readLine
    _ <- IO.println(s"Hello, \$x")
  } yield ()

  import cats.effect.unsafe.implicits.global
  program.unsafeRunSync()
}
```

これで副作用が見えるようになった:

```
sbt:herding-cats> run
[info] running example.Hello
What's your name? eugene
Hello, eugene
[success] Total time: 4 s, completed Apr 11, 2021 1:00:19 PM
```

実際のプログラムを書くときは `IOApp` というより良いプログラムハーネスがあるので、それを使う:

```scala
import cats._, cats.syntax.all._
import cats.effect.{ ExitCode, IO, IOApp }

object Hello extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    program.as(ExitCode.Success)

  lazy val program = for {
    _ <- IO.print("What's your name? ")
    x <- IO.readLine
    _ <- IO.println(s"Hello, \$x")
  } yield ()
}
```

これらの例は IO データ型がモナディックに合成可能であることを例示するが、実行は逐次的だ。

#### Pizza app

もう少し IO の何が嬉しいのかを示すために、http4s client を使ったピザアプリを考える。

```scala mdoc
import cats._, cats.syntax.all._
import cats.effect.IO
import org.http4s.client.Client

def withHttpClient[A](f: Client[IO] => IO[A]): IO[A] = {
  import java.util.concurrent.Executors
  import scala.concurrent.ExecutionContext
  import org.http4s.client.blaze.BlazeClientBuilder
  val threadPool = Executors.newFixedThreadPool(5)
  val httpEc = ExecutionContext.fromExecutor(threadPool)
  BlazeClientBuilder[IO](httpEc).resource.use(f)
}

def search(httpClient: Client[IO], q: String): IO[String] = {
  import io.circe.Json
  import org.http4s.Uri
  import org.http4s.circe._
  val baseUri = Uri.unsafeFromString("https://api.duckduckgo.com/")
  val target = baseUri
    .withQueryParam("q", q + " pizza")
    .withQueryParam("format", "json")
  httpClient.expect[Json](target) map { json =>
    json.findAllByKey("Abstract").headOption.flatMap(_.asString).getOrElse("")
  }
}

{
  import cats.effect.unsafe.implicits.global
  val program = withHttpClient { httpClient =>
    search(httpClient, "New York")
  }
  program.unsafeRunSync()
}
```

これは Duck Duck Go API に New York スタイルのピザのクエリをする。ネットワーク IO によるレイテンシーを低下させるために、並列呼び出しをしたい:

```scala mdoc
{
  import cats.effect.unsafe.implicits.global
  val program = withHttpClient { httpClient =>
    val xs = List("New York", "Neapolitan", "Sicilian", "Chicago", "Detroit", "London")
    xs.parTraverse(search(httpClient, _))
  }
  program.unsafeRunSync()
}
```

`.parTraverse(...)` は内部でファイバーを作成して IO のアクションを並列実行する。並列な IO ができたところで、`Ref` を使ってみてスレッド安全性を試してみよう。

```scala mdoc
import cats.effect.Ref

def appendCharCount(httpClient: Client[IO], q: String, ref: Ref[IO, List[(String, Int)]]): IO[Unit] =
  for {
    s <- search(httpClient, q)
    _ <- ref.update(((q, s.size)) :: _)
  } yield ()

{
  import cats.effect.unsafe.implicits.global
  val program = withHttpClient { httpClient =>
    val xs = List("New York", "Neapolitan", "Sicilian", "Chicago", "Detroit", "London")

    for {
      r <- Ref[IO].of(Nil: List[(String, Int)])
      _ <- xs.parTraverse(appendCharCount(httpClient, _, r))
      x <- r.get
    } yield x
  }
  program.unsafeRunSync().reverse
}
```

ここでは、IO エフェクトの逐次合成と並列合成の両方を組み合わせている。
