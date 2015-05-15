
### Monad

先ほど Cats はモナド型クラスを `FlatMap` と `Monad` の2つに分けると書いた。
この `FlatMap`-`Monad` の関係は、`Apply`-`Applicative` の関係の相似となっている:

```scala
@typeclass trait Monad[F[_]] extends FlatMap[F] with Applicative[F] {
  ....
}
```

`Monad` は　`FlatMap` に `pure` を付けたものだ。Haskell と違って `Monad[F]` は `Applicative[F]` を拡張するため、`return` と `pure` と名前が異なるという問題が生じていない。

#### 綱渡り

<div class="floatingimage">
<img src="../files/day5-with-birds.jpg">
<div class="credit">Derived from <a href="https://www.flickr.com/photos/72562013@N06/10016847314/">Bello Nock's Sky Walk</a> by Chris Phutully</div>
</div>

LYAHFGG:

> さて、棒の左右にとまった鳥の数の差が3以内であれば、ピエールはバランスを取れているものとしましょう。例えば、右に1羽、左に4羽の鳥がとまっているなら大丈夫。だけど左に5羽目の鳥がとまったら、ピエールはバランスを崩して飛び降りる羽目になります。

本の `Pole` の例題を実装してみよう。

```console:new
scala> import cats._, cats.std.all._, cats.syntax.flatMap._
scala> :paste
object Catnip {
  implicit class IdOp[A](val a: A) extends AnyVal {
    def some: Option[A] = Some(a)
  }
  def none[A]: Option[A] = None
}
import Catnip._
scala> type Birds = Int
scala> case class Pole(left: Birds, right: Birds)
```

Scala ではこんな風に `Int` に型エイリアスを付けるのは一般的じゃないと思うけど、ものは試しだ。`landLeft` と `landRight` をメソッドをとして実装したいから `Pole` は case class にする:

```console
scala> :paste
case class Pole(left: Birds, right: Birds) {
  def landLeft(n: Birds): Pole = copy(left = left + n)
  def landRight(n: Birds): Pole = copy(right = right + n)
}
```

OO の方が見栄えが良いと思う:

```console
scala> Pole(0, 0).landLeft(2)
scala> Pole(1, 2).landRight(1)
scala> Pole(1, 2).landRight(-1)
```

連鎖も可能:

```console
scala> Pole(0, 0).landLeft(1).landRight(1).landLeft(2)
scala> Pole(0, 0).landLeft(1).landRight(4).landLeft(-1).landRight(-2)
```

本が言うとおり、中間値で失敗しても計算が続行してしまっている。失敗を `Option[Pole]` で表現しよう:

```console
scala> :paste
case class Pole(left: Birds, right: Birds) {
  def landLeft(n: Birds): Option[Pole] =
    if (math.abs((left + n) - right) < 4) copy(left = left + n).some
    else none[Pole]
  def landRight(n: Birds): Option[Pole] =
    if (math.abs(left - (right + n)) < 4) copy(right = right + n).some
    else none[Pole]
  }
scala> Pole(0, 0).landLeft(2)
scala> Pole(0, 3).landLeft(10)
```

`flatMap` もしくはシンボル使ったエイリアスである `>>=` を使って `landLeft` と `landRight` をチェインする:

```console
scala> val rlr = Monad[Option].pure(Pole(0, 0)) >>= {_.landRight(2)} >>=
  {_.landLeft(2)} >>= {_.landRight(2)}
```

モナディックチェインが綱渡りのシミュレーションを改善したか確かめる:

```console
scala> val lrlr = Monad[Option].pure(Pole(0, 0)) >>= {_.landLeft(1)} >>=
  {_.landRight(4)} >>= {_.landLeft(-1)} >>= {_.landRight(-2)}
```


うまくいった。この例はモナドが何なのかをうまく体現しているので、じっくり考えて理解してほしい。

1. まず、`pure` が `Pole(0, 0)` をデフォルトのコンテクストで包む: `Pole(0, 0).some`
2. 次に、`Pole(0, 0).some >>= {_.landLeft(1)}` が起こる。これは `Some`値なので、`Pole(0, 0)` に `_.landLeft(1)` が適用されて、`Pole(1, 0).some` が返ってくる。
3. 次に、`Pole(1, 0).some >>= {_.landRight(4)}` が起こる。結果は `Pole(1, 4).some`。これでバランス棒の左右の差の最大値となった。
4. `Pole(1, 4).some >>= {_.landLeft(-1)}` が発生して、`none[Pole]` が返ってくる。差が大きすぎて、バランスが崩れてしまった。
5. `none[Pole] >>= {_.landRight(-2)}` は自動的に `none[Pole]` となる。

モナディックな関数をチェインでは、一つの関数の効果 (effect) が次々と渡されていくのが見えると思う。

#### ロープの上のバナナ

LYAHFGG:

> さて、今度はバランス棒にとまっている鳥の数によらず、いきなりピエールを滑らせて落っことす関数を作ってみましょう。この関数を `banana` と呼ぶことにします。

以下が常に失敗する `banana` だ:

```console
scala> :paste
case class Pole(left: Birds, right: Birds) {
  def landLeft(n: Birds): Option[Pole] =
    if (math.abs((left + n) - right) < 4) copy(left = left + n).some
    else none[Pole]
  def landRight(n: Birds): Option[Pole] =
    if (math.abs(left - (right + n)) < 4) copy(right = right + n).some
    else none[Pole]
  def banana: Option[Pole] = none[Pole]
}
scala> val lbl = Monad[Option].pure(Pole(0, 0)) >>= {_.landLeft(1)} >>=
  {_.banana} >>= {_.landRight(1)}
```

LYAHFGG:

> ところで、入力に関係なく既定のモナド値を返す関数だったら、自作せずとも `>>` 関数を使うという手があります。

以下が `>>` の `Option` での振る舞い:

```console
scala> none[Int] >> 3.some
scala> 3.some >> 4.some
scala> 3.some >> none[Int]
```

`banana` を `>> none[Pole]` に置き換えてみよう:

```console
scala> val lbl = Monad[Option].pure(Pole(0, 0)) >>= {_.landLeft(1)} >>
  none[Pole] >>= {_.landRight(1)}
```

突然型推論が崩れてしまった。問題の原因はおそらく演算子の優先順位にある。 [Programming in Scala](http://www.artima.com/pins1ed/basic-types-and-operations.html) 曰く:

> The one exception to the precedence rule, alluded to above, concerns assignment operators, which end in an equals character. If an operator ends in an equals character (`=`), and the operator is not one of the comparison operators `<=`, `>=`, `==`, or `!=`, then the precedence of the operator is the same as that of simple assignment (`=`). That is, it is lower than the precedence of any other operator.

注意: 上記の記述は不完全だ。代入演算子ルールのもう1つの例外は演算子が `===` のように (`=`) から始まる場合だ。

`>>=` (bind) が等号で終わるため、優先順位は最下位に落とされ、`({_.landLeft(1)} >> (none: Option[Pole]))` が先に評価される。いくつかの気が進まない回避方法がある。まず、普通のメソッド呼び出しのようにドットと括弧の記法を使うことができる:

```console
scala> Monad[Option].pure(Pole(0, 0)).>>=({_.landLeft(1)}).>>(none[Pole]).>>=({_.landRight(1)})
```

もしくは優先順位の問題に気付いたなら、適切な場所に括弧を置くことができる:

```console
scala> (Monad[Option].pure(Pole(0, 0)) >>= {_.landLeft(1)}) >> none[Pole] >>= {_.landRight(1)}
```

両方とも正しい答が得られた。

#### for 内包表記

LYAHFGG:

> Haskell にとってモナドはとても便利なので、モナド専用構文まで用意されています。その名は `do` 記法。

まずは入れ子のラムダ式を書いてみよう:

```console
scala> import cats._, cats.syntax.show._
scala> 3.some >>= { x => "!".some >>= { y => (x.show + y).some } }
```

`>>=` が使われたことで計算のどの部分も失敗することができる:

```console
scala> 3.some >>= { x => none[String] >>= { y => (x.show + y).some } }
scala> (none: Option[Int]) >>= { x => "!".some >>= { y => (x.show + y).some } }
scala> 3.some >>= { x => "!".some >>= { y => none[String] } }
```

Haskell の `do` 記法のかわりに、Scala には `for` 内包表記があり、これらは似た機能を持つ:

```console
scala> for {
         x <- 3.some
         y <- "!".some
       } yield (x.show + y)
```

LYAHFGG:

> `do` 式は、`let` 行を除いてすべてモナド値で構成されます。

これは `for` では微妙に違うと思うけど、また今度。

#### 帰ってきたピエール

LYAHFGG:

> ピエールの綱渡りの動作も、もちろん `do` 記法で書けます。

```console
scala> def routine: Option[Pole] =
         for {
           start <- Monad[Option].pure(Pole(0, 0))
           first <- start.landLeft(2)
           second <- first.landRight(2)
           third <- second.landLeft(1)
         } yield third
scala> routine
```

`yield` は `Option[Pole]` じゃなくて `Pole` を受け取るため、`third` も抽出する必要があった。

LYAHFGG:

> ピエールにバナナの皮を踏ませたい場合、`do` 記法ではこう書きます。

```console
scala> def routine: Option[Pole] =
         for {
           start <- Monad[Option].pure(Pole(0, 0))
           first <- start.landLeft(2)
           _ <- none[Pole]
           second <- first.landRight(2)
           third <- second.landLeft(1)
         } yield third
scala> routine
```

#### パターンマッチングと失敗

LYAHFGG:

> `do` 記法でモナド値を変数名に束縛するときには、`let` 式や関数の引数のときと同様、パターンマッチが使えます。

```console
scala> def justH: Option[Char] =
         for {
           (x :: xs) <- "hello".toList.some
         } yield x
scala> justH
```

> `do` 式の中でパターンマッチが失敗した場合、`Monad` 型クラスの一員である `fail` 関数が使われるので、異常終了という形ではなく、そのモナドの文脈に合った形で失敗を処理できます。

```console
scala> def wopwop: Option[Char] =
         for {
           (x :: xs) <- "".toList.some
         } yield x
scala> wopwop
```

失敗したパターンマッチングは `None` を返している。これは `for` 構文の興味深い一面で、今まで考えたことがなかったが、言われるとなるほどと思う。

#### Monad則

モナドには 3つの法則がある:

- 結合律 (associativity): `(m flatMap f) flatMap g === m flatMap { x => f(x) flatMap {g} }`
- 左単位元 (left identity): `(Monad[F].pure(x) flatMap {f}) === f(x)`
- 右単位元 (right identity): `(m flatMap {Monad[F].pure(_)}) === m`

4日目の Monoid則を覚えていると、見覚えがあるかもしれない。
それは、モナドはモノイドの特殊な形だからだ。

「ちょっと待て。`Monoid` は `A` (別名 `*`) のカインドのためのものじゃないのか?」と思うかもしれない。
確かにその通り。そして、これが「モノイド」と `Monoid[A]` の差でもある。
Haskell スタイルの関数型プログラミングはコンテナや実行モデルを抽象化することができる。
圏論では、モノイドといった概念は `A`、`F[A]`、`F[A] => F[B]` といった色んなものに一般化することができる。
「オーマイガー。法則多杉」と思うよりも、多くの法則はそれらをつなぐ基盤となる構造があるということを知ってほしい。

Discipline を使った Monad則の検査はこうなる:

```scala
scala> import cats._, cats.std.all._, cats.laws.discipline.MonadTests
import cats._
import cats.std.all._
import cats.laws.discipline.MonadTests

scala> val rs = MonadTests[Option].monad[Int, Int, Int]
rs: cats.laws.discipline.MonadTests[Option]#RuleSet = cats.laws.discipline.MonadTests\$\$anon\$2@35e8de37

scala> rs.all.check
+ monad.applicative homomorphism: OK, passed 100 tests.
+ monad.applicative identity: OK, passed 100 tests.
+ monad.applicative interchange: OK, passed 100 tests.
+ monad.applicative map: OK, passed 100 tests.
+ monad.apply composition: OK, passed 100 tests.
+ monad.covariant composition: OK, passed 100 tests.
+ monad.covariant identity: OK, passed 100 tests.
+ monad.flatMap associativity: OK, passed 100 tests.
+ monad.flatMap consistent apply: OK, passed 100 tests.
+ monad.invariant composition: OK, passed 100 tests.
+ monad.invariant identity: OK, passed 100 tests.
+ monad.monad left identity: OK, passed 100 tests.
+ monad.monad right identity: OK, passed 100 tests.
```
