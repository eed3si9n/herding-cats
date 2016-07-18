---
out: applicative-wordcount.html
---

  [388]: https://github.com/typelevel/cats/pull/388

### Applicative wordcount

EIP 6節、「アプリカティブ・ファンクターを用いたモジュラー・プログラミング」まで飛ばす。

EIP:

> アプリカティブ・ファンクターには他にもモナドに勝る利点があって、
> それは複雑な反復をよりシンプルなものからモジュラーに開発できることにある。
> ....
>
> Unix でよく使われる wordcount ユーティリティである `wc` を例にこれを説明しよう。
> ｀wc` はテキストファイルの文字数、語句数、行数を計算する。

この例は完全にアプリカティブ関数の合成を使って翻訳することができるけども、
この機能は現在私家版のブランチのみで公開されている。 (PR [#388][388] は審査中)

#### アプリカティブなモジュラー反復

```console:new
scala> import cats._, cats.std.all._
scala> import cats.data.{ Func, AppFunc, Const }
scala> import Func.{ appFunc, appFuncU }
```

> `wc` プログラムの文字数のカウント部分は「モノイドとしての `Int`」のアプリカティブ・ファンクターを累積した結果となる:

以下は `Int` をモノイダル・アプリカティブとして使うための型エイリアスだ:

```console
scala> type Count[A] = Const[Int, A]
```

上のコードでは、`A` は最後まで使われないファントム型なので、`Unit` に決め打ちしてしまう:

```console
scala> def liftInt(i: Int): Count[Unit] = Const(i)
scala> def count[A](a: A): Count[Unit] = liftInt(1)
```

> この反復の本体は全ての要素に対して 1 を返す:

```console
scala> val countChar: AppFunc[Count, Char, Unit] = appFunc(count)
```

この `AppFunc` を使うには、`traverse` を `List[Char]` と共に呼び出す。
これは Hamlet から僕が見つけてきた引用だ:

```console
scala> val text = ("Faith, I must leave thee, love, and shortly too.\n" +
                  "My operant powers their functions leave to do.\n").toList
scala> countChar traverse text
```

うまくいった。

> 行数のカウント (実際には改行文字のカウントなので、最終行に改行が無いと、それは無視される) も同様だ。
> 違いは使う数字が違うだけで、それぞれ改行文字ならば 1、それ以外は 0 を返すようにする。

```console
scala> import cats.syntax.eq._
scala> def testIf(b: Boolean): Int = if (b) 1 else 0
scala> val countLine: AppFunc[Count, Char, Unit] =
         appFunc { (c: Char) => liftInt(testIf(c === '\n')) }
```

これも、使うには `traverse` を呼び出す:

```console
scala> countLine traverse text
```

> 語句のカウントは、状態が関わってくるため少しトリッキーだ。
> ここでは、現在語句内にいるかどうかを表す `Boolean` 値の状態を使った `State` モナドを使って、
> 次にそれをカウントするためのアプリカティブ・ファンクターに合成する。

```console
scala> def isSpace(c: Char): Boolean = (c === ' ' || c === '\n' || c === '\t')
scala> val countWord =
         appFuncU { (c: Char) =>
           import cats.data.State.{ get, set }
           for {
             x <- get[Boolean]
             y = !isSpace(c)
             _ <- set(y)
           } yield testIf(y && !x)
         } andThen appFunc(liftInt)
```

`AppFunc` を走査するとこれは `State` データ型が返ってくる:

```console
scala> val x = countWord traverse text
```

この状態機械を初期値 `false` で実行すると結果が返ってくる:

```console
scala> x.runA(false).run
```

17 words だ。

`shape` と `content` でやったように、アプリカティブ関数を組み合わせることで走査を 1つに融合 (fuse) できる。

```console
scala> val countAll = countWord product countLine product countChar
scala> val allResults = countAll traverse text
scala> val charCount = allResults.second
scala> val lineCount = allResults.first.second
scala> val wordCountState = allResults.first.first
scala> val wordCount = wordCountState.runA(false).run
```

EIP:

> アプリカティブ・ファンクターはより豊かな合成演算子を持つため、
> 多くの場合モナド変換子をリプレースすることができる。
> また、アプリカティブはモナド以外の計算も合成できるという利点もある。

今日はここまで。
