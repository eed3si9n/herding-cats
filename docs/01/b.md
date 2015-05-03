
  [algebra]: https://github.com/non/algebra

## Eq

LYAHFGG:

> `Eq` is used for types that support equality testing. The functions its members implement are `==` and `/=`.

Cats equivalent for the `Eq` typeclass is also called `Eq`.
Technically speaking, `cats.Eq` is actually a type alias of `algebra.Eq` from [non/algebra][algebra].
Not sure if it matters, but it's probably a good idea that it's being reused:

```console
scala> import cats._, cats.std.all._, cats.syntax.eq._
scala> 1 === 1
scala> 1 === "foo"
scala> 1 == "foo"
scala> (Some(1): Option[Int]) =!= (Some(2): Option[Int])
```

Instead of the standard `==`, `Eq` enables `===` and `=!=` syntax by declaring `eqv` method. The main difference is that `===` would fail compilation if you tried to compare `Int` and `String`.
