---
out: List.html
---

### List データ型

LYAHFGG:

> 一方、`[3,8,9]` のような値は複数の計算結果を含んでいるとも、複数の候補値を同時に重ね合わせたような1つの値であるとも解釈できます。リストをアプリカティブ・スタイルで使うと、非決定性を表現していることがはっきりします。

まずは Applicative としての `List` を復習する:

```scala mdoc
import cats._, cats.syntax.all._

(List(1, 2, 3), List(10, 100, 100)) mapN { _ * _ }
```

> それでは、非決定的値を関数に食わせてみましょう。

```scala mdoc
List(3, 4, 5) >>= { x => List(x, -x) }
```

モナディックな視点に立つと、`List` というコンテキストは複数の解がありうる数学的な値を表す。それ以外は、`for` を使って `List` を操作するなどは素の Scala と変わらない:

```scala mdoc
for {
  n <- List(1, 2)
  ch <- List('a', 'b')
} yield (n, ch)
```
