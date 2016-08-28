
  [algebra]: https://github.com/non/algebra
  [Equivalence]: http://en.wikipedia.org/wiki/Equivalence_relation

## Eq

LYAHFGG:

> `Eq` は等値性をテストできる型に使われます。Eq のインスタンスが定義すべき関数は `==` と `/=` です。

Cats で `Eq` 型クラスと同じものも `Eq` と呼ばれている。
<s>細かい点を言うと、`cats.Eq` は実は [non/algebra][algebra] の `algebra.Eq` の型エイリアスだ。
これがどういう影響を及ぼすかは未だ分からないけども、多分再利用してるのはいいことだろうと思う</s>
`Eq` は [non/algebra][algebra] から cats-kernel というサブプロジェクトに移行して、Cats の一部になった:

```console:error
scala> import cats._, cats.instances.all._, cats.syntax.eq._
scala> 1 === 1
scala> 1 === "foo"
scala> 1 == "foo"
scala> (Some(1): Option[Int]) =!= (Some(2): Option[Int])
```

標準の `==` のかわりに、`Eq` は `===` と `=!=` 演算を可能とする。主な違いは `Int` と `String` と比較すると `===` はコンパイルに失敗することだ。

algebra では `neqv` は `eqv` に基いて実装されている。

```scala
/**
 * A type class used to determine equality between 2 instances of the same
 * type. Any 2 instances `x` and `y` are equal if `eqv(x, y)` is `true`.
 * Moreover, `eqv` should form an equivalence relation.
 */
trait Eq[@sp A] extends Any with Serializable { self =>

  /**
   * Returns `true` if `x` and `y` are equivalent, `false` otherwise.
   */
  def eqv(x: A, y: A): Boolean

  /**
   * Returns `false` if `x` and `y` are equivalent, `true` otherwise.
   */
  def neqv(x: A, y: A): Boolean = !eqv(x, y)

  ....
}
```

これは多相性 (polymorphism) の例だ。型の `A` にとって等価性が何を意味しようと、
`neqv` はその逆だと定義されている。それが `String` でも `Int` でも変わらない。
別の言い方をすれば、`Eq[A]` が与えられたとき、`===` は普遍的に `=!=` の逆だ。

気になるのが、`Eq` では等価 (equal) と同値 (equivalent) を同じように使っているフシがあることだ。
[同値関係][Equivalence]は例えば、「同じ誕生日を持つ」関係も含むのに対して、
等価性は代入原理を要請する。
