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
*/

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
