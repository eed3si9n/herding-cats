---
out: abstract-future.html
---

  [afja]: http://eed3si9n.com/ja/the-abstract-future
  [af]: http://logji.blogspot.com/2014/02/the-abstract-future.html
  [truepower]: http://d.hatena.ne.jp/xuwei/20150329/1427599055

### 抽象的な Future

特に大規模なアプリケーションを構築するという文脈でモナドの強力な応用例として、たまに[言及されている][truepower]ブログ記事として[抽象的な Future][afja]
([The Abstract Future][af]) がある。これはもともと Precog 社の開発チームからのブログに 2012年11月27日に
Kris Nuttycombe ([@nuttycom](https://twitter.com/nuttycom)) さんが投稿したものだ。

> Precog 社ではこの Future を多用しており、直接使ったり、Akka のアクターフレームワーク上に実装されたサブシステムと合成可能な方法で会話するための方法として使ったりしている。おそらく Future は今あるツールの中で非同期プログラミングにおける複雑さを抑えこむのに最も有用なものだと言えるだろう。そのため、僕らのコードベースの早期のバージョンの API は Future を直接露出させたものが多かった。
> ....
>
> これが何を意味するかというと、DatasetModule インターフェイスを使っているコンシューマの視点から見ると、Future の側面のうち依存しているのは、静的に型検査された方法で複数の演算を順序付けるという能力だけだ。つまり Future の非同期に関連したさまざまな意味論ではなく、この順序付けが型によって提供される情報のうち実際に使われているものだと言える。そのため、自然と以下の一般化を行うことができる。

ここでは吉田さんと似た例を用いることにする。

```scala mdoc
import cats._, cats.data._, cats.syntax.all._

case class User(id: Long, name: String)

// In actual code, probably more than 2 errors
sealed trait Error
object Error {
  final case class UserNotFound(userId: Long) extends Error
  final case class ConnectionError(message: String) extends Error
}

trait UserRepos[F[_]] {
  implicit def F: Monad[F]
  def userRepo: UserRepo
  trait UserRepo {
    def followers(userId: Long): F[List[User]]
  }
}
```

#### Future を使った UserRepos

`UserRepos` をまず `Future` を使って実装してみる。

```scala mdoc
import scala.concurrent.{ Future, ExecutionContext, Await }
import scala.concurrent.duration.Duration

class UserRepos0(implicit ec: ExecutionContext) extends UserRepos[Future] {
  override val F = implicitly[Monad[Future]]
  override val userRepo: UserRepo = new UserRepo0 {}
  trait UserRepo0 extends UserRepo {
    def followers(userId: Long): Future[List[User]] = Future.successful { Nil }
  }
}
```

このようにして使う:

```scala mdoc
{
  val service = new UserRepos0()(ExecutionContext.global)
  service.userRepo.followers(1L)
}
```

これで非同期な計算結果が得られた。テストのときは同期な値がほしいとする。

#### Id を使った UserRepos

> テスト時には僕たちの計算が非同期で実行されるという事実はおそらく心配したくない。最終的に正しい結果が取得できさえすればいいからだ。
> ....
>
> ほとんどの場合は、僕たちはテストには恒等モナドを使う。例えば、先程出てきた読み込み、ソート、take、reduce を組み合わせた機能をテストしたいとする。テストフレームワークはどのモナドを使っているかを一切考えずに済む。

ここが `Id` データ型の出番だ。

```scala mdoc
class TestUserRepos extends UserRepos[Id] {
  override val F = implicitly[Monad[Id]]
  override val userRepo: UserRepo = new UserRepo0 {}
  trait UserRepo0 extends UserRepo {
    def followers(userId: Long): List[User] =
      userId match {
        case 0L => List(User(1, "Michael"))
        case 1L => List(User(0, "Vito"))
        case x =>  sys.error("not found")
      }
  }
}
```

このようにして使う:

```scala mdoc
val testRepo = new TestUserRepos {}

val ys = testRepo.userRepo.followers(1L)
```

#### 抽象におけるコード

フォロワーの型コンストラクタを抽象化できたところで、10日目にも書いた相互フォローしているかどうかをチェックする `isFriends` を書いてみよう。

```scala mdoc
trait UserServices0[F[_]] { this: UserRepos[F] =>
  def userService: UserService = new UserService
  class UserService {
    def isFriends(user1: Long, user2: Long): F[Boolean] =
      F.flatMap(userRepo.followers(user1)) { a =>
        F.map(userRepo.followers(user2)) { b =>
          a.exists(_.id == user2) && b.exists(_.id == user1)
        }
      }
  }
}
```

このようにして使う:

```scala mdoc
{
  val testService = new TestUserRepos with UserServices0[Id] {}
  testService.userService.isFriends(0L, 1L)
}
```

これは `F[]` が `Monad` を形成するということ以外は一切何も知らずに `isFriends` が実装できることを示している。
`F` を抽象的に保ったままで中置記法の `flatMap` と `map` を使えればさらに良かったと思う。 `FlatMapOps(fa)` を手動で作ってみたけども、これは実行時に abstract method error になった。6日目に実装した `actM` マクロはうまく使えるみたいだ:

```scala mdoc
trait UserServices[F[_]] { this: UserRepos[F] =>
  def userService: UserService = new UserService
  class UserService {
    import example.MonadSyntax._
    def isFriends(user1: Long, user2: Long): F[Boolean] =
      actM[F, Boolean] {
        val a = userRepo.followers(user1).next
        val b = userRepo.followers(user2).next
        a.exists(_.id == user2) && b.exists(_.id == user1)
      }
  }
}

{
  val testService = new TestUserRepos with UserServices[Id] {}
  testService.userService.isFriends(0L, 1L)
}
```

#### EitherT を用いた UserRepos

これは `EitherT` を使って `Future` にカスタムエラー型を乗せたものとも使うことができる。

```scala mdoc
class UserRepos1(implicit ec: ExecutionContext) extends UserRepos[EitherT[Future, Error, *]] {
  override val F = implicitly[Monad[EitherT[Future, Error, *]]]
  override val userRepo: UserRepo = new UserRepo1 {}
  trait UserRepo1 extends UserRepo {
    def followers(userId: Long): EitherT[Future, Error, List[User]] =
      userId match {
        case 0L => EitherT.right(Future { List(User(1, "Michael")) })
        case 1L => EitherT.right(Future { List(User(0, "Vito")) })
        case x =>
          EitherT.left(Future.successful { Error.UserNotFound(x) })
      }
  }
}
```

このようにして使う:

```scala mdoc
{
  import scala.concurrent.duration._
  val service = {
    import ExecutionContext.Implicits._
    new UserRepos1 with UserServices[EitherT[Future, Error, *]] {}
  }

  Await.result(service.userService.isFriends(0L, 1L).value, 1 second)
}
```

3つのバージョンのサービスとも `UserServices` trait は一切変更せずに再利用できたことに注目してほしい。

今日はここまで。
