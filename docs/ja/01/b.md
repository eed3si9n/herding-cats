
  [algebra]: https://github.com/non/algebra

## Eq

LYAHFGG:

> `Eq` は等値性をテストできる型に使われます。Eq のインスタンスが定義すべき関数は `==` と `/=` です。

Cats で `Eq` 型クラスと同じものも `Eq` と呼ばれている。
細かい点を言うと、`cats.Eq` は実は [non/algebra][algebra] の `algebra.Eq` の型エイリアスだ。
これがどういう影響を及ぼすかは未だ分からないけども、多分再利用してるのはいいことだろうと思う:

```console
scala> import cats._, cats.std.all._, cats.syntax.eq._
scala> 1 === 1
scala> 1 === "foo"
scala> 1 == "foo"
scala> (Some(1): Option[Int]) =!= (Some(2): Option[Int])
```

標準の `==` のかわりに、`Eq` は `===` と `=!=` 演算を可能とする。主な違いは `Int` と `String` と比較すると `===` はコンパイルに失敗することだ。
