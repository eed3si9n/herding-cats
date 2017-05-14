---
out: method-injection.html
---

### メソッド注入 (enrich my library)

> `Monoid` を使ってある型の 2つの値を足す関数を書いた場合、このようになります。

```console
scala> def plus[A: Monoid](a: A, b: A): A = implicitly[Monoid[A]].mappend(a, b)
scala> plus(3, 4)
```

これに演算子を提供したい。だけど、1つの型だけを拡張するんじゃなくて、`Monoid` のインスタンスを持つ全ての型を拡張したい。
Simulacrum を用いて Cats スタイルでこれを行なってみる。

```console:new
scala> import simulacrum._
scala> :paste
@typeclass trait Monoid[A] {
  @op("|+|") def mappend(a: A, b: A): A
  def mzero: A
}
object Monoid {
  // "ops" gets generated
  val syntax = ops
  implicit val IntMonoid: Monoid[Int] = new Monoid[Int] {
    def mappend(a: Int, b: Int): Int = a + b
    def mzero: Int = 0
  }
  implicit val StringMonoid: Monoid[String] = new Monoid[String] {
    def mappend(a: String, b: String): String = a + b
    def mzero: String = ""
  }
}
scala> import Monoid.syntax._
scala> 3 |+| 4
scala> "a" |+| "b"
```

1つの定義から `Int` と `String` の両方に `|+|` 演算子を注入することができた。

### 標準データ型に対する演算子構文

このテクニックを使って、Cats はごくたまに `Option` のような標準ライブラリデータ型へのメソッド注入も提供する:

```console
scala> import cats._, cats.data._, cats.implicits._
scala> 1.some
scala> 1.some.orEmpty
```

しかし、Cats の演算子の大半は型クラスに関連付けられている。

これで Cats の雰囲気がつかめてもらえただろうか。
