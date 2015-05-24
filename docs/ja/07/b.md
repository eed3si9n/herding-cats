---
out: Xor.html
---

  [XorSource]: $catsBaseUrl$core/src/main/scala/cats/data/Xor.scala

### Xor データ型

LYAHFGG:

> `Either e a` 型も失敗の文脈を与えるモナドです。しかも、失敗に値を付加できるので、何が失敗したかを説明したり、そのほか失敗にまつわる有用な情報を提供できます。

標準ライブラリの `Either[A, B]` は知ってるし、Cats が `Either` の右バイアスのファンクターを実装するという話も何回か出てきた。
Cats はさらに `Either` 同様のデータ型である [Xor][XorSource] も実装する:

```scala
/** Represents a right-biased disjunction that is either an `A` or a `B`.
 *
 * An instance of `A [[Xor]] B` is either a `[[Xor.Left Left]][A]` or a `[[Xor.Right Right]][B]`.
 *
 * A common use of [[Xor]] is to explicitly represent the possibility of failure in a result as opposed to
 * throwing an exception.  By convention, [[Xor.Left Left]] is used for errors and [[Xor.Right Right]] is reserved for successes.
 * For example, a function that attempts to parse an integer from a string may have a return type of
 * `NumberFormatException [[Xor]] Int`. However, since there is no need to actually throw an exception, the type (`A`)
 * chosen for the "left" could be any type representing an error and has no need to actually extend `Exception`.
 *
 * `A [[Xor]] B` is isomorphic to `scala.Either[A, B]`, but [[Xor]] is right-biased, so methods such as `map` and
 * `flatMap` apply only in the context of the "right" case. This right bias makes [[Xor]] more convenient to use
 * than `scala.Either` in a monadic context. Methods such as `swap`, and `leftMap` provide functionality
 * that `scala.Either` exposes through left projections.
 */
sealed abstract class Xor[+A, +B] extends Product with Serializable {

  def fold[C](fa: A => C, fb: B => C): C = this match {
    case Xor.Left(a) => fa(a)
    case Xor.Right(b) => fb(b)
  }

  def isLeft: Boolean = fold(_ => true, _ => false)

  def isRight: Boolean = fold(_ => false, _ => true)

  def swap: B Xor A = fold(Xor.right, Xor.left)

  ....
}

object Xor extends XorInstances with XorFunctions {
  final case class Left[+A](a: A) extends (A Xor Nothing)
  final case class Right[+B](b: B) extends (Nothing Xor B)
}
```

これらの値は `Xor` の `right` と `left` メソッドを使って作られる:

```console:new
scala> import cats._, cats.data.Xor, cats.std.all._
scala> Xor.right[String, Int](1)
scala> Xor.left[String, Int]("error")
```

標準ライブラリの `Either[A, B]` と違って、
Cats の `Xor` はだいたいにおいて右投射が欲しいだろうと決めてかかってくれる:

```console
scala> import cats.syntax.flatMap._
scala> Xor.left[String, Int]("boom") >>=
         { x => Xor.right[String, Int](x + 1) }
```

`for` 内包表記からも使ってみよう:

```console
scala> import cats.syntax.semigroup._
scala> for {
         e1 <- Xor.right[String, String]("event 1 ok")
         e2 <- Xor.left[String, String]("event 2 failed!")
         e3 <- Xor.left[String, String]("event 3 failed!")
       } yield (e1 |+| e2 |+| e3)
```

見ての通り、最初の失敗が最終結果に繰り上がった。
`Xor` からどうやって値を取り出せばいいだろう?

まずは `fold`:

```console
scala> val e1 = Xor.right[String, String]("event 1 ok")
scala> e1.fold(
         { l => l },
         { r => r + "!" })
```

`isRight` と `isLeft` でどっち側にいるか確かめるという方法もある:

```console
scala> e1.isRight
scala> e1.isLeft
```

右値なら `getOrElse(x)` で値を抽出: 

```console
scala> e1.getOrElse("something good")
```

左値から値を抽出するには、`swap` を使ってまず右値に変換する:

```console
scala> val e2 = Xor.left[String, String]("event 2 failed!")
scala> e2.swap.getOrElse("something good")
```

期待通り、`map` も右バイアスがかかっている:

```console
scala> e1 map { _ + "!" }
scala> e2 map { _ + "!" }
```

左側で連鎖させるには、`=> AA \/ BB` (ただし `[AA >: A, BB >: B]`) を受け取る `orElse` がある:

```scala
scala> e2 orElse Xor.right[String, String]("event 2 retry ok")
```

この `Xor` データ型には他にも `toEither` などメソッドが用意されている。
