---
out: monads-are-fractals.html
---

  [day5]: day5.html
  [day7]: day7.html

### モナドはフラクタルだ

[5日目][day5]に綱渡りの例を使って得られた直観は、
`>>=` を使ったモナディックなチェインはある演算から次の演算へとコンテキストを引き渡すということだった。
中間値に 1つでも `None` があっただけで、チェイン全体が島流しとなる。


引き渡されるコンテキストはモナドのインスタンスによって異なる。
例えば、[7日目][day7]にみた `State` データ型は、`>>=`
によって状態オブジェクトの明示的な引き渡しを自動化する。

これはモナドを `Functor`、`Apply` や `Applicative`
と比較したときに有用な直観だけどもストーリーとしては全体像を語らない。

![sierpinski triangle](files/day9-sierpinski.png)

モナド (正確には `FlatMap`) に関するもう1つの直観は、
これらがシェルピンスキーの三角形のようなフラクタルであることだ。
フラクタルの個々の部分が全体の形の自己相似となっている。

例えば、`List` を例にとる。複数の `List` の `List` は、単一のフラットな `List` として取り扱うことができる。

```console:new
scala> val xss = List(List(1), List(2, 3), List(4))
scala> xss.flatten
```

この `flatten` 関数は `List` データ構造の押し潰しを体現する。
型シグネチャで考えると、これは `F[F[A]] => F[A]` だと言える。

#### List は ++ に関してモナドを形成する

平坦化を `foldLeft` を使って再実装することで、より良い理解を得ることができる:

```console
scala> xss.foldLeft(List(): List[Int]) { _ ++ _ }
```

これによって `List` は `++` に関してモナドを形成すると言うことができる。

#### じゃあ、Option は何に関するモナド?

次に、どの演算に関して `Option` はモナドを形成しているのが考えてみる:

```console
scala> val o1 = Some(None: Option[Int]): Option[Option[Int]]
scala> val o2 = Some(Some(1): Option[Int]): Option[Option[Int]]
scala> val o3 = None: Option[Option[Int]]
```

`foldLeft` で書いてみる:

```console
scala> o1.foldLeft(None: Option[Int]) { (_, _)._2 }
scala> o2.foldLeft(None: Option[Int]) { (_, _)._2 }
scala> o3.foldLeft(None: Option[Int]) { (_, _)._2 }
```

`Option` は `(_, _)._2` に関してモナドを形成しているみたいだ。

#### フラクタルとしての State

フラクタルという視点から `State` データ型に関してもう一度考えてみると、
`State` の `State` がやはり `State` であることは明らかだ。
この特性を利用することで、`pop` や `push` といったミニ・プログラムを書いて、
それらを `for` 内包表記を用いてより大きな `State` に合成するといったことが可能となる:

```scala
def stackManip: State[Stack, Int] = for {
  _ <- push(3)
  a <- pop
  b <- pop
} yield(b)
```

このような合成は自由モナドでもみた。

つまり、同じモナド・インスタンスの中では**モナディック値**は合成することができる。

#### フラクタルを探しだす

独自のモナドを発見したいと思ったら、フラクタル構造に気をつけるべきだ。
見つけたら `flatten` 関数 `F[F[A]] => F[A]` を実装できるか確かめてみよう。
