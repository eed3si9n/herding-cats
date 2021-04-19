---
out: Resource.html
---

  [Regions]: http://okmij.org/ftp/Haskell/regions.html
  [ResourceDoc]: https://typelevel.org/cats-effect/docs/std/resource
  [higher-rank]: https://apocalisp.wordpress.com/2010/07/02/higher-rank-polymorphism-in-scala/
  [MonadCancel]: MonadCancel.html

### Resource データ型

Rúnar さんは [Higher-Rank Polymorphism in Scala][higher-rank] を以下のように締めくくった:

> これ (ランク2多相) を使えば、Lightweight Monadic Regions で説明されている SIO monad のような、静的に保証された安全なリソースへのアクセスができるだろうか。

Cats Effect は [Resource][ResourceDoc] データ型を提供し、これは Oleg Kiselyov さんと Chung-chieh Shan さんの [Lightweight Monadic Regions][Regions] みたいに使えるかもしれない。18日目に見た [MonadCancel][MonadCancel] をデータ型としてエンコードしたものだ。

> `Resource` を構築する最も簡易な方法は `Resource.make` で、最も簡易にリソースを使う方法は `Resource#use` だ。任意のアクションを `Resource.eval` を使って持ち上げることもできる:

```scala
object Resource {
  def make[F[_], A](acquire: F[A])(release: A => F[Unit]): Resource[F, A]

  def eval[F[_], A](fa: F[A]): Resource[F, A]

  def fromAutoCloseable[F[_], A <: AutoCloseable](acquire: F[A])(
      implicit F: Sync[F]): Resource[F, A] =
    Resource.make(acquire)(autoCloseable => F.blocking(autoCloseable.close()))
}

sealed abstract class Resource[F[_], +A] {
  def use[B](f: A => F[B]): F[B]
}
```

[Lightweight Monadic Regions][Regions]:

> 実践に基づいた具体例を見ていこう:
>
> 1. 読み込み用に 2つのファイルを開き、ただし、片方を設定ファイルとする。
> 2. 設定ファイルから出力用ファイル名 (ログファイルなど) を読む。
> 3. 出力用ファイルを開いて、読み込みファイルの内容を交互に書き出す。
> 4. 設定ファイルを閉じる。
> 5. 別の読み込み用のファイルの残りの内容を出力用ファイルに書き出す。

以下はテキストファイルの最初の行を読み込むプログラムだ:

```scala mdoc
import cats._, cats.syntax.all._
import cats.effect.{ IO, MonadCancel, Resource }
import java.io.{ BufferedReader, BufferedWriter }
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

def bufferedReader(path: Path): Resource[IO, BufferedReader] =
  Resource.fromAutoCloseable(IO.blocking {
    Files.newBufferedReader(path, StandardCharsets.UTF_8)
  })
  .onFinalize { IO.println("closed " + path) }

lazy val program: IO[String] = {
  val r0 = bufferedReader(Paths.get("docs/19/00.md"))
  r0 use { reader0 =>
    IO.blocking { reader0.readLine }
  }
}
```

```scala
scala> {
         import cats.effect.unsafe.implicits._
         program.unsafeRunSync()
       }
closed docs/19/00.md
val res0: String = ---
```

以下は、テキストをファイルに書き込むプログラムだ:

```scala mdoc
def bufferedWriter(path: Path): Resource[IO, BufferedWriter] =
  Resource.fromAutoCloseable(IO.blocking {
    Files.newBufferedWriter(path, StandardCharsets.UTF_8)
  })
  .onFinalize { IO.println("closed " + path) }

lazy val program2: IO[Unit] = {
  val w0 = bufferedWriter(Paths.get("/tmp/Resource.txt"))
  w0 use { writer0 =>
    IO.blocking { writer0.write("test\n") }
  }
}

{
  import cats.effect.unsafe.implicits._
  program2.unsafeRunSync()
}
```

これは `/tmp/Resource.txt` という名前のテキストファイルを作成した。ここまではリソース管理的には些細なことしかしていない。Oleg さんと Chung-chieh Shan さんが提示した問題文は、ログファイルの名前は設定ファイルから読み出すが、ログファイルの方が設定ファイルのライフサイクルよりも長生きする必要があるのでより複雑だ。

```scala mdoc
def inner(input0: BufferedReader, config: BufferedReader): IO[(BufferedWriter, IO[Unit])] = for {
  fname <- IO.blocking { config.readLine }
  w0 = bufferedWriter(Paths.get(fname))

  // do the unsafe allocated
  p <- w0.allocated
  (writer0, releaseWriter0) = p
  _ <- IO.blocking { writer0.write(fname + "\n") }
  - <-
    (for {
      l0 <- IO.blocking { input0.readLine }
      _  <- IO.blocking { writer0.write(l0 + "\n") }
      l1 <- IO.blocking { config.readLine }
      _  <- IO.blocking { writer0.write(l1 + "\n") }
    } yield ()).whileM_(IO.blocking { input0.ready && config.ready })
} yield (writer0, releaseWriter0)

lazy val program3: IO[Unit] = {
  val r0 = bufferedReader(Paths.get("docs/19/00.md"))
  r0 use { input0 =>
    MonadCancel[IO].bracket({
      val r1 = bufferedReader(Paths.get("src/main/resources/a.conf"))
      r1 use { config => inner(input0, config) }
    })({ case (writer0, _) =>
      (for {
        l0 <- IO.blocking { input0.readLine }
        _  <- IO.blocking { writer0.write(l0 + "\n") }
      } yield ()).whileM_(IO.blocking { input0.ready })
    })({
      case (_, releaseWriter0) => releaseWriter0
    })
  }
}
```

ログファイルを閉じるのを避けるために `Resource#allocated` メソッドを使って、その代わりに後で絶対に閉じられることが保証されるように `MonadCancel[IO].bracket` を使った。走らせるとこのようになる:

```scala
scala> {
         import cats.effect.unsafe.implicits._
         program3.unsafeRunSync()
       }
closed src/main/resources/a.conf
closed /tmp/zip_test.txt
closed docs/19/00.md
```

設定ファイルが最初に閉じられているのが分かる。

少しズルをして例題を実装することができたが、`Resource` の柔軟性を示すことができたと思う。

#### モナドとしての Resource

`program3` は少しややこしくなったが、複数のリソースをまとめて取得して、まとめて解放したい場合がほとんどだと思う。

```scala mdoc
lazy val program4: IO[String] = (
  for {
    r0 <- bufferedReader(Paths.get("docs/19/00.md"))
    r1 <- bufferedReader(Paths.get("src/main/resources/a.conf"))
    w1 <- bufferedWriter(Paths.get("/tmp/zip_test.txt"))
  } yield (r0, r1, w1)
).use { case (intput0, config, writer0) =>
  IO.blocking { intput0.readLine }
}

{
  import cats.effect.unsafe.implicits._
  program4.unsafeRunSync()
}
```

上の例では、複数のリソースがもモナディックに組み合わされて、`use` されている。

#### Resource のキャンセル対応

`use` 中でもリソースがちゃんとキャンセル対応できるのかを確かめるために、`.` を永遠と表示するデモアプリを作って Ctrl-C でキャンセルさせてみよう:

```scala
import cats._, cats.syntax.all._
import cats.effect.{ ExitCode, IO, IOApp, Resource }
import java.io.{ BufferedReader, BufferedWriter }
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

object Hello extends IOApp {
  def bufferedReader(path: Path): Resource[IO, BufferedReader] =
    Resource.fromAutoCloseable(IO.blocking {
      Files.newBufferedReader(path, StandardCharsets.UTF_8)
    })
    .onFinalize { IO.println("closed " + path) }

  override def run(args: List[String]): IO[ExitCode] =
    program.as(ExitCode.Success)

  lazy val program: IO[String] = (
    for {
      r0 <- bufferedReader(Paths.get("docs/19/00.md"))
      r1 <- bufferedReader(Paths.get("src/main/resources/a.conf"))
    } yield (r0, r1)
  ).use { case (intput0, config) =>
    IO.print(".").foreverM
  }
}
```

アプリを実行した結果こうなった:

```
\$ java -jar target/scala-2.13/herding-cats-assembly-0.1.0-SNAPSHOT.jar
..................................................................................................................................................................................................................................................................................................................................................................................................................................................................................^C............................................................................................................................................................................................................................................closed src/main/resources/a.conf
closed docs/19/00.md
```

リソースがちゃんと閉じられているのが分かる。よくできました。

これは `use { ... }` 中に起こっているので、`Resource` が `MonadCancel` を形成するというのはちょっと違うことに注意してほしい。`use` の定義を見ると理解が深まるかもしれない:

```scala
  /**
   * Allocates a resource and supplies it to the given function.
   * The resource is released as soon as the resulting `F[B]` is
   * completed, whether normally or as a raised error.
   *
   * @param f the function to apply to the allocated resource
   * @return the result of applying [F] to
   */
  def use[B](f: A => F[B])(implicit F: MonadCancel[F, Throwable]): F[B] =
    fold(f, identity)
```

この場合、Ctrl-C は IO が処理していて、`use { ... }` は `f` が失敗したときでもリソースが解放されることを保証しているんだと思う。
