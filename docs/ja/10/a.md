---
out: monad-transfomers.html
---

  [mf]: http://book.realworldhaskell.org/read/monad-transformers.html
  [Reader]: Reader.html
  [sycpb]: http://blog.originate.com/blog/2013/10/21/reader-monad-for-dependency-injection/
  [Free-monads]: Free-monads.html

これまでも出かかってきてたけど、未だ取り扱っていなかった話題としてモナド変換子という概念がある。
幸いなことに、Haskell の良書でオンライン版も公開されている本がもう 1冊あるので、これを参考にしてみる。

### モナド変換子

[Real World Haskell][mf] 曰く:

> もし標準の `State` モナドに何らかの方法でエラー処理を追加することができれば理想的だ。
> 一から手書きで独自のモナドを作るのは当然避けたい。`mtl` ライブラリに入っている標準のモナド同士を組み合わせることはできない。
> だけども、このライブラリは**モナド変換子**というものを提供して、同じことを実現できる。
>
> モナド変換子は通常のモナドに似ているが、孤立して使える実体ではなく、
> 基盤となる別のモナドの振る舞いを変更するものだ。

#### Dependency injection 再び

[6日目][Reader] にみた `Reader` データ型 (`Function1`) を DI に使うという考えをもう一度見てみよう。

```console:new
scala> :paste
case class User(id: Long, parentId: Long, name: String, email: String)
trait UserRepo {
  def get(id: Long): User
  def find(name: String): User
}
```

Jason Arhart さんの
[Scrap Your Cake Pattern Boilerplate: Dependency Injection Using the Reader Monad][sycpb]
は `Config` オブジェクトを作ることで `Reader` データ型を複数のサービスのサポートに一般化している:

```console
scala> import java.net.URI
scala> :paste
trait HttpService {
  def get(uri: URI): String
}
trait Config {
  def userRepo: UserRepo
  def httpService: HttpService
}
```

これを使うには `Config => A` 型のミニ・プログラムを作って、それらを合成する。

ここで、`Option` を使って失敗という概念もエンコードしたいとする。

#### ReaderT としての Kleisli

昨日見た `Kleisli` データ型を `ReaderT`、つまり `Reader` データ型のモナド変換子版として使って、それを `Option`
の上に積み上げることができる:

```console
scala> import cats._, cats.data._, cats.implicits._
scala> :paste
type ReaderTOption[A, B] = Kleisli[Option, A, B]
object ReaderTOption {
  def ro[A, B](f: A => Option[B]): ReaderTOption[A, B] = Kleisli(f)
}
```

`Config` を変更して `httpService` をオプショナルにする:

```console
scala> :paste
trait UserRepo {
  def get(id: Long): Option[User]
  def find(name: String): Option[User]
}
trait Config {
  def userRepo: UserRepo
  def httpService: Option[HttpService]
}
```

次に、「プリミティブ」なリーダーが `ReaderTOption[Config, A]` を返すように書き換える:

```console
scala> :paste
trait Users {
  def getUser(id: Long): ReaderTOption[Config, User] =
    ReaderTOption.ro {
      case config => config.userRepo.get(id)
    }
  def findUser(name: String): ReaderTOption[Config, User] =
    ReaderTOption.ro {
      case config => config.userRepo.find(name)
    }
}
trait Https {
  def getHttp(uri: URI): ReaderTOption[Config, String] =
    ReaderTOption.ro {
      case config => config.httpService map {_.get(uri)}
    }
}
```

これらのミニ・プログラムを合成して複合プログラムを書くことができる:

```console
scala> :paste
trait Program extends Users with Https {
  def userSearch(id: Long): ReaderTOption[Config, String] =
    for {
      u <- getUser(id)
      r <- getHttp(new URI(s"http://www.google.com/?q=\${u.name}"))
    } yield r
}
object Main extends Program {
  def run(config: Config): Option[String] =
    userSearch(2).run(config)
}
val dummyConfig: Config = new Config {
  val testUsers = List(User(0, 0, "Vito", "vito@example.com"),
    User(1, 0, "Michael", "michael@example.com"),
    User(2, 0, "Fredo", "fredo@example.com"))
  def userRepo: UserRepo = new UserRepo {
    def get(id: Long): Option[User] =
      testUsers find { _.id === id }
    def find(name: String): Option[User] =
      testUsers find { _.name === name }
  }
  def httpService: Option[HttpService] = None
}
```

上の `ReaderTOption` データ型は、`Reader` の設定の読み込む能力と、
`Option` の失敗を表現できる能力を組み合わせたものとなっている。

### 複数のモナド変換子を積み上げる

RWH:

> 普通のモナドにモナド変換子を積み上げると、別のモナドになる。
> これは組み合わされたモナドの上にさらにモナド変換子を積み上げて、新しいモナドを作ることができる可能性を示唆する。
> 実際に、これはよく行われていることだ。

状態遷移を表す `StateT` を `ReaderTOption` の上に積んでみる。

```console
scala> :paste
type StateTReaderTOption[C, S, A] = StateT[({type l[X] = ReaderTOption[C, X]})#l, S, A]
object StateTReaderTOption {
  def state[C, S, A](f: S => (S, A)): StateTReaderTOption[C, S, A] =
    StateT[({type l[X] = ReaderTOption[C, X]})#l, S, A] {
      s: S => Monad[({type l[X] = ReaderTOption[C, X]})#l].pure(f(s))
    }
  def get[C, S]: StateTReaderTOption[C, S, S] =
    state { s => (s, s) }
  def put[C, S](s: S): StateTReaderTOption[C, S, Unit] =
    state { _ => (s, ()) }
  def ro[C, S, A](f: C => Option[A]): StateTReaderTOption[C, S, A] =
    StateT[({type l[X] = ReaderTOption[C, X]})#l, S, A] {
      s: S =>
        ReaderTOption.ro[C, (S, A)]{
          c: C => f(c) map {(s, _)}
        }
    }
}
```

これは分かりづらいので、分解してみよう。
結局の所 `State` データ型は `S => (S, A)` をラッピングするものだから、`state` のパラメータ名はそれに合わせた。
次に、`ReaderTOption` のカインドを `* -> *` (ただ 1つのパラメータを受け取る型コンストラクタ) に変える。

同様に、このデータ型を `ReaderTOption` として使う方法が必要なので、それは `ro` に渡される `C => Option[A]` として表した。

これで `Stack` を実装することができる。今回は `String` を使ってみよう。

```console
scala> type Stack = List[String]
scala> val pop = StateTReaderTOption.state[Config, Stack, String] {
         case x :: xs => (xs, x)
         case _       => ???
       }
```

`pop` と `push` を `get` と `push` プリミティブを使って書くこともできる:

```console
scala> import StateTReaderTOption.{get, put}
scala> val pop: StateTReaderTOption[Config, Stack, String] =
         for {
           s <- get[Config, Stack]
           (x :: xs) = s
           _ <- put(xs)
         } yield x
scala> def push(x: String): StateTReaderTOption[Config, Stack, Unit] =
         for {
           xs <- get[Config, Stack]
           r <- put(x :: xs)
         } yield r
```

ついでに `stackManip` も移植する:

```console
scala> def stackManip: StateTReaderTOption[Config, Stack, String] =
         for {
           _ <- push("Fredo")
           a <- pop
           b <- pop
         } yield(b)
```

実行してみよう。

```console
scala> stackManip.run(List("Hyman Roth")).run(dummyConfig)
```

とりあえず `State` 版と同じ機能までたどりつけた。
次に、`Users` を `StateTReaderTOption.ro` を使うように書き換える:

```console
scala> :paste
trait Users {
  def getUser[S](id: Long): StateTReaderTOption[Config, S, User] =
    StateTReaderTOption.ro[Config, S, User] {
      case config => config.userRepo.get(id)
    }
  def findUser[S](name: String): StateTReaderTOption[Config, S, User] =
    StateTReaderTOption.ro[Config, S, User] {
      case config => config.userRepo.find(name)
    }
}
```

これを使ってリードオンリーの設定を使ったスタックの操作ができるようになった:

```console
scala> :paste
trait Program extends Users {
  def stackManip: StateTReaderTOption[Config, Stack, Unit] =
    for {
      u <- getUser(2)
      a <- push(u.name)
    } yield(a)
}
object Main extends Program {
  def run(s: Stack, config: Config): Option[(Stack, Unit)] =
    stackManip.run(s).run(config)
}
```

このプログラムはこのように実行できる:

```console
scala> Main.run(List("Hyman Roth"), dummyConfig)
```

これで `StateT`、`ReaderT`、それと `Option` を同時に動かすことができた。
僕が使い方を良く分かってないせいかもしれないが、`StateTReaderTOption` に関して `state` や `ro`
のようなモナド・コンストラクタを書き出すのは頭をひねる難問だった。

プリミティブなモナド値さえ構築できてしまえば、実際の使う側のコード (`stackManip` などは) 比較的クリーンだと言える。
Cake パターンは確かに回避してるけども、コード中に積み上げられたモナド型である `StateTReaderTOption` が散らばっている設計になっている。

最終目的として `getUser(id: Long)` と `push`　などを同時に使いたいというだけの話なら、
8日目に見た[自由モナド][Free-monads]を使うことで、これらをコマンドとして持つ DSL を構築することも代替案として考えられる。
