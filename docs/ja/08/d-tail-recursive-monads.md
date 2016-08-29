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

### 末尾再帰モナド (FlatMap)

2015年に PureScript でのスタック安全性の取り扱いに関して Phil Freeman ([@paf31][@paf31]) さんは [Stack Safety for Free][ssff] を書いた。
PureScript は Java 同様に正格 (strict) な JavaScript にホストされている言語だ:

<blockquote class="twitter-tweet" data-lang="en"><p lang="en" dir="ltr">I&#39;ve written up some work on stack safe free monad transformers. Feedback would be very much appreciated <a href="http://t.co/1rH7OwaWpy">http://t.co/1rH7OwaWpy</a></p>&mdash; Phil Freeman (@paf31) <a href="https://twitter.com/paf31/status/630148424478781441">August 8, 2015</a></blockquote>
<script async src="//platform.twitter.com/widgets.js" charset="utf-8"></script>

この論文は Rúnar ([@runarorama][@runarorama]) さんの [Stackless Scala With Free Monads][ssfmpaper] にも言及するが、スタック安全性に関してより抜本的な解法を提示している。

#### スタック問題とは

問題の背景として、Scala ではコンパイラが自己再帰の末尾再帰呼び出しは最適化することが可能だ。

例えば、これは自己再帰の末尾再帰呼び出しの例だ。

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

自己再帰じゃない例。スタックオーバーフローを起こしている。

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

次に、`pow` に [Writer][Writer] データ型を追加して、`LongProduct` モノイドを使って計算をさせてみたい。

```console
scala> import cats._, cats.instances.all._, cats.data.Writer
scala> import cats.syntax.flatMap._
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

自己再帰じゃなくなったので、`exp` の値が大きいとスタックオーバーフローするようになってしまった。

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

この Scala の特性は `flatMap` がモナディック関数を呼び出して、さらにそれが `flatMap`
を呼び出すといった形のモナディック合成の便利さを制限するものだ。

#### FlatMap (MonadRec)

> 我々がとった対策方法はモナド `m` の対象を任意のモナドから、いわゆる末尾再帰モナドとよばれる型クラスに候補を狭めたことだ。

```haskell
class (Monad m) <= MonadRec m where
  tailRecM :: forall a b. (a -> m (Either a b)) -> a -> m b
```

Scala で同じ関数を書くとこうなる:

```scala
  /**
   * Keeps calling `f` until a `scala.util.Right[B]` is returned.
   */
  def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B]
```

実は Oscar Boykin ([@posco][@posco]) さんが [#1280][1280] (Remove FlatMapRec make all FlatMap implement tailRecM)
において、この `tailRecM` を `FlatMap` に持ち込んでいて、Cats 0.7.0 の一部となっている。
つまり、Cats 0.7.0 以降の FlatMap/Monad は末尾再帰であると言うことができる。

例えば、`Writer` の `tailRecM` を以下のようにして取得できる:

```console
scala> def tailRecM[A, B] = FlatMap[Writer[Vector[String], ?]].tailRecM[A, B] _
```

スタックセーフな `powWriter` はこう書くことができる:

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

これは `FlatMap` 型クラスのユーザにとってはより大きな安全性を保証するものだが、
インスタンスの実装する者は安全な `tailRecM` を提供しなければいけないことも意味している。

例えば `Option` の実装はこんな感じだ:

```scala
@tailrec
def tailRecM[A, B](a: A)(f: A => Option[Either[A, B]]): Option[B] =
  f(a) match {
    case None => None
    case Some(Left(a1)) => tailRecM(a1)(f)
    case Some(Right(b)) => Some(b)
  }
```

今日はここまで。
