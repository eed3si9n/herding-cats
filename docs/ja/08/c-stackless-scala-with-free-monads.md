---
out: stackless-scala-with-free-monads.html
---

  [@runarorama]: https://twitter.com/runarorama
  [dsdi]: http://functionaltalks.org/2013/06/17/runar-oli-bjarnason-dead-simple-dependency-injection/
  [ssfmvid]: http://skillsmatter.com/podcast/scala/stackless-scala-free-monads
  [ssfmpaper]: http://days2012.scala-lang.org/sites/days2012/files/bjarnason_trampolines.pdf
  [322]: https://github.com/typelevel/cats/pull/322

### Stackless Scala with Free Monads

自由モナドの概念は interpreter パターンを超えたものだ。
恐らくこれからも新しい自由モナドの応用範囲が見つかっていくと思う。

Rúnar ([@runarorama][@runarorama]) さんは Scala で `Free` を使うことを広めた第一人者だ。
6日目に扱った [Dead-Simple Dependency Injection][dsdi] というトークでは
key-value ストアを実装するためのミニ言語を `Free` を用いて実装していた。
同年の Scala Days 2012 では Rúnar さんは
[Stackless Scala With Free Monads][ssfmvid] というトークをやっている。
ペーパーを読む前にトークを観ておくことをお勧めするけど、ペーパーの方が引用しやすいので
[Stackless Scala With Free Monads][ssfmpaper] もリンクしておく。

Rúnar さんはまず State モナドの実装を使ってリストに添字を zip するコードから始める。
これはリストがスタックの限界よりも大きいと、スタックを吹っ飛ばす。
続いてプログラム全体を一つのループで回すトランポリンというものを紹介している。

```scala
sealed trait Trampoline [+ A] {
  final def runT : A =
    this match {
      case More (k) => k().runT
      case Done (v) => v
    }
}
case class More[+A](k: () => Trampoline[A])
  extends Trampoline[A]
case class Done [+A](result: A)
  extends Trampoline [A]
```

上記のコードでは `Function0` の `k` は次のステップのための thunk となっている。

これを State モナドを使った使用例に拡張するため、`flatMap` を `FlatMap` というデータ構造に具現化している:

```scala
case class FlatMap [A,+B](
  sub: Trampoline [A],
  k: A => Trampoline[B]) extends Trampoline[B]
```

続いて、`Trampoline` は実は `Function0` の Free モナドであることが明かされる。
Cats では以下のように定義されている:

```scala
  type Trampoline[A] = Free[Function0, A]
```

#### トランポリン

トランポリンを使えば、どんなプログラムでもスタックを使わないものに変換することができる。
`Trampoine` object はトランポリン化するのに役立つ関数を定義する:

```scala
object Trampoline {
  def done[A](a: A): Trampoline[A] =
    Free.Pure[Function0,A](a)

  def suspend[A](a: => Trampoline[A]): Trampoline[A] =
    Free.Suspend[Function0, A](() => a)

  def delay[A](a: => A): Trampoline[A] =
    suspend(done(a))
}
```

トークに出てきた `even` と `odd` を実装してみよう:

```console:new
scala> import cats._, cats.data._, cats.implicits._, cats.free.{ Free, Trampoline }
scala> import Trampoline._
scala> :paste
def even[A](ns: List[A]): Trampoline[Boolean] =
  ns match {
    case Nil => done(true)
    case x :: xs => suspend(odd(xs))
  }
def odd[A](ns: List[A]): Trampoline[Boolean] =
  ns match {
    case Nil => done(false)
    case x :: xs => suspend(even(xs))
  }
scala> even(List(1, 2, 3)).run
scala> even((0 to 3000).toList).run
```

上を実装してるうちにまた SI-7139 に引っかかったので、Cats を少し改良する必要があった。 [#322][322]

#### 自由モナド

さらに Rúnar さんは便利な Free モナドを作れるいくつかのデータ型を紹介する:

```scala
type Pair[+A] = (A, A)
type BinTree[+A] = Free[Pair, A]

type Tree[+A] = Free[List, A]

type FreeMonoid[+A] = Free[({type λ[+α] = (A,α)})#λ, Unit]

type Trivial[+A] = Unit
type Option[+A] = Free[Trivial, A]
```

モナドを使った Iteratee まであるみたいだ。最後に自由モナドを以下のようにまとめている:

> - データが末端に来る全ての再帰データ型に使えるモデル
> - 自由モナドは変数が末端にある式木で、flatMap は変数の置換にあたる。

#### Free を用いた自由モノイド

`Free` を使って「リスト」を定義してみよう。

```console
scala> type FreeMonoid[A] = Free[(A, +?), Unit]
scala> def cons[A](a: A): FreeMonoid[A] =
         Free.liftF[(A, +?), Unit]((a, ()))
scala> val x = cons(1)
scala> val xs = cons(1) flatMap {_ => cons(2)}
```

この結果を処理する一例として標準の `List` に変換してみる:

```console
scala> implicit def tuple2Functor[A]: Functor[(A, ?)] =
         new Functor[(A, ?)] {
           def map[B, C](fa: (A, B))(f: B => C) =
             (fa._1, f(fa._2))
         }
scala> def toList[A](list: FreeMonoid[A]): List[A] =
         list.fold(
           { _ => Nil },
           { case (x: A @unchecked, xs: FreeMonoid[A]) => x :: toList(xs) })
scala> toList(xs)
```
