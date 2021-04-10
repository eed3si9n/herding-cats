
### PartialOrder

In addition to `Order`, Cats also defines `PartialOrder`.

```scala mdoc
import cats._, cats.syntax.all._

1 tryCompare 2

1.0 tryCompare Double.NaN
```

`PartialOrder` enables `tryCompare` syntax which returns `Option[Int]`.
According to algebra, it'll return `None` if operands are not comparable.
It's returning `Some(-1)` when comparing `1.0` and `Double.NaN`, so I'm not sure when things are incomparable.

```scala mdoc
def lt[A: PartialOrder](a1: A, a2: A): Boolean = a1 <= a2
lt(1, 2)
```

```scala mdoc:fail
lt[Int](1, 2.0)
```

`PartialOrder` also enables `>`, `>=`, `<`, and `<=` operators,
but these are tricky to use because if you're not careful
you could end up using the built-in comparison operators.
