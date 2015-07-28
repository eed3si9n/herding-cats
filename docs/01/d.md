
### PartialOrder

In addition to `Order`, Cats also defines `PartialOrder`.
This also is a type alias to `algebra.PartialOrder`.

```console:new
scala> import cats._, cats.std.all._, cats.syntax.partialOrder._
scala> 1 tryCompare 2
scala> 1.0 tryCompare Double.NaN
```

`PartialOrder` enables `tryCompare` syntax which returns `Option[Int]`.
According to algebra, it'll return `None` if operands are not comparable.
It's returning `Some(-1)` when comparing `1.0` and `Double.NaN`, so I'm not sure when things are incomparable.

```console:error
scala> def lt[A: PartialOrder](a1: A, a2: A): Boolean = a1 <= a2
scala> lt[Int](1, 2.0)
scala> lt(1, 2)
```

`PartialOrder` also enables `>`, `>=`, `<`, and `<=` operators,
but these are tricky to use because if you're not careful
you could end up using the built-in comparison operators.
