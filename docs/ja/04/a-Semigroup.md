---
out: Semigroup.html
---

  [clwd]: checking-laws-with-discipline.html
  [fafm]: http://learnyouahaskell.com/functors-applicative-functors-and-monoids

### Semigroup

「すごいHaskellたのしく学ぼう」の本を持ってるひとは新しい章に進める。モノイドだ。ウェブサイトを読んでるひとは [Functors, Applicative Functors and Monoids][fafm] の続きだ。

とりあえず、Cats には `newtype` や tagged type 的な機能は入ってないみたいだ。
後で自分たちで実装することにする。

Haskell の `Monoid` は、Cats では `Semigroup` と `Monoid` に分かれている。
これらはそれぞれ `algebra.Semigroup` と `algebra.Monoid` の型エイリアスだ。
`Apply` と `Applicative` 同様に、`Semigroup` は `Monoid` の弱いバージョンだ。
同じ問題を解く事ができるなら、より少ない前提を置くため弱い方がかっこいい。

LYAHFGG:

LYAHFGG:

> 例えば、`(3 * 4) * 5` も `3 * (4 * 5)` も、答は `60` です。`++` についてもこの性質は成り立ちます。
> ...
>
> この性質を**結合的** (associativity) と呼びます。演算 `*` と `++` は結合的であると言います。結合的でない演算の例は `-` です。

確かめてみる:

```scala mdoc
import cats._, cats.syntax.all._

assert { (3 * 2) * (8 * 5) === 3 * (2 * (8 * 5)) }

assert { List("la") ++ (List("di") ++ List("da")) === (List("la") ++ List("di")) ++ List("da") }
```

エラーがないから等価ということだ。

#### Semigroup 型クラス

これが `algebra.Semigroup` の型クラスコントラクトだ。

```scala
/**
 * A semigroup is any set `A` with an associative operation (`combine`).
 */
trait Semigroup[@sp(Int, Long, Float, Double) A] extends Any with Serializable {

  /**
   * Associative operation taking which combines two values.
   */
  def combine(x: A, y: A): A

  ....
}
```

これは `combine` 演算子とそのシンボルを使ったエイリアスである `|+|` を可能とする。使ってみる。

```scala mdoc
List(1, 2, 3) |+| List(4, 5, 6)

"one" |+| "two"
```

#### Semigroup則

結合則が semigroup の唯一の法則だ。

- associativity `(x |+| y) |+| z = x |+| (y |+| z)`

以下は、Semigroup則を REPL から検査する方法だ。
詳細は[Discipline を用いた法則のチェック][clwd]を参照。

```scala
scala> import cats._, cats.data._, cats.implicits._
import cats._
import cats.data._
import cats.implicits._

scala> import cats.kernel.laws.GroupLaws
import cats.kernel.laws.GroupLaws

scala> val rs1 = GroupLaws[Int].semigroup(Semigroup[Int])
rs1: cats.kernel.laws.GroupLaws[Int]#GroupProperties = cats.kernel.laws.GroupLaws\$GroupProperties@5a077d1d

scala> rs1.all.check
+ semigroup.associativity: OK, passed 100 tests.
+ semigroup.combineN(a, 1) == a: OK, passed 100 tests.
+ semigroup.combineN(a, 2) == a |+| a: OK, passed 100 tests.
+ semigroup.serializable: OK, proved property.
```

#### Semigroups としての List

```scala mdoc
List(1, 2, 3) |+| List(4, 5, 6)
```

#### 積と和

`Int` は、`+` と `*` の両方に関して semigroup を形成することができる。
Tagged type の代わりに、cats は加算に対してにのみ
semigroup のインスタンスを提供するという方法をとっている。

これを演算子構文で書くのはトリッキーだ。

```scala mdoc
def doSomething[A: Semigroup](a1: A, a2: A): A =
  a1 |+| a2

doSomething(3, 5)(Semigroup[Int])
```

これなら、関数構文で書いたほうが楽かもしれない:

```scala mdoc
Semigroup[Int].combine(3, 5)
```
