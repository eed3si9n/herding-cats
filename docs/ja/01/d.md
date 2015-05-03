
### PartialOrder

`Order` の他に、Cats は `PartialOrder` も定義する。
これは `algebra.PartialOrder` の型エイリアスだ。

```console:new
scala> import cats._, cats.std.all._, cats.syntax.partialOrder._
scala> 1 tryCompare 2
scala> 1.0 tryCompare Double.NaN
```

`PartialOrder` は `Option[Int]` を返す `tryCompare` 演算を可能とする。
algebra によると、オペランドが比較不能な場合は `None` を返すとのことだ。
だけど、`1.0` と `Double.NaN` を比較しても `Some(-1)` を返しているので、何が比較不能なのかは不明だ。

```console
scala> def lt[A: PartialOrder](a1: A, a2: A): Boolean = a1 <= a2
scala> lt[Int](1, 2.0)
scala> lt(1, 2)
```

`PartialOrder` は他にも `>`, `>=`, `<`, そして `<=`
演算子を可能とするが、これらは気をつけないと標準の比較演算子を使うことになるのでトリッキーだ。
