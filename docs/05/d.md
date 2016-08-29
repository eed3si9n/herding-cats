
  [FunctorFilterSource]: $catsBaseUrl$/core/src/main/scala/cats/FunctorFilter.scala

### FunctorFilter

Scala's `for` comprehension allows filtering:

```console:new
scala> import cats._, cats.instances.all._, cats.syntax.show._
scala> for {
         x <- (1 to 50).toList if x.show contains '7'
       } yield x
```

Here's [the typeclass contract for `FunctorFilter`][FunctorFilterSource]:

```scala
@typeclass trait FunctorFilter[F[_]] extends Functor[F] {
  /**
   * A combined [[map]] and [[filter]]. Filtering is handled via `Option`
   * instead of `Boolean` such that the output type `B` can be different than
   * the input type `A`.
   * ....
   *
   **/
  def mapFilter[A, B](fa: F[A])(f: A => Option[B]): F[B]
}
```

We can use this like this:

```console
scala> import cats.syntax.functorFilter._
scala> val english = Map(1 -> "one", 3 -> "three", 10 -> "ten")
scala> (1 to 50).toList mapFilter { english.get(_) }
```

This enables the derivative functions/operators `collect`, `flattenOption`, and `filter`:

```scala
@typeclass trait FunctorFilter[F[_]] extends Functor[F] {

  def mapFilter[A, B](fa: F[A])(f: A => Option[B]): F[B]

  /**
   * Similar to [[mapFilter]] but uses a partial function instead of a function
   * that returns an `Option`.
   */
  def collect[A, B](fa: F[A])(f: PartialFunction[A, B]): F[B] =
    mapFilter(fa)(f.lift)

  /**
   * "Flatten" out a structure by collapsing `Option`s.
   */
  def flattenOption[A](fa: F[Option[A]]): F[A] = mapFilter(fa)(identity)

  /**
   * Apply a filter to a structure such that the output structure contains all
   * `A` elements in the input structure that satisfy the predicate `f` but none
   * that don't.
   */
  def filter[A](fa: F[A])(f: A => Boolean): F[A] =
    mapFilter(fa)(a => if (f(a)) Some(a) else None)
}
```

We can use this like this:

```console
scala> def collectEnglish[F[_]: FunctorFilter](f: F[Int]): F[String] =
         f collect {
           case 1  => "one"
           case 3  => "three"
           case 10 => "ten"
         }
scala> collectEnglish((1 to 50).toList)
scala> def filterSeven[F[_]: FunctorFilter](f: F[Int]): F[Int] =
         f filter { _.show contains '7' }
scala> filterSeven((1 to 50).toList)
```
