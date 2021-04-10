---
out: FlatMap.html
---

  [fom]: http://learnyouahaskell.com/a-fistful-of-monads
  [FlatMapSource]: $catsBaseUrl$/core/src/main/scala/cats/FlatMap.scala
  [FlatMapSyntaxSource]: $catsBaseUrl$/core/src/main/scala/cats/syntax/flatMap.scala

### FlatMap

今日は[すごいHaskellたのしく学ぼう](http://www.amazon.co.jp/dp/4274068854)の新しい章「モナドがいっぱい」を始めることができる。

> モナドはある願いを叶えるための、アプリカティブ値の自然な拡張です。その願いとは、「普通の値 `a` を取って文脈付きの値を返す関数に、文脈付きの値 `m a` を渡したい」というものです。

Cats は Monad 型クラスを `FlatMap` と `Monad` という 2つの型クラスに分ける。
以下が[FlatMap の型クラスのコントラクト]だ:

```scala
@typeclass trait FlatMap[F[_]] extends Apply[F] {
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B]

  ....
}
```

`FlatMap` が、`Applicative` の弱いバージョンである `Apply` を拡張することに注目してほしい。これらが[演算子][FlatMapSyntaxSource]だ:

```scala
class FlatMapOps[F[_], A](fa: F[A])(implicit F: FlatMap[F]) {
  def flatMap[B](f: A => F[B]): F[B] = F.flatMap(fa)(f)
  def mproduct[B](f: A => F[B]): F[(A, B)] = F.mproduct(fa)(f)
  def >>=[B](f: A => F[B]): F[B] = F.flatMap(fa)(f)
  def >>[B](fb: F[B]): F[B] = F.flatMap(fa)(_ => fb)
}
```

これは `flatMap` 演算子とシンボルを使ったエイリアスである `>>=` を導入する。他の演算子に関しては後回しにしよう。とりあえず標準ライブラリで `flatMap` は慣れている:

```scala mdoc
import cats._, cats.syntax.all._

(Right(3): Either[String, Int]) flatMap { x => Right(x + 1) }
```

#### Option から始める

本の通り、`Option` から始めよう。この節では Cats の型クラスを使っているのか標準ライブラリの実装なのかについてはうるさく言わないことにする。
以下がファンクターとしての `Option`:

```scala mdoc
"wisdom".some map { _ + "!" }

none[String] map { _ + "!" }
```

`Apply` としての `Option`:

```scala mdoc
({(_: Int) + 3}.some) ap 3.some

none[String => String] ap "greed".some

({(_: String).toInt}.some) ap none[String]
```

以下は `FlatMap` としての `Option`:

```scala mdoc
3.some flatMap { (x: Int) => (x + 1).some }

"smile".some flatMap { (x: String) =>  (x + " :)").some }

none[Int] flatMap { (x: Int) => (x + 1).some }

none[String] flatMap { (x: String) =>  (x + " :)").some }
```

期待通り、モナディックな値が `None` の場合は `None` が返ってきた。

#### FlatMap則

FlatMap には結合律 (associativity) という法則がある:

- associativity: `(m flatMap f) flatMap g === m flatMap { x => f(x) flatMap {g} }`

Cats の `FlatMapLaws` にはあと 2つ定義してある:

```scala
trait FlatMapLaws[F[_]] extends ApplyLaws[F] {
  implicit override def F: FlatMap[F]

  def flatMapAssociativity[A, B, C](fa: F[A], f: A => F[B], g: B => F[C]): IsEq[F[C]] =
    fa.flatMap(f).flatMap(g) <-> fa.flatMap(a => f(a).flatMap(g))

  def flatMapConsistentApply[A, B](fa: F[A], fab: F[A => B]): IsEq[F[B]] =
    fab.ap(fa) <-> fab.flatMap(f => fa.map(f))

  /**
   * The composition of `cats.data.Kleisli` arrows is associative. This is
   * analogous to [[flatMapAssociativity]].
   */
  def kleisliAssociativity[A, B, C, D](f: A => F[B], g: B => F[C], h: C => F[D], a: A): IsEq[F[D]] = {
    val (kf, kg, kh) = (Kleisli(f), Kleisli(g), Kleisli(h))
    ((kf andThen kg) andThen kh).run(a) <-> (kf andThen (kg andThen kh)).run(a)
  }
}
```

