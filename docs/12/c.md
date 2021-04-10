---
out: partial-unification.html
---

  [SI-2712]: https://issues.scala-lang.org/browse/SI-2712
  [cecc3a]: https://github.com/stew/cats/commit/cecc3afbdbb6fbbe764005cd52e9efe7acdfc8f2
  [combining-applicative]: combining-applicative.html

### Coercing type inference using partial unification

EIP:

> We will have a number of new datatypes with coercion functions like `Id`, `unId`, `Const` and `unConst`.
> To reduce clutter, we introduce a common notation for such coercions.

In Scala, implicits and type inference take us pretty far.
But by dealing with typeclass a lot, we also end up seeing some of the weaknesses of Scala's type inference.
One of the issues we come across frequently is its inability to infer partially applied parameterized type,
also known as [SI-2712][SI-2712].

<s>If you're reading this, please jump to the page, upvote the bug, or help solving the problem.</s>

This was fixed by Miles Sabin in [scala#5102](https://github.com/scala/scala/pull/5102) as "-Ypartial-unification" flag. See also [Explaining Miles's Magic](https://gist.github.com/djspiewak/7a81a395c461fd3a09a6941d4cd040f2).

Here's an example that Daniel uses:

```scala mdoc
def foo[F[_], A](fa: F[A]): String = fa.toString

foo { x: Int => x * 2 }
```

The above did not compile before.

> The reason it does not compile is because `Function1` takes two type parameters, whereas `F[_]` only takes one.

With `-Ypartial-unification` it will now compile, but it's important to understand that the compiler will now assume that the type constructors can be partially applied from left to right. In short, this will reward right-biased datatypes like `Either`, but you could end up with wrong answer if the datatype is left-biased.

In 2019, Scala 2.13.0 was released with partial unification enabled by default.
