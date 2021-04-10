---
out: Alternative.html
---

  [day3]: day3.html
  [@mstk]: https://twitter.com/mstk
  [wgc]: https://blog.jle.im/entry/wolf-goat-cabbage-the-list-monadplus-logic-problems.html
  [altrightdist]: https://github.com/typelevel/cats/pull/225#discussion_r29788180

### Alternative

`Alternative` という `Applicative` と `MonoidK` を組み合わせた型クラスがある:

```scala
@typeclass trait Alternative[F[_]] extends Applicative[F] with MonoidK[F] { self =>
   ....
}
```

`Alternative` そのものは新しいメソッドや演算子を導入しない。

これは `Monad` 上に `filter` などを追加する `MonadPlus` を弱くした (なのでかっこいい) `Applicative` 版だと考えることができる。
Applicative スタイルについては[3日目][day3]を参照。

#### Alternative 則

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

最後の法則に関しては、それが不必要では無いかという[未回答なままの質問][altrightdist]が吉田さんから出ている。

#### オオカミ、ヤギ、キャベツ

Justin Le ([@mstk][@mstk]) さんが 2013年に書いた　[「オオカミ、ヤギ、キャベツ: List MonadPlus とロジックパズル」][wgc] を `Alternative` で実装してみよう。

<blockquote class="twitter-tweet" data-lang="en"><p lang="en" dir="ltr">Wolf, Goat, Cabbage: Solving simple logic problems in <a href="https://twitter.com/hashtag/haskell?src=hash">#haskell</a> using the List MonadPlus :) <a href="http://t.co/YkKi6EQdDy">http://t.co/YkKi6EQdDy</a></p>&mdash; Justin Le (@mstk) <a href="https://twitter.com/mstk/status/416294820982702080">December 26, 2013</a></blockquote>
<script async src="//platform.twitter.com/widgets.js" charset="utf-8"></script>

> ある農家の人が持ち物のオオカミ、ヤギ、キャベツを連れて川を渡ろうとしている。ところが、ボートには自分以外もう一つのものしか運ぶことができない。オオカミとヤギを放ったらかしにすると、ヤギが食べられてしまう。ヤギとキャベツを放ったらかしにすると、キャベツが食べられてしまう。損害が無いように持ち物を川の向こうまで渡らせるにはどうすればいいだろうか?

```scala mdoc
import cats._, cats.syntax.all._

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

implicit lazy val moveShow = Show.show[Move](_ match {
  case Move(Farmer)  => "F"
  case Move(Wolf)    => "W"
  case Move(Goat)    => "G"
  case Move(Cabbage) => "C"
})
```

#### makeNMoves0

`n` 回の動きはこのように表現できる。

```scala mdoc
val possibleMoves = List(Farmer, Wolf, Goat, Cabbage) map {Move(_)}

def makeMove0(ps: List[List[Move]]): List[List[Move]] =
  (ps , possibleMoves) mapN { (p, m) =>  List(m) <+> p }

def makeNMoves0(n: Int): List[List[Move]] =
  n match {
    case 0 => Nil
    case 1 => makeMove0(List(Nil))
    case n => makeMove0(makeNMoves0(n - 1))
  }
```

テストしてみる:

```scala mdoc
makeNMoves0(1)

makeNMoves0(2)
```

#### isSolution

> ヘルパー関数の `isSolution :: Plan -> Bool` を定義してみよう。
> 基本的にｈ，全てのキャラクターの位置が `East` であることをチェックする。

`Alternative` にあるものだけで `filter` を定義できる:

```scala mdoc
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

val p = List(Move(Goat), Move(Farmer), Move(Wolf), Move(Goat))

positionOf(p, Farmer)

positionOf(p, Wolf)
```

全ての位置が `East` であるかは以下のようにチェックできる:

```scala mdoc
def isSolution(p: List[Move]) =
  {
    val pos = (List(p), possibleMoves) mapN { (p, m) => positionOf(p, m.x) }
    (filterA(pos)(_ == West)).isEmpty
  }
```

#### makeMove

> 合法な動きとはどういうことだろう? とりあえず、農家の人が川の同じ岸にいる必要がある。

```scala mdoc
def moveLegal(p: List[Move], m: Move): Boolean =
  positionOf(p, Farmer) == positionOf(p, m.x)

moveLegal(p, Move(Wolf))
```

> 誰も何も食べなければ、計画は安全だと言える。つまり、オオカミとヤギ、もしくはヤギとキャベツが同じ岸にいる場合は農家の人も一緒にいる必要がある。

```scala mdoc
def safePlan(p: List[Move]): Boolean =
  {
    val posGoat = positionOf(p, Goat)
    val posFarmer = positionOf(p, Farmer)
    val safeGoat = posGoat != positionOf(p, Wolf)
    val safeCabbage = positionOf(p, Cabbage) != posGoat
    (posFarmer == posGoat) || (safeGoat && safeCabbage)
  }
```

これらの関数を使って `makeMove` を再実装できる:

```scala mdoc
def makeMove(ps: List[List[Move]]): List[List[Move]] =
  (ps, possibleMoves) mapN { (p, m) =>
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

パズルを解いてみる:

```scala mdoc
findSolution(6)

findSolution(7)

findSolution(8)
```

うまくいった。今日はここまで。
