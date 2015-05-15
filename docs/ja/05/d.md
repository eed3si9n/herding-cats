
  [MonadFilterSource]: $catsBaseUrl$/core/src/main/scala/cats/MonadFilter.scala

### MonadFilter

Scala の `for` 内包表記はフィルタリングができる:

```console:new
scala> import cats._, cats.std.all._, cats.syntax.show._
scala> for {
         x <- (1 to 50).toList if x.show contains '7'
       } yield x
```

LYAHFGG:

> `MonadPlus` は、モノイドの性質をあわせ持つモナドを表す型クラスです。

以下が[MonadFilter 型クラスのコントラクト][MonadFilterSource]だ:

```scala
/**
 * a Monad equipped with an additional method which allows us to
 * create an "Empty" value for the Monad (for whatever "empty" makes
 * sense for that particular monad). This is of particular interest to
 * us since it allows us to add a `filter` method to a Monad, which is
 * used when pattern matching or using guards in for comprehensions.
 */
@typeclass trait MonadFilter[F[_]] extends Monad[F] {

  def empty[A]: F[A]

  ....
}
```

これから派生した関数・演算子として `filter` と `filterM` がある:

```scala
@typeclass trait MonadFilter[F[_]] extends Monad[F] {

  def empty[A]: F[A]

  def filter[A](fa: F[A])(f: A => Boolean): F[A] =
    flatMap(fa)(a => if (f(a)) pure(a) else empty[A])

  def filterM[A](fa: F[A])(f: A => F[Boolean]): F[A] =
    flatMap(fa)(a => flatMap(f(a))(b => if (b) pure(a) else empty[A]))
}
```

このように使う:

```console
scala> import cats.syntax.monadFilter._
scala> def filterSeven[F[_]: MonadFilter](f: F[Int]): F[Int] =
         f filter { _.show contains '7' }
scala> filterSeven((1 to 50).toList)
```

#### 騎士の旅

LYAHFGG:

> ここで、非決定性計算を使って解くのにうってつけの問題をご紹介しましょう。チェス盤の上にナイトの駒が1つだけ乗っています。ナイトを3回動かして特定のマスまで移動させられるか、というのが問題です。

ペアに型エイリアスを付けるかわりにまた case class にしよう:

```console
scala> case class KnightPos(c: Int, r: Int)
```

以下がナイトの次に取りうる位置を全て計算する関数だ:

```console
scala> case class KnightPos(c: Int, r: Int) {
         def move: List[KnightPos] =
           for {
             KnightPos(c2, r2) <- List(KnightPos(c + 2, r - 1), KnightPos(c + 2, r + 1),
               KnightPos(c - 2, r - 1), KnightPos(c - 2, r + 1),
               KnightPos(c + 1, r - 2), KnightPos(c + 1, r + 2),
               KnightPos(c - 1, r - 2), KnightPos(c - 1, r + 2)) if (
               ((1 to 8).toList contains c2) && ((1 to 8).toList contains r2))
           } yield KnightPos(c2, r2)
       }
scala> KnightPos(6, 2).move
scala> KnightPos(8, 1).move
```

答は合ってるみたいだ。次に、3回のチェインを実装する:

```console
scala> case class KnightPos(c: Int, r: Int) {
         def move: List[KnightPos] =
           for {
             KnightPos(c2, r2) <- List(KnightPos(c + 2, r - 1), KnightPos(c + 2, r + 1),
             KnightPos(c - 2, r - 1), KnightPos(c - 2, r + 1),
             KnightPos(c + 1, r - 2), KnightPos(c + 1, r + 2),
             KnightPos(c - 1, r - 2), KnightPos(c - 1, r + 2)) if (
             ((1 to 8).toList contains c2) && ((1 to 8).toList contains r2))
           } yield KnightPos(c2, r2)
         def in3: List[KnightPos] =
           for {
             first <- move
             second <- first.move
             third <- second.move
           } yield third
         def canReachIn3(end: KnightPos): Boolean = in3 contains end
       }
scala> KnightPos(6, 2) canReachIn3 KnightPos(6, 1)
scala> KnightPos(6, 2) canReachIn3 KnightPos(7, 3)
```

`(6, 2)` からは 3手で `(6, 1)` に動かすことができるけども、`(7, 3)` は無理のようだ。ピエールの鳥の例と同じように、モナド計算の鍵となっているのは 1手の効果が次に伝搬していることだと思う。

また、続きはここから。
