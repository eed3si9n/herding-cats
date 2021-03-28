
  [FunctorFilterSource]: $catsBaseUrl$/core/src/main/scala/cats/FunctorFilter.scala

### FunctorFilter

Scala の `for` 内包表記はフィルタリングができる:

```scala mdoc
// plain Scala

for {
  x <- (1 to 50).toList if x.toString contains '7'
} yield x
```

[FunctorFilter 型クラスのコントラクト][FunctorFilterSource]だ:


```scala
@typeclass
trait FunctorFilter[F[_]] extends Serializable {
  def functor: Functor[F]

  def mapFilter[A, B](fa: F[A])(f: A => Option[B]): F[B]

  def collect[A, B](fa: F[A])(f: PartialFunction[A, B]): F[B] =
    mapFilter(fa)(f.lift)

  def flattenOption[A](fa: F[Option[A]]): F[A] =
    mapFilter(fa)(identity)

  def filter[A](fa: F[A])(f: A => Boolean): F[A] =
    mapFilter(fa)(a => if (f(a)) Some(a) else None)

  def filterNot[A](fa: F[A])(f: A => Boolean): F[A] =
    mapFilter(fa)(Some(_).filterNot(f))
}
```

このように使うことができる:

```scala mdoc
import cats._, cats.syntax.all._

val english = Map(1 -> "one", 3 -> "three", 10 -> "ten")

(1 to 50).toList mapFilter { english.get(_) }

def collectEnglish[F[_]: FunctorFilter](f: F[Int]): F[String] =
  f collect {
    case 1  => "one"
    case 3  => "three"
    case 10 => "ten"
  }

collectEnglish((1 to 50).toList)

def filterSeven[F[_]: FunctorFilter](f: F[Int]): F[Int] =
  f filter { _.show contains '7' }

filterSeven((1 to 50).toList)
```
