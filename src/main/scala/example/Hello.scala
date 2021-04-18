package example

/*
import cats._, cats.syntax.all._
import cats.effect.IO

object Hello extends App {
  val program = for {
    _ <- IO.print("What's your name? ")
    x <- IO.readLine
    _ <- IO.println(s"Hello, $x")
  } yield ()

  import cats.effect.unsafe.implicits.global
  program.unsafeRunSync()
}

import cats._, cats.syntax.all._
import cats.effect.{ ExitCode, IO, IOApp }

object Hello extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    program.as(ExitCode.Success)

  lazy val program = for {
    _ <- IO.print("What's your name? ")
    x <- IO.readLine
    _ <- IO.println(s"Hello, $x")
  } yield ()
}

*/

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
