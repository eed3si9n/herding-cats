---
out: Writer.html
---

  [fafmm]: http://learnyouahaskell.com/for-a-few-monads-more
  [DataPackageSource]: $catsBaseUrl$/core/src/main/scala/cats/data/package.scala
  [WriterTSource]: $catsBaseUrl$/core/src/main/scala/cats/data/WriterT.scala
  [performance-characteristics]: http://scalajp.github.com/scala-collections-doc-ja/collections_40.html

### Writer データ型

[すごいHaskellたのしく学ぼう](http://www.amazon.co.jp/dp/4274068854) 曰く:

> `Maybe` モナドが失敗の可能性という文脈付きの値を表し、リストモナドが非決定性が付いた値を表しているのに対し、`Writer` モナドは、もう1つの値がくっついた値を表し、付加された値はログのように振る舞います。

本に従って `applyLog` 関数を実装してみよう:

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

メソッドの注入が implicit のユースケースとしては多いため、Scala 2.10 に implicit class という糖衣構文が登場して、クラスから強化クラスに昇進させるのが簡単になった。ログを `Semigroup` として一般化する:

```console
scala> import cats._, cats.instances.all._, cats.syntax.semigroup._
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

> 値にモノイドのおまけを付けるには、タプルに入れるだけです。`Writer w a` 型の実体は、そんなタプルの `newtype` ラッパーにすぎず、定義はとてもシンプルです。

Cats でこれに対応するのは [`Writer`][DataPackageSource] だ:

```scala
type Writer[L, V] = WriterT[Id, L, V]
object Writer {
  def apply[L, V](l: L, v: V): WriterT[Id, L, V] = WriterT[Id, L, V]((l, v))

  def value[L:Monoid, V](v: V): Writer[L, V] = WriterT.value(v)

  def tell[L](l: L): Writer[L, Unit] = WriterT.tell(l)
}
```

`Writer[L, V]` は、`WriterT[Id, L, V]` の型エイリアスだ。

### WriterT

以下は [`WriterT`][WriterTSource] を単純化したものだ:

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

`Writer` の値はこのように作る:

```console
scala> import cats.data.Writer
scala> val w = Writer("Smallish gang.", 3)
scala> val v = Writer.value[String, Int](3)
scala> val l = Writer.tell[String]("Log something")
```

`Writer` データ型を実行するには `run` メソッドを呼ぶ:

```console
scala> w.run
```

### Writer に for 構文を使う

LYAHFGG:

> こうして `Monad` インスタンスができたので、`Writer` を `do` 記法で自由に扱えます。

```console
scala> import cats.syntax.show._
scala> def logNumber(x: Int): Writer[List[String], Int] =
         Writer(List("Got number: " + x.show), 3)
scala> def multWithLog: Writer[List[String], Int] =
         for {
           a <- logNumber(3)
           b <- logNumber(5)
         } yield a * b
scala> multWithLog.run
```

### プログラムにログを追加する

以下が例題の `gcd` だ:

```console
scala> import cats.syntax.flatMap._
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

### 非効率な List の構築

LYAHFGG:

> `Writer` モナドを使うときは、使うモナドに気をつけてください。リストを使うととても遅くなる場合があるからです。リストは `mappend` に `++` を使っていますが、`++` を使ってリストの最後にものを追加する操作は、そのリストがとても長いと遅くなってしまいます。


[主なコレクションの性能特性をまとめた表][performance-characteristics]があるので見てみよう。不変コレクションで目立っているのが全ての演算を実質定数でこなす `Vector` だ。`Vector` は分岐度が 32 の木構造で、構造共有を行うことで高速な更新を実現している。

Vector を使った `gcd`:

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

### 性能の比較

本のように性能を比較するマイクロベンチマークを書いてみよう:

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

僕のマシンの実行結果だとこうなった:

```scala
scala> vectorFinalCountDown(10000).run._1.last
res17: String = 6 msec

scala> listFinalCountDown(10000).run._1.last
res18: String = 630 msec
```

`List` が 100倍遅いことが分かる。
