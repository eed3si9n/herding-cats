---
out: tail-recursive-monads.html
---

  [@runarorama]: https://twitter.com/runarorama
  [@paf31]: https://twitter.com/paf31
  [ssfmvid]: http://skillsmatter.com/podcast/scala/stackless-scala-free-monads
  [ssfmpaper]: http://days2012.scala-lang.org/sites/days2012/files/bjarnason_trampolines.pdf
  [ssff]: http://functorial.com/stack-safety-for-free/index.pdf
  [Writer]: Writer.html
  [@posco]: https://twitter.com/posco
  [1280]: https://github.com/typelevel/cats/pull/1280

### Tail Recursive Monads (FlatMap)

In 2015 Phil Freeman ([@paf31][@paf31]) wrote [Stack Safety for Free][ssff] working with PureScript hosted on JavaScript, a strict language like Java:

<blockquote class="twitter-tweet" data-lang="en"><p lang="en" dir="ltr">I&#39;ve written up some work on stack safe free monad transformers. Feedback would be very much appreciated <a href="http://t.co/1rH7OwaWpy">http://t.co/1rH7OwaWpy</a></p>&mdash; Phil Freeman (@paf31) <a href="https://twitter.com/paf31/status/630148424478781441">August 8, 2015</a></blockquote>
<script async src="//platform.twitter.com/widgets.js" charset="utf-8"></script>

The paper gives a hat tip to Rúnar ([@runarorama][@runarorama])'s [Stackless Scala With Free Monads][ssfmpaper],
but presents a more drastic solution to the stack safety problem.

#### The stack problem

As a background, in Scala the compiler is able to optimize on self-recursive tail calls.
For example, here's an example of a self-recursive tail calls.

```console
scala> import scala.annotation.tailrec
scala> :paste
def pow(n: Long, exp: Long): Long =
  {
    @tailrec def go(acc: Long, p: Long): Long =
      (acc, p) match {
        case (acc, 0) => acc
        case (acc, p) => go(acc * n, p - 1)
      }
    go(1, exp)
  }
scala> pow(2, 3)
```

Here's an example that's not self-recursive. It's blowing up the stack.

```scala
scala> :paste
object OddEven0 {
  def odd(n: Int): String = even(n - 1)
  def even(n: Int): String = if (n <= 0) "done" else odd(n - 1)
}

// Exiting paste mode, now interpreting.

defined object OddEven0

scala> OddEven0.even(200000)
java.lang.StackOverflowError
  at OddEven0\$.even(<console>:15)
  at OddEven0\$.odd(<console>:14)
  at OddEven0\$.even(<console>:15)
  at OddEven0\$.odd(<console>:14)
  at OddEven0\$.even(<console>:15)
  ....
```

Next, we'd try to add [Writer][Writer] datatype to do the `pow` calculation using `LongProduct` monoid.

```console
scala> import cats._, cats.data._, cats.implicits._
scala> :paste
case class LongProduct(value: Long)
implicit val longProdMonoid: Monoid[LongProduct] = new Monoid[LongProduct] {
  def empty: LongProduct = LongProduct(1)
  def combine(x: LongProduct, y: LongProduct): LongProduct = LongProduct(x.value * y.value)
}
def powWriter(x: Long, exp: Long): Writer[LongProduct, Unit] =
  exp match {
    case 0 => Writer(LongProduct(1L), ())
    case m =>
      Writer(LongProduct(x), ()) >>= { _ => powWriter(x, exp - 1) }
  }
scala> powWriter(2, 3).run
```

This is no longer self-recursive, so it will blow the stack with large `exp`.

```scala
scala> powWriter(1, 10000).run
java.lang.StackOverflowError
  at \$anonfun\$powWriter\$1.apply(<console>:35)
  at \$anonfun\$powWriter\$1.apply(<console>:35)
  at cats.data.WriterT\$\$anonfun\$flatMap\$1.apply(WriterT.scala:37)
  at cats.data.WriterT\$\$anonfun\$flatMap\$1.apply(WriterT.scala:36)
  at cats.package\$\$anon\$1.flatMap(package.scala:34)
  at cats.data.WriterT.flatMap(WriterT.scala:36)
  at cats.data.WriterTFlatMap1\$class.flatMap(WriterT.scala:249)
  at cats.data.WriterTInstances2\$\$anon\$4.flatMap(WriterT.scala:130)
  at cats.data.WriterTInstances2\$\$anon\$4.flatMap(WriterT.scala:130)
  at cats.FlatMap\$class.\$greater\$greater\$eq(FlatMap.scala:26)
  at cats.data.WriterTInstances2\$\$anon\$4.\$greater\$greater\$eq(WriterT.scala:130)
  at cats.FlatMap\$Ops\$class.\$greater\$greater\$eq(FlatMap.scala:20)
  at cats.syntax.FlatMapSyntax1\$\$anon\$1.\$greater\$greater\$eq(flatMap.scala:6)
  at .powWriter1(<console>:35)
  at \$anonfun\$powWriter\$1.apply(<console>:35)
```

This characteristic of Scala limits the usefulness of monadic composition where `flatMap` can call
monadic function `f`, which then can call `flatMap` etc..

#### FlatMap (MonadRec)

> Our solution is to reduce the candidates for the target monad `m` from an arbitrary
> monad, to the class of so-called tail-recursive monads.

```haskell
class (Monad m) <= MonadRec m where
  tailRecM :: forall a b. (a -> m (Either a b)) -> a -> m b
```

Here's the same function in Scala:

```scala
  /**
   * Keeps calling `f` until a `scala.util.Right[B]` is returned.
   */
  def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B]
```

As it turns out, Oscar Boykin ([@posco][@posco]) brought `tailRecM` into `FlatMap` in [#1280][1280]
(Remove FlatMapRec make all FlatMap implement tailRecM), and it's now part of Cats 0.7.0.
In other words, all FlatMap/Monads in Cats 0.7.0 are tail-recursive.

We can for example, obtain the `tailRecM` for `Writer`:

```console
scala> def tailRecM[A, B] = FlatMap[Writer[Vector[String], ?]].tailRecM[A, B] _
```

Here's how we can make a stack-safe `powWriter`:

```console
scala> :paste
def powWriter2(x: Long, exp: Long): Writer[LongProduct, Unit] =
  FlatMap[Writer[LongProduct, ?]].tailRecM(exp) {
    case 0L      => Writer.value[LongProduct, Either[Long, Unit]](Right(()))
    case m: Long => Writer.tell(LongProduct(x)) >>= { _ => Writer.value(Left(m - 1)) }
  }
scala> powWriter2(2, 3).run
scala> powWriter2(1, 10000).run
```

This guarantees greater safety on the user of `FlatMap` typeclass, but it would mean that
each the implementers of the instances would need to provide a safe `tailRecM`.

Here's the one for `Option` for example:

```scala
@tailrec
def tailRecM[A, B](a: A)(f: A => Option[Either[A, B]]): Option[B] =
  f(a) match {
    case None => None
    case Some(Left(a1)) => tailRecM(a1)(f)
    case Some(Right(b)) => Some(b)
  }
```

That's it for today!
