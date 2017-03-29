---
out: import-guide.html
---

  [ImplicitsSource]: $catsBaseUrl$/core/src/main/scala/cats/implicits/package.scala

### import ガイド

Cats は implicit を使い倒している。ライブラリを使う側としても、拡張する側としても何がどこから来てるかという一般的な勘を作っていくのは大切だ。
ただし、Cats を始めたばかりの頃はとりあえず以下の import を使ってこのページは飛ばしても大丈夫だと思う:

```scala
scala> import cats._, cats.data._, cats.implicits._
```

### implicit のまとめ

Scala の import と implicit を手早く復習しよう! Scala では import は 2つの目的で使われる:

1. 値や型の名前をスコープに取り込むため。
2. implicit をスコープに取り込むため。

implicit には僕が考えられる限り 4つの使い方がある:

1. 型クラスインスタンスを提供するため。
2. メソッドや演算子を注入するため。(静的モンキーパッチ)
3. 型制約を宣言するため。
4. 型の情報をコンパイラから取得するため。

implicit は以下の優先順位で選択される:

1. プレフィックス無しでアクセスできる暗黙の値や変換子。ローカル宣言、import、外のスコープ、継承、および現在のパッケージオブジェクトから取り込まれる。同名の暗黙の値があった場合は内側のスコープのものが外側のものを shadow する。
2. 暗黙のスコープ。型、その部分、および親型のコンパニオンオブジェクトおよびパッケージオブジェクト内で宣言された暗黙の値や変換子。

### import cats._

まずは `import cats._` で何が import されるのかみてみよう。

まずは、名前だ。`Show[A]` や `Functor[F[_]]` のような型クラスは trait として実装されていて、`cats` パッケージ内で定義されている。だから、`cats.Show[[A]` と書くかわりに `Show[A]` と書ける。

次も、名前だけど、これは型エイリアス。`cats` のパッケージオブジェクトは `Eq[A]` や `~>[F[_], G[_]]` のような主な型エイリアスを宣言する。これも `cats.Eq[A]` というふうにアクセスすることができる。

最後に、`Id[A]` の `Traverse[F[_]]` や `Monad[F[_]]` その他への型クラスインスタンスとして `catsInstancesForId` が定義されているけど、気にしなくてもいい。パッケージオブジェクトに入っているというだけで暗黙のスコープに入るので、これは import しても結果は変わらない。確かめてみよう:

```scala
scala> cats.Functor[cats.Id]
res0: cats.Functor[cats.Id] = cats.package\$\$anon\$1@3c201c09
```

import は必要なしということで、うまくいった。つまり、`import cats._` の効果はあくまで便宜のためであって、省略可能だ。

### import cats.data._

次に `import cats.data._` で何が取り込まれるか見ていく。

まずは、これも名前だ。`cats.data` パッケージ以下には `Validated[+E, +A]` のようなカスタムデータ型が定義されている。

次に、型エイリアス。`cats.data` のパッケージオブジェクト内には `Reader[A, B]` (`ReaderT` モナド変換子を特殊化したものという扱い) のような型エイリアスが定義してある。

### import cats.implicits._

だとすると、`import cats.implicits._` は一体何をやっているんだろう? 以下が [implicits オブジェクト][ImplicitsSource]の定義だ:

```scala
package cats

object implicits extends syntax.AllSyntax with instances.AllInstances
```

これは import をまとめるのに便利な方法だ。`implicits` object そのものは何も定義せずに、trait をミックスインしている。以下にそれぞれの trait を詳しくみていくけど、飲茶スタイルでそれぞれ別々に import することもできる。フルコースに戻ろう。

#### cats.instances.AllInstances

これまでの所、僕は意図的に型クラスインスタンスという概念とメソッド注入 (別名 enrich my library) という概念をあたかも同じ事のように扱ってきた。だけど、`(Int, +)` が `Monoid` を形成することと、`Monoid` が `|+|` 演算子を導入することは 2つの異なる事柄だ。

Cats の設計方針で興味深いことの 1つとしてこれらの概念が徹底して "instance" (インスタンス) と "syntax" (構文) として区別されていることが挙げられる。たとえどれだけ一部のユーザにとって論理的に筋が通ったとしても、ライブラリがシンボルを使った演算子を導入すると議論の火種となる。 sbt、dispatch、specs などのライブラリやツールはそれぞれ独自の DSL を導入し、それらの効用に関して何度も議論が繰り広げられた。

`AllInstances` は、`Either[A, B]` や `Option[A]` といった標準のデータ型に対する型クラスのインスタンスのミックスインだ。

```scala
package cats
package instances

trait AllInstances
  extends FunctionInstances
  with    StringInstances
  with    EitherInstances
  with    ListInstances
  with    OptionInstances
  with    SetInstances
  with    StreamInstances
  with    VectorInstances
  with    AnyValInstances
  with    MapInstances
  with    BigIntInstances
  with    BigDecimalInstances
  with    FutureInstances
  with    TryInstances
  with    TupleInstances
  with    UUIDInstances
  with    SymbolInstances
```

#### cats.syntax.AllSyntax

`AllSyntax` は、Cats 内にある全ての演算子をミックスインする trait だ。

```scala
package cats
package syntax

trait AllSyntax
    extends ApplicativeSyntax
    with ApplicativeErrorSyntax
    with ApplySyntax
    with BifunctorSyntax
    with BifoldableSyntax
    with BitraverseSyntax
    with CartesianSyntax
    with CoflatMapSyntax
    with ComonadSyntax
    with ComposeSyntax
    with ContravariantSyntax
    with CoproductSyntax
    with EitherSyntax
    with EqSyntax
    ....
```

### アラカルト形式

僕は、飲茶スタイルという名前の方がいいと思うけど、カートで点心が運ばれてきて好きなものを選んで取る「飲茶」でピンと来なければ、カウンターに座って好きなものを頼む焼き鳥屋だと考えてもいい。

もし何らかの理由で `cats.implicits._` を全て import したくなければ、好きなものを選ぶことができる。

#### 型クラスインスタンス

型クラスはデータ型ごとに分かれている。以下が `Option` のための全ての型クラスインスタンスを導入する方法だ:

```console:new
scala> {
         import cats.instances.option._
         cats.Monad[Option].pure(0)
       }
```

全てのインスタンスが欲しければ、以下が全て取り込む方法だ:

```console
scala> {
         import cats.instances.all._
         cats.Monoid[Int].empty
       }
```

演算子の注入を一切行なっていないので、ヘルパー関数や型クラスインスタンスに定義された関数を使う必要がある (そっちの方が好みという人もいる)。

#### Cats 型クラスの syntax

型クラスの syntax は型クラスごとに分かれている。以下が `Eq` のためのメソッドや演算子を注入する方法だ:

```console
scala> {
          import cats.syntax.eq._
          import cats.instances.all._
          1 === 1
       }
```

#### Cats データ型の syntax

`Writer` のような Cats 独自のデータ型のための syntax も `cats.syntax` パッケージ以下にある:

```console
scala> {
          import cats.syntax.writer._
          import cats.instances.all._
          1.tell
       }
```

#### 標準データ型の syntax

標準データ型のための sytnax はデータ型ごとに分かれている。以下が `Option` のための演算子とヘルパー関数を注入する方法だ:

```console
scala> {
          import cats.syntax.option._
          import cats.instances.all._
          1.some
       }
```

#### 全ての syntax

以下は全ての syntax と型クラスインスタンスを取り込む方法だ。

```console
scala> {
          import cats.syntax.all._
          import cats.instances.all._
          1.some
       }
```

これは `cats.implicits._` を import するのと同じだ。
繰り返すが、これを読んで分からなかったら、まずは以下を使っていれば大丈夫だ:

```scala
scala> import cats._, cats.data._, cats.implicits._
```
