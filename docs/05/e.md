---
out: knights-quest.html
---

#### A knight's quest

LYAHFGG:

> Here's a problem that really lends itself to being solved with non-determinism. Say you have a chess board and only one knight piece on it. We want to find out if the knight can reach a certain position in three moves.

Instead of type aliasing a pair, let's make this into a case class again:

```scala mdoc
case class KnightPos(c: Int, r: Int)
```

Here's the function to calculate all of the knight's next positions:

```scala mdoc:reset
case class KnightPos(c: Int, r: Int) {
  def move: List[KnightPos] =
    for {
      KnightPos(c2, r2) <- List(KnightPos(c + 2, r - 1), KnightPos(c + 2, r + 1),
        KnightPos(c - 2, r - 1), KnightPos(c - 2, r + 1),
        KnightPos(c + 1, r - 2), KnightPos(c + 1, r + 2),
        KnightPos(c - 1, r - 2), KnightPos(c - 1, r + 2)) if (
        ((1 to 8).toList contains c2) && ((1 to 8).toList contains r2))
    } yield KnightPos(c2, r2)
}

KnightPos(6, 2).move

KnightPos(8, 1).move
```

The answers look good. Now we implement chaining this three times:

```scala mdoc:reset
case class KnightPos(c: Int, r: Int) {
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

KnightPos(6, 2) canReachIn3 KnightPos(6, 1)

KnightPos(6, 2) canReachIn3 KnightPos(7, 3)
```

As it turns out, from `(6, 2)` you can reach `(6, 1)` in three moves, but not `(7, 3)`. As with Pierre's bird example, one of key aspect of the monadic calculation is that the effect of one move can be passed on to the next.

We'll pick up from here.
