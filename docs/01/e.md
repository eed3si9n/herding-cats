
### `Show`

LYAHFGG:

> Members of `Show` can be presented as strings.

Cats' equivalent for the `Show` typeclass is `Show`:

```console:new
scala> import cats._, cats.std.all._, cats.syntax.show._
scala> 3.show
scala> "hello".show
```

At first, it might seem silly to define `Show` because Scala
already has `toString` on `Any`.
`Any` also means anything would match the criteria, so you lose type safety.
The `toString` could be junk supplied by some parent class:

```console
scala> (new {}).toString
scala> (new {}).show
```
