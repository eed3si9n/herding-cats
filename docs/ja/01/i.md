---
out: typeclasses-102.html
---

  [tt]: http://learnyouahaskell.com/types-and-typeclasses
  [moott]: http://learnyouahaskell.com/making-our-own-types-and-typeclasses

### 型クラス中級講座

Haskell の文法に関しては飛ばして第8章の[型や型クラスを自分で作ろう][moott] まで行こう (本を持っている人は第7章)。

### 信号の型クラス

```haskell
data TrafficLight = Red | Yellow | Green
```

これを Scala で書くと:

```console:new
scala> import cats._, cats.std.all._
scala> :paste
sealed trait TrafficLight
object TrafficLight {
  case object Red extends TrafficLight
  case object Yellow extends TrafficLight
  case object Green extends TrafficLight
}
```

これに `Eq` のインスタンスを定義する。

```console
scala> implicit val trafficLightEq: Eq[TrafficLight] =
  new Eq[TrafficLight] {
    def eqv(a1: TrafficLight, a2: TrafficLight): Boolean = a1 == a2
  }
```

**注意**: 最新の `algebra.Equal` には `Equal.instance` と `Equal.fromUniversalEquals` も定義されている。

`Eq` を使えるかな?

```console:error
scala> import cats.syntax.eq._
scala> TrafficLight.Red === TrafficLight.Yellow
```


`Eq` が不変 (invariant) なサブタイプ `Eq[A]` を持つせいで、`Eq[TrafficLight]` が検知されないみたいだ。
この問題を回避する方法としては、`TrafficLight` にキャストするヘルパー関数を定義するという方法がある:

```console
scala> :paste
sealed trait TrafficLight
object TrafficLight {
  def red: TrafficLight = Red
  def yellow: TrafficLight = Yellow
  def green: TrafficLight = Green
  case object Red extends TrafficLight
  case object Yellow extends TrafficLight
  case object Green extends TrafficLight
}
scala> implicit val trafficLightEq: Eq[TrafficLight] =
  new Eq[TrafficLight] {
    def eqv(a1: TrafficLight, a2: TrafficLight): Boolean = a1 == a2
  }
scala> TrafficLight.red === TrafficLight.yellow
```

ちょっと冗長だけども、一応動いた。
