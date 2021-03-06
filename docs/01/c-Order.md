
### Order

LYAHFGG:

> `Ord` is for types that have an ordering. `Ord` covers all the standard comparing functions such as `>`, `<`, `>=` and `<=`.

Cats' equivalent for the `Ord` typeclass is `Order`.

```scala mdoc
// plain Scala
1 > 2.0
```

```scala mdoc:fail
import cats._, cats.syntax.all._

1 compare 2.0
```

```scala mdoc
import cats._, cats.syntax.all._

1.0 compare 2.0

1.0 max 2.0
```

`Order` enables `compare` syntax which returns `Int`: negative, zero, or positive.
It also enables `min` and `max` operators.
Similar to `Eq`, comparing `Int` and `Double` fails compilation.
