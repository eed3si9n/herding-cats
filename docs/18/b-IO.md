---
out: IO.html
---

### IO datatype

Analoguous to Launchbury and SPJ's "State Threads" paper, Cats Effect uses the notion of lightweight thread called _fibers_ to model the effects.

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

Here's a hello world program using Cats Effect IO.

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

Running this looks like this:

```
> run
[info] running example.Hello
[success] Total time: 1 s, completed Apr 11, 2021 12:51:44 PM
```

Nothing should have happened. Unlike standard library's `scala.concurrent.Future` + standard execution contexts, IO datatype represents an effect in a suspended state, and it does not execute until we tell it to run explicitly.

Here's how we can run it:

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

Now we'll see the side effects:

```
sbt:herding-cats> run
[info] running example.Hello
What's your name? eugene
Hello, eugene
[success] Total time: 4 s, completed Apr 11, 2021 1:00:19 PM
```

Cats Effect comes with a better program harness called `IOApp`, which you should use for actual program:

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

These examples show that IO datatype can compose monadically, but they are executing sequentially.

#### Pizza app

To motivate the use of IO, let's consider a pizza app using http4s client.

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

This constructs a program that queries Duck Duck Go API about New York style pizza. To mitigate the latency involved in the network IO, we'd like to make parallel calls:

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

`.parTraverse(...)` internally spawns fibers so the IO actions would be performed in parallel. Now that we have parallel IOs, we can try using `Ref` again to excercise the thread-safety of `Ref`.

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

Here we're combining sequential and parallel composition of the IO effects.
