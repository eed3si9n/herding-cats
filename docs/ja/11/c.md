---
out: Const.html
---

  [Gibbons2006]: http://www.cs.ox.ac.uk/jeremy.gibbons/publications/dgp.pdf
  [iterator2009]: http://www.comlab.ox.ac.uk/jeremy.gibbons/publications/iterator.pdf

### Const データ型

[Datatype-Generic Programming][Gibbons2006] の第5章は
「Iterator パターンの本質」(The Essence of the Iterator pattern) と呼ばれていて、
Gibbons さんと Oliveira さんが 2006年に書いた論文と同じ題名だ。
現在公開されているバージョンの [The Essence of the Iterator Pattern][iterator2009]
は 2009年のものだ。DGP の流れをくんだものとしてこの論文を読むと、その文脈が分かるようになると思う。

この論文の冒頭に出てくる例を Java に翻訳してみた。

```java
public static <E> int loop(Collection<E> coll) {
  int n = 0;
  for (E elem: coll) {
    n = n + 1;
    doSomething(elem);
  }
  return n;
}
```

EIP:

> この *loop* メソッドや、これに似た反復には、
> 要素の**投射** (mapping)、
> そして同時にそれらの要素から得られる何かの**累積** (accumulating)
> という2つの側面があって、両方とも捕捉する必要があることを強調したい。

論文の前半は関数型の反復とアプリカティブ・スタイルに関するリビューとなっている。
アプリカティブ・ファンクターに関しては、3種類のアプリカティブがあるらしい:

1. Monadic applicative functors
2. Naperian applicative functors
3. Monoidal applicative functors

全てのモナドがアプリカティブであることは何回か話した。
Naperian applicative functor は固定された形のデータ構造を zip するものだ。

アプリカティブファンクターは McBride さんと Paterson さんによって
*idiom* と名付けられたため、本人たちがアプリカティブ・ファンクターに改名したにもかかわらず
Gibbons さんは論文中で *idiomatic* と *applicative* の両方の用語を同じ意味で使っている。

#### Const データ型を用いた　monoidal applicative functors

> 非モナドな、2つ目の種類のアプリカティブ・ファンクターとして
> モノイダルな対象を持つ定数ファンクターが挙げられる。

全ての `Monoid` からアプリカティブ・ファンクターを導出することができる。
`pure` には `empty` を使って、`ap` には `|+|` を使う。
`Const` データ型は Cats でも `Const` と呼ばれている:

```scala
/**
 * [[Const]] is a phantom type, it does not contain a value of its second type parameter `B`
 * [[Const]] can be seen as a type level version of `Function.const[A, B]: A => B => A`
 */
final case class Const[A, B](getConst: A) {
  /**
   * changes the type of the second type parameter
   */
  def retag[C]: Const[A, C] =
    this.asInstanceOf[Const[A, C]]

  ....
}
```

上のコードの型パラメータ `A` は値を表すが、
`B` は `Functor` の型合わせのみに使われる phantom 型だ。

```console:new
scala> import cats._, cats.instances.all._, cats.data.Const
scala> import cats.syntax.functor._
scala> Const(1) map { (_: String) + "!" }
```

`A` が　`Semigroup` を形成するとき、`Apply` を導き出すことができ、
`A` が `Monoid` を形成するとき、`Applicative` を導き出すことができる。

> このアプリカティブ・ファンクター間での計算は何らかの結果を累積する。
> 整数と加算のモノイドの場合は、カウントや和となる...

```console
scala> import cats.syntax.apply._
scala> Const(2).retag[String => String] ap Const(1).retag[String]
```
