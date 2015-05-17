
  [MonadFilterSource]: $catsBaseUrl$/core/src/main/scala/cats/MonadFilter.scala

### MonadFilter

Scala's `for` comprehension allows filtering:

```console:new
scala> import cats._, cats.std.all._, cats.syntax.show._
scala> for {
         x <- (1 to 50).toList if x.show contains '7'
       } yield x
```

LYAHFGG:

> The `MonadPlus` type class is for monads that can also act as monoids.

Here's [the typeclass contract for `MonadFilter`][MonadFilterSource]:

```scala
/**
 * a Monad equipped with an additional method which allows us to
 * create an "Empty" value for the Monad (for whatever "empty" makes
 * sense for that particular monad). This is of particular interest to
 * us since it allows us to add a `filter` method to a Monad, which is
 * used when pattern matching or using guards in for comprehensions.
 */
@typeclass trait MonadFilter[F[_]] extends Monad[F] {

  def empty[A]: F[A]

  ....
}
```

This enables the derivative functions/operators `filter` and `filterM`:

```scala
@typeclass trait MonadFilter[F[_]] extends Monad[F] {

  def empty[A]: F[A]

  def filter[A](fa: F[A])(f: A => Boolean): F[A] =
    flatMap(fa)(a => if (f(a)) pure(a) else empty[A])

  def filterM[A](fa: F[A])(f: A => F[Boolean]): F[A] =
    flatMap(fa)(a => flatMap(f(a))(b => if (b) pure(a) else empty[A]))
}
```

We can use this like this:

```console
scala> import cats.syntax.monadFilter._
scala> def filterSeven[F[_]: MonadFilter](f: F[Int]): F[Int] =
         f filter { _.show contains '7' }
scala> filterSeven((1 to 50).toList)
```

#### A knight's quest

LYAHFGG:

> Here's a problem that really lends itself to being solved with non-determinism. Say you have a chess board and only one knight piece on it. We want to find out if the knight can reach a certain position in three moves.

Instead of type aliasing a pair, let's make this into a case class again:

```console
scala> case class KnightPos(c: Int, r: Int)
```

Here's the function to calculate all of the knight's next positions:

```console
scala> case class KnightPos(c: Int, r: Int) {
         def move: List[KnightPos] =
           for {
             KnightPos(c2, r2) <- List(KnightPos(c + 2, r - 1), KnightPos(c + 2, r + 1),
               KnightPos(c - 2, r - 1), KnightPos(c - 2, r + 1),
               KnightPos(c + 1, r - 2), KnightPos(c + 1, r + 2),
               KnightPos(c - 1, r - 2), KnightPos(c - 1, r + 2)) if (
               ((1 to 8).toList contains c2) && ((1 to 8).toList contains r2))
           } yield KnightPos(c2, r2)
       }
scala> KnightPos(6, 2).move
scala> KnightPos(8, 1).move
```

The answers look good. Now we implement chaining this three times:

```console
scala> case class KnightPos(c: Int, r: Int) {
         def move: List[KnightPos] =
           for {
             KnightPos(c2, r2) <- List(KnightPos(c + 2, r - 1), KnightPos(c + 2, r + 1),
             KnightPos(c - 2, r - 1), KnightPos(c - 2, r + 1),
             KnightPos(c + 1, r - 2), KnightPos(c + 1, r + 2),
             KnightPos(c - 1, r - 2), KnightPos(c - 1, r + 2)) if (
             ((1 to 8).toList contains c2) && ((1 to 8).toList contains r2))
           } yield KnightPos(c2, r2)
         def in3: List[KnightPos] =
           for {
             first <- move
             second <- first.move
             third <- second.move
           } yield third
         def canReachIn3(end: KnightPos): Boolean = in3 contains end
       }
scala> KnightPos(6, 2) canReachIn3 KnightPos(6, 1)
scala> KnightPos(6, 2) canReachIn3 KnightPos(7, 3)
```

As it turns out, from `(6, 2)` you can reach `(6, 1)` in three moves, but not `(7, 3)`. As with Pierre's bird example, one of key aspect of the monadic calculation is that the effect of one move can be passed on to the next.

We'll pick up from here.
