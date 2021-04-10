---
out: partial-unification.html
---

  [SI-2712]: https://issues.scala-lang.org/browse/SI-2712
  [cecc3a]: https://github.com/stew/cats/commit/cecc3afbdbb6fbbe764005cd52e9efe7acdfc8f2
  [combining-applicative]: combining-applicative.html

### 部分的ユニフィケーションを用いた型推論の強制

EIP:

> ここではいくつかのデータ型とそれに関連した強要関数 (coercion function)、
> `Id`、 `unId`、 `Const`、 `unConst` が出てくる。
> 読みやすくするために、これらの強要に共通する記法を導入する。

Scala の場合は implicit と型推論だけで結構いける。
だけど、型クラスを駆使していると Scala の型推論の弱点にも出くわすことがある。
中でも頻繁に遭遇するのは部分適用されたパラメータ型を推論できないという問題で、
[SI-2712][SI-2712] として知られている。

<s>今、これを読んでいるならば、そのページに飛んで投票を行うか、できれば問題を解決するのを手伝ってきてほしい。</s>

これは Miles Sabin さんによって [scala#5102](https://github.com/scala/scala/pull/5102) において、"-Ypartial-unification" フラグとして修正された。[Explaining Miles's Magic](https://gist.github.com/djspiewak/7a81a395c461fd3a09a6941d4cd040f2) も参照してほしい。

以下は Daniel さんが用いた例だ:

```scala mdoc
def foo[F[_], A](fa: F[A]): String = fa.toString

foo { x: Int => x * 2 }
```

上の例は以前はコンパイルしなかった。

> コンパイルしない理由は `Function1` が 2つのパラメータを受け取るのに対して、`F[_]` は 1つしかパラメータを取らないからだ。

`-Ypartial-unification` によってコンパイルするようになるが、コンパイラは型コンストラクタが左から右へと部分的に適用可能だという前提で推測を行うことに注意する必要がある。つまり、これは右バイアスのかかった `Either` のようなデータ型に恩恵があるが、左バイアスのかかったデータ型を使っていると間違った結果が得られる可能性がある。

2019年に Scala 2.13.0 がリリースされ、これは部分的ユニフィケーションがデフォルトで使えるようになった。
