---
out: typeclasses-102.html
---

  [tt]: http://learnyouahaskell.com/types-and-typeclasses
  [moott]: http://learnyouahaskell.com/making-our-own-types-and-typeclasses

### typeclasses 102

I am now going to skip over to Chapter 8 [Making Our Own Types and Typeclasses][moott] (Chapter 7 if you have the book) since the chapters in between are mostly about Haskell syntax.

### A traffic light datatype

```haskell
data TrafficLight = Red | Yellow | Green
```

In Scala this would be:

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

Now let's define an instance for `Eq`.

```console
scala> implicit val trafficLightEq: Eq[TrafficLight] =
  new Eq[TrafficLight] {
    def eqv(a1: TrafficLight, a2: TrafficLight): Boolean = a1 == a2
  }
```

**Note**: The latest `algebra.Equal` includes `Equal.instance` and `Equal.fromUniversalEquals`.

Can I use the `Eq`?

```console
scala> import cats.syntax.eq._
scala> TrafficLight.Red === TrafficLight.Yellow
```

So apparently `Eq[TrafficLight]` doesn't get picked up because `Eq` has nonvariant subtyping: `Eq[A]`.
One way to workaround this issue is to define helper functions to cast them up to `TrafficLight`:

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

It is a bit of boilerplate, but it works.
