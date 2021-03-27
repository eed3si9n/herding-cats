
### PartialOrder

`Order` の他に、Cats は `PartialOrder` も定義する。

```scala mdoc
import cats._, cats.data._, cats.implicits._

1 tryCompare 2

1.0 tryCompare Double.NaN
```

`PartialOrder` は `Option[Int]` を返す `tryCompare` 演算を可能とする。
algebra によると、オペランドが比較不能な場合は `None` を返すとのことだ。
だけど、`1.0` と `Double.NaN` を比較しても `Some(-1)` を返しているので、何が比較不能なのかは不明だ。

```scala mdoc
def lt[A: PartialOrder](a1: A, a2: A): Boolean = a1 <= a2
lt(1, 2)
```

```scala mdoc:fail
lt[Int](1, 2.0)
```

`PartialOrder` は他にも `>`, `>=`, `<`, そして `<=`
演算子を可能とするが、これらは気をつけないと標準の比較演算子を使うことになるのでトリッキーだ。
