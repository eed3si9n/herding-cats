---
out: Coproduct.html
---

  [@milessabin]: https://twitter.com/milessabin
  [scala-union-types]: http://www.chuusai.com/2011/06/09/scala-union-types-curry-howard/
  [alacarte]: http://www.cs.ru.nl/~W.Swierstra/Publications/DataTypesALaCarte.pdf
  [@wouterswierstra]: https://twitter.com/wouterswierstra

### 余積

双対としてよく知られているものに、積の双対である**余積** (coproduct、「直和」とも) がある。双対を表すのに英語では頭に "co-" を、日本語だと「余」を付ける。

以下に積の定義を再掲する:

> **定義 2.15.** 任意の圏 **C** において、対象 A と B の積の図式は対象 P と射 p<sub>1</sub> と p<sub>2</sub> から構成され<br>
> ![product diagram](../files/day17-product-diagram.png)<br>
> 以下の UMP を満たす:
>
> この形となる任意の図式があるとき<br>
> ![product definition](../files/day17-product-definition.png)<br>
> 次の図式<br>
> ![product of objects](../files/day17-product-of-objects.png)<br>
> が可換となる (つまり、x<sub>1</sub> = p<sub>1</sub> u かつ x<sub>2</sub> = p<sub>2</sub> u が成立する) 一意の射 u: X => P が存在する。

矢印をひっくり返すと余積図式が得られる:<br>
![coproducts](../files/day17-coproducts.png)

余積は同型を除いて一意なので、余積は *A + B*、*u: A + B => X* の射は *[f, g]* と表記することができる。

> 「余射影」の *i<sub>1</sub>: A => A + B* と *i<sub>2</sub>: B => A + B* は、単射 ("injective") ではなくても「単射」 ("injection") という。

「埋め込み」(embedding) ともいうみたいだ。積が `scala.Product` などでエンコードされる直積型に関係したように、余積は直和型 (sum type, disjoint union type) と関連する。

#### 代数的データ型

*A + B* をエンコードする最初の方法は sealed trait と case class を使う方法だ。

```scala mdoc
sealed trait XList[A]

object XList {
  case class XNil[A]() extends XList[A]
  case class XCons[A](head: A, rest: XList[A]) extends XList[A]
}

XList.XCons(1, XList.XNil[Int])
```

#### 余積としての Either データ型

目をすくめて見ると `Either` を直和型だと考えることもできる。`Either` の型エイリアスとして `|:` を定義する:

```scala mdoc
type |:[+A1, +A2] = Either[A1, A2]
```

Scala は型コンストラクタに中置記法を使えるので、`Either[String, Int]` の代わりに `String |: Int` と書けるようになった。

```scala mdoc
val x: String |: Int = Right(1)
```

ここまでは普通の Scala 機能しか使っていない。Cats は単射 *i<sub>1</sub>: A => A + B* と *i<sub>2</sub>: B => A + B* を表す `cats.Injection` という型クラスを提供する。これを使うと Left と Right を気にせずに coproduct を作ることができる。

```scala mdoc
import cats._, cats.data._, cats.syntax.all._

val a = Inject[String, String |: Int].inj("a")

val one = Inject[Int, String |: Int].inj(1)
```

値を再取得するには `prj` を呼ぶ:

```scala mdoc
Inject[String, String |: Int].prj(a)

Inject[String, String |: Int].prj(one)
```

`apply` と `unapply` を使って書くときれいに見える:

```scala mdoc
lazy val StringInj = Inject[String, String |: Int]

lazy val IntInj = Inject[Int, String |: Int]

val b = StringInj("b")

val two = IntInj(2)

two match {
  case StringInj(x) => x
  case IntInj(x)    => x.show + "!"
}
```

`|:` にコロンを入れた理由は右結合にするためで、3つ以上の型を使うときに便利だからだ:

```scala mdoc
val three = Inject[Int, String |: Int |: Boolean].inj(3)
```

見ての通り、戻り値の型は `String |: (Int |: Boolean)` となった。

#### Curry-Howard エンコーディング

関連して [Miles Sabin (@milessabin)][@milessabin] さんの [Unboxed union types in Scala via the Curry-Howard isomorphism][scala-union-types] も興味深い。

#### Shapeless.Coproduct

Shapeless の [Coproducts and discriminated unions](https://github.com/milessabin/shapeless/wiki/Feature-overview:-shapeless-2.0.0#coproducts-and-discriminated-unions) も参考になる。

#### EitherK データ型

Cats には `EitherK[F[_], G[_], A]` というデータ型があって、これは型コンストラクタにおける Either だ。

[Data types à la carte][alacarte] で、[Wouter Swierstra (@wouterswierstra)][@wouterswierstra] さんがこれを使っていわゆる Expression Problem と呼ばれているものを解決できると解説している。

今日はここまで。
