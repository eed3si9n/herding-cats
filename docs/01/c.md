
### Order

LYAHFGG:

> `Ord` is for types that have an ordering. `Ord` covers all the standard comparing functions such as `>`, `<`, `>=` and `<=`.

Cats' equivalent for the `Ord` typeclass is `Order`.

```console:new,error
scala> import cats._, cats.std.all._, cats.syntax.order._
scala> 1 > 2.0
scala> 1 compare 2.0
scala> 1.0 compare 2.0
scala> 1.0 max 2.0
```

`Order` enables `compare` syntax which returns `Int`: negative, zero, or positive.
It also enables `min` and `max` operators.
Similar to `Eq`, comparing `Int` and `Double` fails compilation.
