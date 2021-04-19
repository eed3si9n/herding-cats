---
out: Resource.html
---

  [Regions]: http://okmij.org/ftp/Haskell/regions.html
  [ResourceDoc]: https://typelevel.org/cats-effect/docs/std/resource
  [higher-rank]: https://apocalisp.wordpress.com/2010/07/02/higher-rank-polymorphism-in-scala/
  [MonadCancel]: MonadCancel.html

### Resource datatype

Rúnar closed [Higher-Rank Polymorphism in Scala][higher-rank] with:

> I wonder if we can use this [encoding of rank-2 polymorphic function] to get static guarantees of safe resource access, as in the SIO monad detailed in Lightweight Monadic Regions.

Cats Effect provides [Resource][ResourceDoc] datatype, which might work like Oleg Kiselyov and Chung-chieh Shan's [Lightweight Monadic Regions][Regions] paper. In short, it's a datatype encoding of [MonadCancel][MonadCancel] we looked at in day 18, but easier to use.

> The simplest way to construct a `Resource` is with `Resource.make` and the simplest way to consume a resource is with `Resource#use`. Arbitrary actions can also be lifted to resources with `Resource.eval`:

```scala
object Resource {
  def make[F[_], A](acquire: F[A])(release: A => F[Unit]): Resource[F, A]

  def eval[F[_], A](fa: F[A]): Resource[F, A]

  def fromAutoCloseable[F[_], A <: AutoCloseable](acquire: F[A])(
      implicit F: Sync[F]): Resource[F, A] =
    Resource.make(acquire)(autoCloseable => F.blocking(autoCloseable.close()))
}

sealed abstract class Resource[F[_], +A] {
  def use[B](f: A => F[B]): F[B]
}
```

[Lightweight Monadic Regions][Regions]:

> We use the following motivating example inspired by real life:
>
> 1. open two files for reading, one of them a configuration file;
> 2. read the name of an output file (such as the log file) from the configuration file;
> 3. open the output file and zip the contents of both input files into the output file;
> 4. close the configuration file;
> 5. copy the rest, if any, of the other input file to the output file.

Here's a program that reads from the first line of a text file:

```scala mdoc
import cats._, cats.syntax.all._
import cats.effect.{ IO, MonadCancel, Resource }
import java.io.{ BufferedReader, BufferedWriter }
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

def bufferedReader(path: Path): Resource[IO, BufferedReader] =
  Resource.fromAutoCloseable(IO.blocking {
    Files.newBufferedReader(path, StandardCharsets.UTF_8)
  })
  .onFinalize { IO.println("closed " + path) }

lazy val program: IO[String] = {
  val r0 = bufferedReader(Paths.get("docs/19/00.md"))
  r0 use { reader0 =>
    IO.blocking { reader0.readLine }
  }
}
```

```scala
scala> {
         import cats.effect.unsafe.implicits._
         program.unsafeRunSync()
       }
closed docs/19/00.md
val res0: String = ---
```

Here's another program that writes text into a file:

```scala mdoc
def bufferedWriter(path: Path): Resource[IO, BufferedWriter] =
  Resource.fromAutoCloseable(IO.blocking {
    Files.newBufferedWriter(path, StandardCharsets.UTF_8)
  })
  .onFinalize { IO.println("closed " + path) }

lazy val program2: IO[Unit] = {
  val w0 = bufferedWriter(Paths.get("/tmp/Resource.txt"))
  w0 use { writer0 =>
    IO.blocking { writer0.write("test\n") }
  }
}

{
  import cats.effect.unsafe.implicits._
  program2.unsafeRunSync()
}
```

This created a text file named `/tmp/Resource.txt`. Thus far the resource management aspect has been trivial. The actual problem presented by Oleg and Chung-chieh Shan is more complicated because the name of the log file is read from the config file, but it outlives the life cycle of the config file.

```scala mdoc
def inner(input0: BufferedReader, config: BufferedReader): IO[(BufferedWriter, IO[Unit])] = for {
  fname <- IO.blocking { config.readLine }
  w0 = bufferedWriter(Paths.get(fname))

  // do the unsafe allocated
  p <- w0.allocated
  (writer0, releaseWriter0) = p
  _ <- IO.blocking { writer0.write(fname + "\n") }
  - <-
    (for {
      l0 <- IO.blocking { input0.readLine }
      _  <- IO.blocking { writer0.write(l0 + "\n") }
      l1 <- IO.blocking { config.readLine }
      _  <- IO.blocking { writer0.write(l1 + "\n") }
    } yield ()).whileM_(IO.blocking { input0.ready && config.ready })
} yield (writer0, releaseWriter0)

lazy val program3: IO[Unit] = {
  val r0 = bufferedReader(Paths.get("docs/19/00.md"))
  r0 use { input0 =>
    MonadCancel[IO].bracket({
      val r1 = bufferedReader(Paths.get("src/main/resources/a.conf"))
      r1 use { config => inner(input0, config) }
    })({ case (writer0, _) =>
      (for {
        l0 <- IO.blocking { input0.readLine }
        _  <- IO.blocking { writer0.write(l0 + "\n") }
      } yield ()).whileM_(IO.blocking { input0.ready })
    })({
      case (_, releaseWriter0) => releaseWriter0
    })
  }
}
```

To avoid closing the log file, I'm using `Resource#allocated` method, and to make sure it would eventually be closed I'm using `MonadCancel[IO].bracket`. Here's what happens when we run this:

```scala
scala> {
         import cats.effect.unsafe.implicits._
         program3.unsafeRunSync()
       }
closed src/main/resources/a.conf
closed /tmp/zip_test.txt
closed docs/19/00.md
```

We see that the config file is closed before everything else.

So we were able to implement the example by cheating a bit, but it shows the flexibility of `Resource`.

#### Resource as Monad

`program3` got complicated, but more often than not we can acquire and release some resources together.

```scala mdoc
lazy val program4: IO[String] = (
  for {
    r0 <- bufferedReader(Paths.get("docs/19/00.md"))
    r1 <- bufferedReader(Paths.get("src/main/resources/a.conf"))
    w1 <- bufferedWriter(Paths.get("/tmp/zip_test.txt"))
  } yield (r0, r1, w1)
).use { case (intput0, config, writer0) =>
  IO.blocking { intput0.readLine }
}

{
  import cats.effect.unsafe.implicits._
  program4.unsafeRunSync()
}
```

In the above, `Resource`s are combined into one monadically, and then used.

#### Resource cancellation

To make sure the resources are cancelled during usage, let's write a demo application that would print `.` forever, and cancel the program using Ctrl-C:

```scala
import cats._, cats.syntax.all._
import cats.effect.{ ExitCode, IO, IOApp, Resource }
import java.io.{ BufferedReader, BufferedWriter }
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

object Hello extends IOApp {
  def bufferedReader(path: Path): Resource[IO, BufferedReader] =
    Resource.fromAutoCloseable(IO.blocking {
      Files.newBufferedReader(path, StandardCharsets.UTF_8)
    })
    .onFinalize { IO.println("closed " + path) }

  override def run(args: List[String]): IO[ExitCode] =
    program.as(ExitCode.Success)

  lazy val program: IO[String] = (
    for {
      r0 <- bufferedReader(Paths.get("docs/19/00.md"))
      r1 <- bufferedReader(Paths.get("src/main/resources/a.conf"))
    } yield (r0, r1)
  ).use { case (intput0, config) =>
    IO.print(".").foreverM
  }
}
```

And here's the result of running the app:

```
\$ java -jar target/scala-2.13/herding-cats-assembly-0.1.0-SNAPSHOT.jar
..................................................................................................................................................................................................................................................................................................................................................................................................................................................................................^C............................................................................................................................................................................................................................................closed src/main/resources/a.conf
closed docs/19/00.md
```

Great. Resources are correctly closed.

Note that this is different from `Resource` forming `MonadCancel`, since it's happening within `use { ... }`. We can look at the definition of `use` to understand this better:

```scala
  /**
   * Allocates a resource and supplies it to the given function.
   * The resource is released as soon as the resulting `F[B]` is
   * completed, whether normally or as a raised error.
   *
   * @param f the function to apply to the allocated resource
   * @return the result of applying [F] to
   */
  def use[B](f: A => F[B])(implicit F: MonadCancel[F, Throwable]): F[B] =
    fold(f, identity)
```

In this case I think Ctrl-C is handled by IO, and `use { ... }` guarantees to release the resource when `f` fails.
