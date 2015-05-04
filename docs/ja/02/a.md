---
out: making-our-own-typeclass-with-simulacrum.html
---
  
  [@stewoconnor]: https://twitter.com/stewoconnor
  [@stew]: https://github.com/stew
  [294]: https://github.com/non/cats/pull/294
  [simulacrum]: https://github.com/mpilquist/simulacrum
  [@mpilquist]: https://github.com/mpilquist

### simulacrum を用いた独自型クラスの定義

LYAHFGG:

> JavaScript をはじめ、いくつかの弱く型付けされた言語では、`if` 式の中にほとんど何でも書くことができます。....
> 真理値の意味論が必要なところでは厳密に `Bool` 型を使うのが Haskell の流儀ですが、
> JavaScript 的な振る舞いを実装してみるのも面白そうですよね!

Scala でモジュラーな型クラスを定義するための従来のステップは以下のうようになっていた:

1. 型クラス・コントラクト trait である `Foo` を定義する。
2. 同名のコンパニオン・オブジェクト `Foo` を定義して、`implicitly` のように振る舞う `apply` や、関数から `Foo` のインスタンスを定義するためのヘルパーメソッドを定義する。
3. `FooOps` クラスを定義して、一項演算子や二項演算子を定義する。
4. `Foo` のインスタンスから `FooOps` を implicit に提供する `FooSyntax` trait を定義する。

正直言って、最初のもの以外はほとんどコピーペーストするだけのボイラープレートだ。
ここで登場するのが、Michael Pilquist ([@mpilquist][@mpilquist]) 氏の
[simulacrum][simulacrum] (シミュラクラム) だ。
`@typeclass` アノテーションを書くだけで、simulacrum は魔法のように上記の 2-4 をほぼ生成してくれる。
丁度、Cats を全面的に simulacrum化させた Stew O'Connor ([@stewoconnor][@stewoconnor]/[@stew][@stew]) 氏の [#294][294]
が先日 merge されたばかりだ。

#### Yes と No の型クラス

とりあえず、truthy 値の型クラスを作れるか試してみよう。
`@typeclass` アノテーションに注意:

```console:new
scala> import simulacrum._
scala> :paste
@typeclass trait CanTruthy[A] { self =>
  /** Return true, if `a` is truthy. */
  def truthy(a: A): Boolean
}
object CanTruthy {
  def fromTruthy[A](f: A => Boolean): CanTruthy[A] = new CanTruthy[A] {
    def truthy(a: A): Boolean = f(a)
  }
}
```

[README][simulacrum] によると、マクロによって演算子の enrich 関連コードが色々と生成される:

```scala
// これは、生成されたであろうコードの予想。自分で書く必要は無い!
object CanTruthy {
  def fromTruthy[A](f: A => Boolean): CanTruthy[A] = new CanTruthy[A] {
    def truthy(a: A): Boolean = f(a)
  }

  def apply[A](implicit instance: CanTruthy[A]): CanTruthy[A] = instance

  trait Ops[A] {
    def typeClassInstance: CanTruthy[A]
    def self: A
    def truthy: A = typeClassInstance.truthy(self)
  }

  trait ToCanTruthyOps {
    implicit def toCanTruthyOps[A](target: A)(implicit tc: CanTruthy[A]): Ops[A] = new Ops[A] {
      val self = target
      val typeClassInstance = tc
    }
  }

  trait AllOps[A] extends Ops[A] {
    def typeClassInstance: CanTruthy[A]
  }

  object ops {
    implicit def toAllCanTruthyOps[A](target: A)(implicit tc: CanTruthy[A]): AllOps[A] = new AllOps[A] {
      val self = target
      val typeClassInstance = tc
    }
  }
}
```

ちゃんと動くか確かめるために、`Int` のインスタンスを定義して、使ってみよう。ゴールは `1.truthy` が `true` を返すことだ:

```console
scala> implicit val intCanTruthy: CanTruthy[Int] = CanTruthy.fromTruthy({
         case 0 => false
         case _ => true
       })
scala> import CanTruthy.ops._
scala> 10.truthy
```

動いた。これは、かなり便利だ。
ただ一点警告があって、それはコンパイル時にマクロパラダイス・プラグインが必要なことだ。`CanTruthy`
が一度コンパイルされてしまえば、呼び出す側はマクロパラダイスはいらない。

### シンボルを使った演算子

`CanTruthy` に関しては、注入された演算子は一項演算子で、かつ型クラス・コントラクトの関数と同名のものだった。
simulacrum は `@op` アノテーションを使うことで、シンボルを使った演算子も定義することができる:

```console
scala> @typeclass trait CanAppend[A] {
  @op("|+|") def append(a1: A, a2: A): A
}
scala> implicit val intCanAppend: CanAppend[Int] = new CanAppend[Int] {
  def append(a1: Int, a2: Int): Int = a1 + a2
}
scala> import CanAppend.ops._
scala> 1 |+| 2
```
