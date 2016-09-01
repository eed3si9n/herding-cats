---
out: Alternative.html
---

  [day3]: day3.html
  [@mstk]: https://twitter.com/mstk
  [wgc]: https://blog.jle.im/entry/wolf-goat-cabbage-the-list-monadplus-logic-problems.html
  [altrightdist]: https://github.com/typelevel/cats/pull/225#discussion_r29788180

### Alternative

There's a typeclass that combines `Applicative` and `MonoidK` called `Alternative`:

```scala
@typeclass trait Alternative[F[_]] extends Applicative[F] with MonoidK[F] { self =>
   ....
}
```

`Alternative` on its own does not introduce any new methods or operators.

It's more of a weaker (thus better) `Applicative` version of `MonadPlus` that adds `filter` on top of `Monad`.
See [day 3][day3] to review the applicative style of coding.

#### Alternative laws

```scala
trait AlternativeLaws[F[_]] extends ApplicativeLaws[F] with MonoidKLaws[F] {
  implicit override def F: Alternative[F]
  implicit def algebra[A]: Monoid[F[A]] = F.algebra[A]

  def alternativeRightAbsorption[A, B](ff: F[A => B]): IsEq[F[B]] =
    (ff ap F.empty[A]) <-> F.empty[B]

  def alternativeLeftDistributivity[A, B](fa: F[A], fa2: F[A], f: A => B): IsEq[F[B]] =
    ((fa |+| fa2) map f) <-> ((fa map f) |+| (fa2 map f))

  def alternativeRightDistributivity[A, B](fa: F[A], ff: F[A => B], fg: F[A => B]): IsEq[F[B]] =
    ((ff |+| fg) ap fa) <-> ((ff ap fa) |+| (fg ap fa))
}
```

There's an [open question][altrightdist] by Yoshida-san on whether the last law is necessary or not.

#### Wolf, Goat, Cabbage

Here's Justin Le ([@mstk][@mstk])'s 2013 ['Wolf, Goat, Cabbage: The List MonadPlus & Logic Problems.'][wgc] We can try implementing this using `Alternative`.

> A farmer has a wolf, a goat, and a cabbage that he wishes to transport across a river. Unfortunately, his boat can carry only one thing at a time with him. He can’t leave the wolf alone with the goat, or the wolf will eat the goat. He can’t leave the goat alone with the cabbage, or the goat will eat the cabbage. How can he properly transport his belongings to the other side one at a time, without any disasters?

```console:new
scala> import cats._, cats.instances.all._, cats.syntax.show._
scala> import cats.syntax.cartesian._, cats.syntax.semigroupk._
scala> :paste
sealed trait Character
case object Farmer extends Character
case object Wolf extends Character
case object Goat extends Character
case object Cabbage extends Character

case class Move(x: Character)

case class Plan(moves: List[Move])

sealed trait Position
case object West extends Position
case object East extends Position

implicit val moveShow = Show.show[Move](_ match {
  case Move(Farmer)  => "F"
  case Move(Wolf)    => "W"
  case Move(Goat)    => "G"
  case Move(Cabbage) => "C"
})
```

#### makeNMoves

Here's making `n` moves

```console
scala> val possibleMoves = List(Farmer, Wolf, Goat, Cabbage) map {Move(_)}
scala> :paste
def makeMove(ps: List[List[Move]]): List[List[Move]] =
  (ps |@| possibleMoves) map { (p, m) =>  List(m) <+> p }
def makeNMoves(n: Int): List[List[Move]] =
  n match {
    case 0 => Nil
    case 1 => makeMove(List(Nil))
    case n => makeMove(makeNMoves(n - 1))
  }
```

We can test this as follows:


```console
scala> makeNMoves(1)
scala> makeNMoves(2)
```

#### isSolution

> Let’s define our helper function `isSolution :: Plan -> Bool`.
> Basically, we want to check if the positions of all of the characters are `East`.

We can define `filter` using just what's available in `Alternative`:

```console
scala> :paste
def filterA[F[_]: Alternative, A](fa: F[A])(cond: A => Boolean): F[A] =
  {
    var acc = Alternative[F].empty[A]
    Alternative[F].map(fa) { x =>
      if (cond(x)) acc = Alternative[F].combineK(acc, Alternative[F].pure(x))
      else ()
    }
    acc
  }
def positionOf(p: List[Move], c: Character): Position =
  {
    def positionFromCount(n: Int): Position = {
      if (n % 2 == 0) West
      else East
    }
    c match {
      case Farmer => positionFromCount(p.size)
      case x      => positionFromCount(filterA(p)(_ == Move(c)).size)
    }
  }

scala> val p = List(Move(Goat), Move(Farmer), Move(Wolf), Move(Goat))
scala> positionOf(p, Farmer)
scala> positionOf(p, Wolf)
```

Here's how we can check all positions are `East`:

```console
scala> :paste
def isSolution(p: List[Move]) =
  {
    val pos = (List(p) |@| possibleMoves) map { (p, m) => positionOf(p, m.x) }
    (filterA(pos)(_ == West)).isEmpty
  }
```

#### makeMove

> What makes a move legal? Well, the farmer has to be on the same side as whatever is being moved.

```console
scala> def moveLegal(p: List[Move], m: Move): Boolean =
         positionOf(p, Farmer) == positionOf(p, m.x)
scala> moveLegal(p, Move(Wolf))
```

> The plan is safe if nothing can eat anything else. That means if the wolf and goat or goat and cabbage sit on the same bank, so too must the farmer.

```console
scala> :paste
def safePlan(p: List[Move]): Boolean =
  {
    val posGoat = positionOf(p, Goat)
    val posFarmer = positionOf(p, Farmer)
    val safeGoat = posGoat != positionOf(p, Wolf)
    val safeCabbage = positionOf(p, Cabbage) != posGoat
    (posFarmer == posGoat) || (safeGoat && safeCabbage)
  }
```

Using these functions we can now re-implement `makeMove`:

```console
scala> :paste
def makeMove(ps: List[List[Move]]): List[List[Move]] =
  (ps |@| possibleMoves) map { (p, m) =>
  if (!moveLegal(p, m)) Nil
  else if (!safePlan(List(m) <+> p)) Nil
  else List(m) <+> p
}
def makeNMoves(n: Int): List[List[Move]] =
  n match {
    case 0 => Nil
    case 1 => makeMove(List(Nil))
    case n => makeMove(makeNMoves(n - 1))
  }
def findSolution(n: Int): Unit =
  filterA(makeNMoves(n))(isSolution) map { p =>
    println(p map {_.show})
  }
```

Let's try solving the puzzle:

```scala
scala> findSolution(6)

scala> findSolution(7)
List(G, F, C, G, W, F, G)
List(G, F, W, G, C, F, G)

scala> findSolution(8)
List(G, F, C, G, W, F, G)
List(G, F, W, G, C, F, G)
List(G, F, C, G, W, F, G)
List(G, F, W, G, C, F, G)
List(G, F, C, G, W, F, G)
List(G, F, W, G, C, F, G)
```

It worked. That's all for today.
