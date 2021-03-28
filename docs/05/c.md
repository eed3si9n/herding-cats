---
out: List.html
---

### List datatype

LYAHFGG:

> On the other hand, a value like `[3,8,9]` contains several results, so we can view it as one value that is actually many values at the same time. Using lists as applicative functors showcases this non-determinism nicely.

Let's look at using `List` as Applicatives again:

```scala mdoc
import cats._, cats.syntax.all._

(List(1, 2, 3), List(10, 100, 100)) mapN { _ * _ }
```

> let's try feeding a non-deterministic value to a function:

```scala mdoc
List(3, 4, 5) >>= { x => List(x, -x) }
```

So in this monadic view, a `List` context represents a mathematical value that could have multiple solutions. Other than that manipulating `List`s using `for` notation is just like plain Scala:

```scala mdoc
for {
  n <- List(1, 2)
  ch <- List('a', 'b')
} yield (n, ch)
```
