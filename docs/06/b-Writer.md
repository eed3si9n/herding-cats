---
out: Writer.html
---

  [fafmm]: http://learnyouahaskell.com/for-a-few-monads-more
  [DataPackageSource]: $catsBaseUrl$/core/src/main/scala/cats/data/package.scala
  [WriterTSource]: $catsBaseUrl$/core/src/main/scala/cats/data/WriterT.scala
  [performance-characteristics]: http://docs.scala-lang.org/overviews/collections/performance-characteristics.html

### Writer datatype

[Learn You a Haskell for Great Good][fafmm] says:

> Whereas the `Maybe` monad is for values with an added context of failure, and the list monad is for nondeterministic values,
> `Writer` monad is for values that have another value attached that acts as a sort of log value.

Let's follow the book and implement `applyLog` function:

```console:new
scala> def isBigGang(x: Int): (Boolean, String) =
         (x > 9, "Compared gang size to 9.")
scala> implicit class PairOps[A](pair: (A, String)) {
         def applyLog[B](f: A => (B, String)): (B, String) = {
           val (x, log) = pair
           val (y, newlog) = f(x)
           (y, log ++ newlog)
         }
       }
scala> (3, "Smallish gang.") applyLog isBigGang
```


Since method injection is a common use case for implicits, Scala 2.10 adds a syntax sugar called implicit class to make the promotion from a class to an enriched class easier.
Here's how we can generalize the log to a `Semigroup`:

```console
scala> import cats._, cats.data._, cats.implicits._
scala> implicit class PairOps[A, B: Semigroup](pair: (A, B)) {
         def applyLog[C](f: A => (C, B)): (C, B) = {
           val (x, log) = pair
           val (y, newlog) = f(x)
           (y, log |+| newlog)
         }
       }
```

### Writer

LYAHFGG:

> To attach a monoid to a value, we just need to put them together in a tuple. The `Writer w a` type is just a `newtype` wrapper for this.

In Cats, the equivalent is called [`Writer`][DataPackageSource]:

```scala
type Writer[L, V] = WriterT[Id, L, V]
object Writer {
  def apply[L, V](l: L, v: V): WriterT[Id, L, V] = WriterT[Id, L, V]((l, v))

  def value[L:Monoid, V](v: V): Writer[L, V] = WriterT.value(v)

  def tell[L](l: L): Writer[L, Unit] = WriterT.tell(l)
}
```

`Writer[L, V]` is a type alias for `WriterT[Id, L, V]`

### WriterT

Here's the simplified version of [`WriterT`][WriterTSource]:

```scala
final case class WriterT[F[_], L, V](run: F[(L, V)]) {
  def tell(l: L)(implicit functorF: Functor[F], semigroupL: Semigroup[L]): WriterT[F, L, V] =
    mapWritten(_ |+| l)

  def written(implicit functorF: Functor[F]): F[L] =
    functorF.map(run)(_._1)

  def value(implicit functorF: Functor[F]): F[V] =
    functorF.map(run)(_._2)

  def mapBoth[M, U](f: (L, V) => (M, U))(implicit functorF: Functor[F]): WriterT[F, M, U] =
    WriterT { functorF.map(run)(f.tupled) }

  def mapWritten[M](f: L => M)(implicit functorF: Functor[F]): WriterT[F, M, V] =
    mapBoth((l, v) => (f(l), v))
}
```

Here's how we can create `Writer` values:

```console
scala> val w = Writer("Smallish gang.", 3)
scala> val v = Writer.value[String, Int](3)
scala> val l = Writer.tell[String]("Log something")
```

To run the `Writer` datatype you can call its `run` method:

```console
scala> w.run
```

### Using for syntax with Writer

LYAHFGG:

> Now that we have a `Monad` instance, we're free to use `do` notation for `Writer` values.

```console
scala> def logNumber(x: Int): Writer[List[String], Int] =
         Writer(List("Got number: " + x.show), 3)
scala> def multWithLog: Writer[List[String], Int] =
         for {
           a <- logNumber(3)
           b <- logNumber(5)
         } yield a * b
scala> multWithLog.run
```

### Adding logging to program

Here's the `gcd` example:

```console
scala> :paste
def gcd(a: Int, b: Int): Writer[List[String], Int] = {
  if (b == 0) for {
      _ <- Writer.tell(List("Finished with " + a.show))
    } yield a
  else
    Writer.tell(List(s"\${a.show} mod \${b.show} = \${(a % b).show}")) >>= { _ =>
      gcd(b, a % b)
    }
}
scala> gcd(12, 16).run
```

### Inefficient List construction

LYAHFGG:

> When using the `Writer` monad, you have to be careful which monoid to use, because using lists can sometimes
> turn out to be very slow. That's because lists use `++` for `mappend` and using `++` to add something to
> the end of a list is slow if that list is really long.


Here's [the table of performance characteristics for major collections][performance-characteristics]. What stands out for immutable collection is `Vector` since it has effective constant for all operations. `Vector` is a tree structure with the branching factor of 32, and it's able to achieve fast updates by structure sharing.

Here's the Vector version of `gcd`:

```console
scala> :paste
def gcd(a: Int, b: Int): Writer[Vector[String], Int] = {
  if (b == 0) for {
      _ <- Writer.tell(Vector("Finished with " + a.show))
    } yield a
  else
    Writer.tell(Vector(s"\${a.show} mod \${b.show} = \${(a % b).show}")) >>= { _ =>
      gcd(b, a % b)
    }
}
scala> gcd(12, 16).run
```

### Comparing performance

Like the book let's write a microbenchmark to compare the performance:

```console
scala> :paste
def vectorFinalCountDown(x: Int): Writer[Vector[String], Unit] = {
  import annotation.tailrec
  @tailrec def doFinalCountDown(x: Int, w: Writer[Vector[String], Unit]): Writer[Vector[String], Unit] = x match {
    case 0 => w >>= { _ => Writer.tell(Vector("0")) }
    case x => doFinalCountDown(x - 1, w >>= { _ =>
      Writer.tell(Vector(x.show))
    })
  }
  val t0 = System.currentTimeMillis
  val r = doFinalCountDown(x, Writer.tell(Vector[String]()))
  val t1 = System.currentTimeMillis
  r >>= { _ => Writer.tell(Vector((t1 - t0).show + " msec")) }
}

def listFinalCountDown(x: Int): Writer[List[String], Unit] = {
  import annotation.tailrec
  @tailrec def doFinalCountDown(x: Int, w: Writer[List[String], Unit]): Writer[List[String], Unit] = x match {
    case 0 => w >>= { _ => Writer.tell(List("0")) }
    case x => doFinalCountDown(x - 1, w >>= { _ =>
      Writer.tell(List(x.show))
    })
  }
  val t0 = System.currentTimeMillis
  val r = doFinalCountDown(x, Writer.tell(List[String]()))
  val t1 = System.currentTimeMillis
  r >>= { _ => Writer.tell(List((t1 - t0).show + " msec")) }
}
```

Here's the result I got on my machine:

```scala
scala> vectorFinalCountDown(10000).run._1.last
res17: String = 6 msec

scala> listFinalCountDown(10000).run._1.last
res18: String = 630 msec
```

As you can see, `List` is 100 times slower.
