---
out: Reader.html
---

  [@runarorama]: https://twitter.com/runarorama
  [@jarhart]: https://twitter.com/jarhart
  [dsdi]: http://functionaltalks.org/2013/06/17/runar-oli-bjarnason-dead-simple-dependency-injection/
  [ltudif]: https://yow.eventer.com/yow-2012-1012/lambda-the-ultimate-dependency-injection-framework-by-runar-bjarnason-1277
  [sycpb]: http://blog.originate.com/blog/2013/10/21/reader-monad-for-dependency-injection/

### Reader データ型

[すごいHaskellたのしく学ぼう](http://www.amazon.co.jp/dp/4274068854) 曰く:

> 第11章では、関数を作る型、`(->) r` も、`Functor` のインスタンスであることを見ました。

```console:new
scala> import cats._, cats.std.all._, cats.syntax.functor._
scala> val f = (_: Int) * 2
scala> val g = (_: Int) + 10
scala> (g map f)(8)
```

> それから、関数はアプリカティブファンクターであることも見ましたね。これにより、関数が将来返すであろう値を、すでに持っているかのように演算できるようになりました。

```console
scala> import cats.syntax.monoidal._
scala> val h = (f |@| g) map {_ + _}
scala> h(3)
```

> 関数の型 `(->) r` はファンクターであり、アプリカティブファンクターであるばかりでなく、モナドでもあります。これまでに登場したモナド値と同様、関数もまた文脈を持った値だとみなすことができるのです。関数にとっての文脈とは、値がまだ手元になく、値が欲しければその関数を別の何かに適用しないといけない、というものです。

この例題も実装してみよう:

```console
scala> import cats.syntax.flatMap._
scala> val addStuff: Int => Int = for {
         a <- (_: Int) * 2
         b <- (_: Int) + 10
       } yield a + b
scala> addStuff(3)
```

> `(*2)` と `(+10)` はどちらも `3` に適用されます。実は、`return (a+b)` も同じく `3` に適用されるんですが、引数を無視して常に `a+b` を返しています。そいういうわけで、関数モナドは **Reader モナド**とも呼ばれたりします。すべての関数が共通の情報を「読む」からです。

`Reader` モナドは値が既にあるかのようなフリをさせてくれる。恐らくこれは1つのパラメータを受け取る関数でしか使えない。

#### DI: Dependency injection

2012年3月9日にあった nescala 2012 で Rúnar ([@runarorama][@runarorama]) さんが
[Dead-Simple Dependency Injection][dsdi]
というトークを行った。そこで提示されたアイディアの一つは `Reader` モナドを dependency injection
に使うというものだった。同年の 12月に YOW 2012 でそのトークを長くした
[Lambda: The Ultimate Dependency Injection Framework][ltudif]
も行われた。
翌 2013年に Jason Arhart さんが書いた
[Scrap Your Cake Pattern Boilerplate: Dependency Injection Using the Reader Monad][sycpb]
に基づいた例をここでは使うことにする。

まず、ユーザを表す case class と、ユーザを取得するためのデータストアを抽象化した trait があるとする。

```console
scala> :paste
case class User(id: Long, parentId: Long, name: String, email: String)
trait UserRepo {
  def get(id: Long): User
  def find(name: String): User
}
```

次に、`UserRepo` trait の全ての演算に対してプリミティブ・リーダーを定義する:

```console
scala> :paste
trait Users {
  def getUser(id: Long): UserRepo => User = {
    case repo => repo.get(id)
  }
  def findUser(name: String): UserRepo => User = {
    case repo => repo.find(name)
  }
}
```

(ボイラープレートをぶち壊せとか言いつつ) これはボイラープレートっぽい。一応、次。

プリミティブ・リーダーを合成することで、アプリケーションを含む他のリーダーを作ることができる。

```console
scala> :paste
object UserInfo extends Users {
  def userInfo(name: String): UserRepo => Map[String, String] =
    for {
      user <- findUser(name)
      boss <- getUser(user.parentId)
    } yield Map(
      "name" -> s"\${user.name}",
      "email" -> s"\${user.email}",
      "boss_name" -> s"\${boss.name}"
    )
}
trait Program {
  def app: UserRepo => String =
    for {
      fredo <- UserInfo.userInfo("Fredo")
    } yield fredo.toString
}
```

この `app` を実行するためには、`UserRepo` の実装を提供する何かが必要だ:

```console
scala> :paste
import cats.syntax.eq._

val testUsers = List(User(0, 0, "Vito", "vito@example.com"),
  User(1, 0, "Michael", "michael@example.com"),
  User(2, 0, "Fredo", "fredo@example.com"))
object Main extends Program {
  def run: String = app(mkUserRepo)
  def mkUserRepo: UserRepo = new UserRepo {
    def get(id: Long): User = (testUsers find { _.id === id }).get
    def find(name: String): User = (testUsers find { _.name === name }).get
  }
}
Main.run
```

ボスの名前が表示された。

`for` 内包表記の代わりに `actM` を使ってみる:

```console
scala> :paste
object UserInfo extends Users {
  import example.MonadSyntax._
  def userInfo(name: String): UserRepo => Map[String, String] =
    actM[UserRepo => ?, Map[String, String]] {
      val user = findUser(name).next
      val boss = getUser(user.parentId).next
      Map(
        "name" -> s"\${user.name}",
        "email" -> s"\${user.email}",
        "boss_name" -> s"\${boss.name}"
      )
    }
}
trait Program {
  import example.MonadSyntax._
  def app: UserRepo => String =
    actM[UserRepo => ?, String] {
      val fredo = UserInfo.userInfo("Fredo").next
      fredo.toString
    }
}
object Main extends Program {
  def run: String = app(mkUserRepo)
  def mkUserRepo: UserRepo = new UserRepo {
    def get(id: Long): User = (testUsers find { _.id === id }).get
    def find(name: String): User = (testUsers find { _.name === name }).get
  }
}
Main.run
```

`actM` ブロックの中は `for` バージョンよりも自然な形に見えるけども、
型注釈が必要なせいで、多分こっちの方が使いづらいと思う。

今日はここまで。
