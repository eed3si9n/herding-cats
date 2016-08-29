---
out: composing-monadic-functions.html
---

  [KleisliSource]: $catsBaseUrl$/core/src/main/scala/cats/data/Kleisli.scala
  [354]: https://github.com/typelevel/cats/pull/354

### モナディック関数の合成

LYAHFGG:

> 第13章でモナド則を紹介したとき、`<=<` 関数は関数合成によく似ているど、普通の関数 `a -> b` ではなくて、`a -> m b` みたいなモナディック関数に作用するのだよと言いました。

Cats には [Kleisli][KleisliSource] と呼ばれる `A => M[B]` という型の関数に対する特殊なラッパーがある:

```scala
/**
 * Represents a function `A => F[B]`.
 */
final case class Kleisli[F[_], A, B](run: A => F[B]) { self =>

  ....
}

object Kleisli extends KleisliInstances with KleisliFunctions

private[data] sealed trait KleisliFunctions {

  def pure[F[_], A, B](x: B)(implicit F: Applicative[F]): Kleisli[F, A, B] =
    Kleisli(_ => F.pure(x))

  def ask[F[_], A](implicit F: Applicative[F]): Kleisli[F, A, A] =
    Kleisli(F.pure)

  def local[M[_], A, R](f: R => R)(fa: Kleisli[M, R, A]): Kleisli[M, R, A] =
    Kleisli(f andThen fa.run)
}
```

`Kleisli()` コンストラクタを使って `Kliesli` 値を構築する:

```console:new
scala> :paste
object Catnip {
  implicit class IdOp[A](val a: A) extends AnyVal {
    def some: Option[A] = Some(a)
  }
  def none[A]: Option[A] = None
}
import Catnip._
scala> import cats._, cats.instances.all._
scala> import cats.data.Kleisli
scala> val f = Kleisli { (x: Int) => (x + 1).some }
scala> val g = Kleisli { (x: Int) => (x * 100).some }
```

`compose` を使って関数を合成すると、右辺項が先に適用される。

```console
scala> import cats.syntax.flatMap._
scala> 4.some >>= (f compose g).run
```

`andThen` を使うと、左辺項が先に適用される:

```console
scala> 4.some >>= (f andThen g).run
```

`compose` と `andThen` は関数の合成同様に動作するが、モナディックなコンテキストを保持するのが違いだ。

#### lift メソッド

Kleisli には、モナディック関数を別のアプリカティブ・ファンクターに持ち上げる `lift` のような面白いメソッドがいくつかある。
と思って使ってみたけども、壊れている事に気付いたので、これが修正版だ [#354][354]:

```scala
  def lift[G[_]](implicit G: Applicative[G]): Kleisli[λ[α => G[F[α]]], A, B] =
    Kleisli[λ[α => G[F[α]]], A, B](a => Applicative[G].pure(run(a)))
```

使ってみる:

```scala
scala> val l = f.lift[List]
l: cats.data.Kleisli[[α]List[Option[α]],Int,Int] = Kleisli(<function1>)

scala> List(1, 2, 3) >>= l.run
res0: List[Option[Int]] = List(Some(2), Some(3), Some(4))
```
