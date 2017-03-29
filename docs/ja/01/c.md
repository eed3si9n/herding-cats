
### Order

LYAHFGG:

> `Ord` は、何らかの順序を付けられる型のための型クラスです。`Ord` はすべての標準的な大小比較関数、`>`、`<`、`>=`、 `<=` をサポートします。

Cats で `Ord` に対応する型クラスは `Order` だ。

```console:new,error
scala> import cats._, cats.data._, cats.implicits._
scala> 1 > 2.0
scala> 1 compare 2.0
scala> 1.0 compare 2.0
scala> 1.0 max 2.0
```

`Order` は `Int` (負、ゼロ、正) を返す `compare` 演算を可能とする。
また、`minx` と `max` 演算子も可能とする。
`Eq` 同様、`Int` と `Double` の比較はコンパイルを失敗させる。
