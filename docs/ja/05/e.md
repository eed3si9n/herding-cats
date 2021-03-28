---
out: knights-quest.html
---

#### 騎士の旅

LYAHFGG:

> ここで、非決定性計算を使って解くのにうってつけの問題をご紹介しましょう。チェス盤の上にナイトの駒が1つだけ乗っています。ナイトを3回動かして特定のマスまで移動させられるか、というのが問題です。

ペアに型エイリアスを付けるかわりにまた case class にしよう:

```scala mdoc
case class KnightPos(c: Int, r: Int)
```

以下がナイトの次に取りうる位置を全て計算する関数だ:

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

答は合ってるみたいだ。次に、3回のチェインを実装する:

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

`(6, 2)` からは 3手で `(6, 1)` に動かすことができるけども、`(7, 3)` は無理のようだ。ピエールの鳥の例と同じように、モナド計算の鍵となっているのは 1手の効果が次に伝搬していることだと思う。

また、続きはここから。
