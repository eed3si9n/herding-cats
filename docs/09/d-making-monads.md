---
out: making-monads.html
---

### Making monads

LYAHFGG:

> In this section, we're going to look at an example of how a type gets made, identified as a monad and then given the appropriate `Monad` instance. 
> ...
> What if we wanted to model a non-deterministic value like `[3,5,9]`, but we wanted to express that `3` has a 50% chance of happening and `5` and `9` both have a 25% chance of happening? 

Since Scala doesn't have a built-in rational, let's just use `Double`. Here's the case class:

```console:new
scala> import cats._, cats.data._, cats.implicits._
scala> :paste
case class Prob[A](list: List[(A, Double)])

trait ProbInstances {
  implicit def probShow[A]: Show[Prob[A]] = Show.fromToString
}

case object Prob extends ProbInstances
```

> Is this a functor? Well, the list is a functor, so this should probably be a functor as well, because we just added some stuff to the list.

```console
scala> :paste
case class Prob[A](list: List[(A, Double)])

trait ProbInstances {
  implicit val probInstance: Functor[Prob] = new Functor[Prob] {
    def map[A, B](fa: Prob[A])(f: A => B): Prob[B] =
      Prob(fa.list map { case (x, p) => (f(x), p) })
  }
  implicit def probShow[A]: Show[Prob[A]] = Show.fromToString
}
case object Prob extends ProbInstances
scala> Prob((3, 0.5) :: (5, 0.25) :: (9, 0.25) :: Nil) map {-_}
```

Just like the book, we are going to implement `flatten` first.

```console
scala> :paste
case class Prob[A](list: List[(A, Double)])

trait ProbInstances {
  def flatten[B](xs: Prob[Prob[B]]): Prob[B] = {
    def multall(innerxs: Prob[B], p: Double) =
      innerxs.list map { case (x, r) => (x, p * r) }
    Prob((xs.list map { case (innerxs, p) => multall(innerxs, p) }).flatten)
  }

  implicit val probInstance: Functor[Prob] = new Functor[Prob] {
    def map[A, B](fa: Prob[A])(f: A => B): Prob[B] =
      Prob(fa.list map { case (x, p) => (f(x), p) })
  }
  implicit def probShow[A]: Show[Prob[A]] = Show.fromToString
}

case object Prob extends ProbInstances
```

This should be enough prep work for monad:

```console
scala> :paste
import scala.annotation.tailrec
case class Prob[A](list: List[(A, Double)])

trait ProbInstances { self =>
  def flatten[B](xs: Prob[Prob[B]]): Prob[B] = {
    def multall(innerxs: Prob[B], p: Double) =
      innerxs.list map { case (x, r) => (x, p * r) }
    Prob((xs.list map { case (innerxs, p) => multall(innerxs, p) }).flatten)
  }

  implicit val probInstance: Monad[Prob] = new Monad[Prob] {
    def pure[A](a: A): Prob[A] = Prob((a, 1.0) :: Nil)
    def flatMap[A, B](fa: Prob[A])(f: A => Prob[B]): Prob[B] = self.flatten(map(fa)(f))
    override def map[A, B](fa: Prob[A])(f: A => B): Prob[B] =
      Prob(fa.list map { case (x, p) => (f(x), p) })
    def tailRecM[A, B](a: A)(f: A => Prob[Either[A, B]]): Prob[B] = {
      val buf = List.newBuilder[(B, Double)]
      @tailrec def go(lists: List[List[(Either[A, B], Double)]]): Unit =
        lists match {
          case (ab :: abs) :: tail => ab match {
            case (Right(b), p) =>
              buf += ((b, p))
              go(abs :: tail)
            case (Left(a), p) =>
              go(f(a).list :: abs :: tail)
          }
          case Nil :: tail => go(tail)
          case Nil => ()
        }
      go(f(a).list :: Nil)
      Prob(buf.result)
    }
  }
  implicit def probShow[A]: Show[Prob[A]] = Show.fromToString
}

case object Prob extends ProbInstances
```

The book says it satisfies the monad laws. Let's implement the `Coin` example:

```console
scala> :paste
sealed trait Coin
object Coin {
  case object Heads extends Coin
  case object Tails extends Coin
  implicit val coinEq: Eq[Coin] = new Eq[Coin] {
    def eqv(a1: Coin, a2: Coin): Boolean = a1 == a2
  }
  def heads: Coin = Heads
  def tails: Coin = Tails
}
import Coin.{heads, tails}
def coin: Prob[Coin] = Prob(heads -> 0.5 :: tails -> 0.5 :: Nil)
def loadedCoin: Prob[Coin] = Prob(heads -> 0.1 :: tails -> 0.9 :: Nil)
```

Here's how we can implement `flipThree`:

```console
scala> def flipThree: Prob[Boolean] = for {
  a <- coin
  b <- coin
  c <- loadedCoin
} yield { List(a, b, c) forall {_ === tails} }
scala> flipThree
```

So the probability of having all three coins on `Tails` even with a loaded coin is pretty low.
