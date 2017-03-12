---
out: stacking-future-and-either.html
---

  [xuweik]: https://twitter.com/xuwei_k
  [combine-future-and-either]: http://d.hatena.ne.jp/xuwei/20140919/1411136788
  [seal]: https://twitter.com/xuwei_k/status/392260189673373696
  [xwj]: https://ja.wikipedia.org/wiki/%E5%BE%90%E6%B8%AD
  [3hj86e]: http://twitpic.com/3hj86e
  [Xor]: Xor.html
  [XorTSource]: $catsBaseUrl$core/src/main/scala/cats/data/XorT.scala

### Future と Either の積み上げ

モナド変換子の用例として度々取り上げられるものに `Future` データ型と `Either` の積み上げがある。
日本語で書かれたブログ記事として吉田さん ([@xuwei_k][xuweik]) の
[Scala で Future と Either を組み合わせたときに綺麗に書く方法][combine-future-and-either]というものがある。

東京の外だとあまり知られていない話だと思うが、吉田さんは書道科専攻で、大学では篆書を書いたり判子を刻って
(ほる? 何故か変換できない) いたらしい:

<blockquote class="twitter-tweet" lang="en"><p lang="ja" dir="ltr">「大学では、はんこを刻ったり、篆書を書いてました」&#10;「えっ？なぜプログラマに？？？」 <a href="http://t.co/DEhqy4ELpF">pic.twitter.com/DEhqy4ELpF</a></p>&mdash; Kenji Yoshida (@xuwei_k) <a href="https://twitter.com/xuwei_k/status/392260189673373696">October 21, 2013</a></blockquote>
<script async src="//platform.twitter.com/widgets.js" charset="utf-8"></script>

ハンドル名の由来となっている[徐渭][xwj]は明代の書・画・詩・詞・戯曲・散文の士で自由奔放な作風で有名だった。
これは吉田さんの[関数型言語][3hj86e]という書だ。

それはさておき、`Future` と `Either` を積み上げる必要が何故あるのだろうか?
ブログ記事によるとこういう説明になっている:

1. `Future[A]` は Scala によく現れる。
2. future をブロックしたくないため、そこらじゅう `Future` だらけになる。
3. `Future` は非同期であるため、発生したエラーを捕獲する必要がある。
4. `Future` は `Throwable` は処理できるが、それに限られている。
5. プログラムが複雑になってくると、エラー状態に対して自分で型付けしたくなってくる。
6. `Future` と `Either` を組み合わせるには?

ここからが準備段階となる:

```console:new
scala> :paste
case class User(id: Long, name: String)

// In actual code, probably more than 2 errors
sealed trait Error
object Error {
  final case class UserNotFound(userId: Long) extends Error
  final case class ConnectionError(message: String) extends Error
}
object UserRepo {
  def followers(userId: Long): Either[Error, List[User]] = ???
}
import UserRepo.followers
```

> user がいて、twitter のようにフォローできて、「フォローしてる」「フォローされてる」という関係を保持するアプリを作るとします。
>
> とりあえず今あるのは、followers という、指定された userId の follower 一覧を取ってくるメソッドです。
> さて、このメソッドだけがあったときに
> 「あるユーザー同士が、相互フォローの関係かどうか？」
> を取得するメソッドはどう書けばよいでしょうか？

答えも載っているので、そのまま REPL に書き出してみる。`UserId` 型だけは `Long` に変えた。

```console
scala> def isFriends0(user1: Long, user2: Long): Either[Error, Boolean] =
         for {
           a <- followers(user1).right
           b <- followers(user2).right
         } yield a.exists(_.id == user2) && b.exists(_.id == user1)
```

次に、データベース・アクセスか何かを非同期にするために `followers` が `Future` を返すようにする:

```console
scala> :paste
import scala.concurrent.{ Future, ExecutionContext }
object UserRepo {
  def followers(userId: Long): Future[Either[Error, List[User]]] = ???
}
import UserRepo.followers
```

> さてそうしたときに、isFriendsメソッドは、どのように書き換えればいいでしょうか？さて、これもすぐに正解だしてしまいます。
> ただ、一応２パターンくらい出しておきましょう

```console
scala> def isFriends1(user1: Long, user2: Long)
         (implicit ec: ExecutionContext): Future[Either[Error, Boolean]] =
         for {
           a <- followers(user1)
           b <- followers(user2)
         } yield for {
           x <- a.right
           y <- b.right
         } yield x.exists(_.id == user2) && y.exists(_.id == user1)
```

次のがこれ:

```console
scala> def isFriends2(user1: Long, user2: Long)
         (implicit ec: ExecutionContext): Future[Either[Error, Boolean]] =
         followers(user1) flatMap {
           case Right(a) =>
             followers(user2) map {
               case Right(b) =>
                 Right(a.exists(_.id == user2) && b.exists(_.id == user1))
               case Left(e) =>
                 Left(e)
             }
           case Left(e) =>
             Future.successful(Left(e))
         }
```

これらの2つのバージョンの違いは何だろうか?

> 正常系の場合の動作は同じですが、`followers(user1)` がエラーだった場合の動作が異なります。
>
> 上記の `for`式を2回使ってる `isFriends1` のほうでは、`followers(user1)` がエラーでも、
> `followers(user2)` の呼び出しは必ず実行されます。
>
> 一方、`isFriends2` のほうは、`followers(user1)` の呼び出しがエラーだと、`followers(user2)` は実行されません。

どちらにせよ、両方の関数も元のものに比べると入り組んだものとなった。
しかも増えた部分のコードは紋切型 (ボイラープレート) な型合わせをしているのがほとんどだ。
`Future[Either[Error, A]]` が出てくる**全て**の関数をこのように書き換えるのは想像したくない。

#### EitherT データ型

`Either` のモナド変換子版である `XorT` データ型というものがある。

```scala
/**
 * Transformer for `Either`, allowing the effect of an arbitrary type constructor `F` to be combined with the
 * fail-fast effect of `Either`.
 *
 * `EitherT[F, A, B]` wraps a value of type `F[Either[A, B]]`. An `F[C]` can be lifted in to `EitherT[F, A, C]` via `EitherT.right`,
 * and lifted in to a `EitherT[F, C, B]` via `EitherT.left`.
 */
case class EitherT[F[_], A, B](value: F[Either[A, B]]) {
  ....
}
```

`UserRepo.followers` を仮実装してみると、こうなった:

```console
scala> :paste
import cats._, cats.instances.all._
import cats.data.EitherT
object UserRepo {
  def followers(userId: Long)
    (implicit ec: ExecutionContext): EitherT[Future, Error, List[User]] =
    userId match {
      case 0L =>
        EitherT.right(Future { List(User(1, "Michael")) })
      case 1L =>
        EitherT.right(Future { List(User(0, "Vito")) })
      case x =>
        println("not found")
        EitherT.left(Future.successful { Error.UserNotFound(x) })
    }
}
import UserRepo.followers
```

`isFriends0` の書き換えをもう一度やってみる。

```console
scala> def isFriends3(user1: Long, user2: Long)
         (implicit ec: ExecutionContext): EitherT[Future, Error, Boolean] =
         for{
           a <- followers(user1)
           b <- followers(user2)
         } yield a.exists(_.id == user2) && b.exists(_.id == user1)
```

素晴らしくないだろうか? 型シグネチャを変えて、あと `ExecutionContext` を受け取るようしたこと以外は、
`isFriends3` は `isFriends0` と同一のものだ。

実際に使ってみよう。

```console
scala> implicit val ec = scala.concurrent.ExecutionContext.global
scala> import scala.concurrent.Await
scala> import scala.concurrent.duration._
scala> Await.result(isFriends3(0, 1).value, 1 second)
```

最初のユーザが見つからない場合は、`EitherT` はショートするようになっている。

```scala
scala> Await.result(isFriends3(2, 3).value, 1 second)
not found
res34: cats.data.Xor[Error,Boolean] = Left(UserNotFound(2))
```

`"not found"` は一回しか表示されなかった。

`StateTReaderTOption` の例と違って、この `XorT` は様々な場面で活躍しそうな雰囲気だ。

今日はこれまで。
