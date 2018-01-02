
  [FunctorEmptySource]: https://github.com/typelevel/cats-mtl/blob/v0.2.2/core/src/main/scala/cats/mtl/FunctorEmpty.scala

### FunctorEmpty

Scala's `for` comprehension allows filtering:

```console:new
scala> import cats._, cats.data._, cats.implicits._
scala> for {
         x <- (1 to 50).toList if x.show contains '7'
       } yield x
```

Here's [the typeclass contract for `FunctorEmpty`][FunctorEmptySource] in cats-mtl module:

```scala
trait FunctorEmpty[F[_]] extends Serializable {
  val functor: Functor[F]

  def mapFilter[A, B](fa: F[A])(f: A => Option[B]): F[B]

  def collect[A, B](fa: F[A])(f: PartialFunction[A, B]): F[B]

  def flattenOption[A](fa: F[Option[A]]): F[A]

  def filter[A](fa: F[A])(f: A => Boolean): F[A]
}
```

We can use this like this:

```console
scala> import cats.mtl._, cats.mtl.implicits._
scala> val english = Map(1 -> "one", 3 -> "three", 10 -> "ten")
scala> (1 to 50).toList mapFilter { english.get(_) }
scala> def collectEnglish[F[_]: FunctorEmpty](f: F[Int]): F[String] =
         f collect {
           case 1  => "one"
           case 3  => "three"
           case 10 => "ten"
         }
scala> collectEnglish((1 to 50).toList)
scala> def filterSeven[F[_]: FunctorEmpty](f: F[Int]): F[Int] =
         f filter { _.show contains '7' }
scala> filterSeven((1 to 50).toList)
```
