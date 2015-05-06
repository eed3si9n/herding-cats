---
out: Kinds.html
---

  [moott]: http://learnyouahaskell.com/making-our-own-types-and-typeclasses
  [cheatsheet]: scalaz-cheatsheet.html
  [scala2340]: https://github.com/scala/scala/pull/2340

### 型を司るもの、カインド

[すごいHaskellたのしく学ぼう](http://www.amazon.co.jp/dp/4274068854) 曰く:

> 型とは、値について何らかの推論をするために付いている小さなラベルです。そして、型にも小さなラベルが付いているんです。その名は**種類** (kind)。
> ...
> 種類とはそもそも何者で、何の役に立つのでしょう？さっそく GHCi の `:k` コマンドを使って、型の種類を調べてみましょう。

Scala 2.10 時点では Scala REPL に `:k` コマンドが無かったので、ひとつ書いてみた: [kind.scala](https://gist.github.com/eed3si9n/3610635)。
George Leontiev 氏 ([@folone](https://twitter.com/folone)) その他のお陰で、Scala 2.11 より `:kind` コマンドは標準機能として取り込まれた。使ってみよう:

```scala
scala> :k Int
scala.Int's kind is A

scala> :k -v Int
scala.Int's kind is A
*
This is a proper type.
```

`Int` と他の全ての値を作ることのできる型はプロパーな型と呼ばれ `*` というシンボルで表記される (「型」と読む)。これは値レベルだと `1` に相当する。Scala の型変数構文を用いるとこれは `A` と書ける。

```scala
scala> :k -v Option
scala.Option's kind is F[+A]
* -(+)-> *
This is a type constructor: a 1st-order-kinded type.

scala> :k -v Either
scala.util.Either's kind is F[+A1,+A2]
* -(+)-> * -(+)-> *
This is a type constructor: a 1st-order-kinded type.
```

これらは、**型コンストラクタ**と呼ばれる。別の見方をすると、これらはプロパーな型から1ステップ離れている型だと考えることもできる。
これは値レベルだと、1階値、つまり普通関数と呼ばれる `(_: Int) + 3` などに相当する。

カリー化した表記法を用いて `* -> *` や `* -> * -> *` などと書く。このとき `Option[Int]` は `*` で、`Option` が `* -> *` であることに注意。Scala の型変数構文を用いるとこれらは `F[+A]`、 `F[+A1,+A2]` となる。

```scala
scala> :k -v Eq
algebra.Eq's kind is F[A]
* -> *
This is a type constructor: a 1st-order-kinded type.
```

Scala は型クラスという概念を型コンストラクタを用いてエンコード (悪く言うとコンプレクト) する。
これを見たとき、`Eq` は `A` (つまりプロパーな型) の型クラスだと思ってほしい。
`Eq` には `Int` などを渡すので、これは理にかなっている。

```scala
scala> :k -v Functor
cats.Functor's kind is X[F[A]]
(* -> *) -> *
This is a type constructor that takes type constructor(s): a higher-kinded type.
```

繰り返すが、Scala は型クラスを型コンストラクタを用いてエンコードするため、
これを見たとき、`Functor` は `F[A]` (つまり、型コンストラクタ) のための型クラスだと思ってほしい。
`Functor` には `List` などを渡すので、これも理にかなっている。

別の言い方をすると、これは型コンストラクタを受け取る型コンストラクタだ。
これは値レベルだと高階関数に相当するもので、**高カインド型** (higher-kinded type) と呼ばれる。
これらは `(* -> *) -> *` と表記される。Scala の型変数構文を用いるとこれは `X[F[A]]` と書ける。

### forms-a vs is-a

型クラス関連の用語は混用されやすい。
例えば、`(Int, +)` のペアはモノイドという型クラスを形成する。
口語的には、「なんらかの演算に関して X はモノイドを形成できるか? (can X form a monoid under some operation?)
という意味で「is X a monoid?」と言ったりする。

この例は、昨日の説明で、暗に `Either[A, B]` はファンクターである ("is-a") という説明になっていたはずだ。
実用的では無いかもしれないが、左バイアスのかかったファンクターを定義することは**可能である**ため、これは正確ではないと言える。
