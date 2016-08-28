---
out: making-monads.html
---

### モナドを作る

LYAHFGG:

> この節では、型が生まれてモナドであると確認され、適切な `Monad` インスタンスが与えられるまでの過程を、例題を通して学ぼうと思います。
> ...
> `[3,5,9]` のような非決定的値を表現したいのだけど、さらに `3` である確率は 50パーセント、`5` と `9` である確率はそれぞれ 25パーセントである、ということを表したくなったらどうしましょう？

Scala に有理数が標準で入っていないので、`Double` を使う。以下が case class:

```console:new
scala> import cats._, cats.instances.all._
scala> :paste
case class Prob[A](list: List[(A, Double)])

trait ProbInstances {
  implicit def probShow[A]: Show[Prob[A]] = Show.fromToString
}

case object Prob extends ProbInstances
```

> これってファンクターでしょうか？ええ、リストはファンクターですから、リストに何かを足したものである `Prob` もたぶんファンクターでしょう。

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
scala> import cats.syntax.functor._
scala> Prob((3, 0.5) :: (5, 0.25) :: (9, 0.25) :: Nil) map {-_} 
```

本と同様に `flatten` をまず実装する。

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

これでモナドのための準備は整ったはずだ:

```console
scala> :paste
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
  }
  implicit def probShow[A]: Show[Prob[A]] = Show.fromToString
}

case object Prob extends ProbInstances
```

本によるとモナド則は満たしているらしい。`Coin` の例題も実装してみよう:

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

`flipThree` の実装はこうなる:

```console
scala> import cats.syntax.flatMap._
scala> import cats.syntax.eq._
scala> def flipThree: Prob[Boolean] = for {
  a <- coin
  b <- coin
  c <- loadedCoin
} yield { List(a, b, c) forall {_ === tails} }
scala> flipThree
```

イカサマのコインを 1つ使っても 3回とも裏が出る確率はかなり低いことが分かった。
