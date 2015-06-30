---
out: genericity.html
---

  [Gibbons2006]: http://www.cs.ox.ac.uk/jeremy.gibbons/publications/dgp.pdf
  [Pickling]: https://github.com/scala/pickling
  [shapeless]: https://github.com/milessabin/shapeless
  [scodec]: https://github.com/scodec/scodec

### ジェネリシティ

大局的に見ると、関数型プログラミングは色々なものの抽象化だと考えることができる。
Jeremy Gibbons さんの 2006年の本
[Datatype-Generic Programming][Gibbons2006]
を流し読みしていると、まとめ的なものが見つかった。

以下は拙訳。

> **ジェネリック・プログラミング**とは、安全性を犠牲にせずにプログラミング言語をより柔軟にすることだ。

#### 値によるジェネリシティ

> 全てのプログラマが最初に習うことの一つで、最も重要なテクニックは値をパラメータ化することだ。

```console:new
scala> def triangle4: Unit = {
         println("*")
         println("**")
         println("***")
         println("****")
       }
```

4 を抽象化して、パラメータとして追い出すことができる:

```console
scala> def triangle(side: Int): Unit = {
         (1 to side) foreach { row =>
           (1 to row) foreach { col =>
             println("*")
           }
         }
       }
```

#### 型によるジェネリシティ

`List[A]` は、要素の型という別の型によってパラメータ化されている
**多相的なデータ型** (polymorphic datatype) だ。
これは**パラメトリックな多相性** (parametric polymorphism) を可能とする。

```console
scala> def head[A](xs: List[A]): A = xs(0)
```

上の関数は全てのプロパー型に対して動作する。

#### 関数によるジェネリシティ

> **高階**なプログラムは別のプログラムによりパラメータ化されている。

例えば、`foldLeft` を使って 2つのリストの追加である `append` を書くことができる:

```console
scala> def append[A](list: List[A], ys: List[A]): List[A] =
         list.foldLeft(ys) { (acc, x) => x :: acc }
scala> append(List(1, 2, 3), List(4, 5, 6))
```

数を足し合わせるのにも使うことができる:

```console
scala> def sum(list: List[Int]): Int =
        list.foldLeft(0) { _ + _ }
```

#### 構造によるジェネリシティ

Scala Collection のようなコレクション・ライブラリによって体現化される「ジェネリック・プログラミング」がこれだ。
C++ の Standard Template Library の場合は、パラメトリックなデータ型は**コンテナ**とよばれ、
input iterator や forward iterator といった**イテレータ**によって様々な抽象化が提供される。

型クラスという概念もここに当てはまる。

```console
scala> :paste
trait Read[A] {
  def reads(s: String): Option[A]
}
object Read extends ReadInstances {
  def read[A](f: String => Option[A]): Read[A] = new Read[A] {
    def reads(s: String): Option[A] = f(s)
  }
  def apply[A: Read]: Read[A] = implicitly[Read[A]]
}
trait ReadInstances {
  implicit val stringRead: Read[String] =
    Read.read[String] { Some(_) }
  implicit val intRead: Read[Int] =
    Read.read[Int] { s =>
      try {
        Some(s.toInt)
      } catch {
        case e: NumberFormatException => None
      }
    }
}
scala> Read[Int].reads("1")
```

型クラスは、型クラス・コントラクトとよばれる型が満たさなければいけない要請を表す。
また、型クラスのインスタンスを定義することで、それらの要請を提供する型を列挙することができる。
`Read[A]` における `A` は全称的 (universal) ではないため、
これは**アドホック多相性**を可能とする。

#### 性質によるジェネリシティ

Scala Collection ライブラリの中では、型が列挙する演算よりも込み入った概念が約束されていることがある。

> 演算のシグネチャの他にも、
> これらの演算が満たす法則や、演算の計算量や空間量に対する漸近的複雑度など、機能以外の特性などがある。

法則を持つ型クラスもここに当てはまる。例えば、`Monoid[A]` にはモノイド則がある。
それぞれのインスタンスに対して、これらの法則を満たしているかプロパティ・ベース・テストのツールなどを使って検証する必要がある。

#### ステージによるジェネリシティ

様々な種類の**メタプログラミング**は、別のプログラムを構築したり操作するプログラムの開発だと考えることができる。
これにはコード生成やマクロも含む。

#### 形によるジェネリシティ

ここに多相的なデータ型である二分木があるとする:

```console
scala> :paste
sealed trait Btree[A]
object Btree {
  case class Tip[A](a: A) extends Btree[A]
  case class Bin[A](left: Btree[A], right: Btree[A]) extends Btree[A]
}
```

次に、似たようなプログラムを抽象化するために `foldB` を書いてみる:

```console
scala> def foldB[A, B](tree: Btree[A], b: (B, B) => B)(t: A => B): B =
         tree match {
           case Btree.Tip(a)      => t(a)
           case Btree.Bin(xs, ys) => b(foldB(xs, b)(t), foldB(ys, b)(t))
         }
```

次の目標は `foldB` と `foldLeft` を抽象化することだ。

> これらの 2つの畳み込み演算で異なるのは、それらが作用するデータ型の**形** (shape) であって、
> それがプログラムそのものの形を変えている。
> ここで求めれるパラメータ化の対象はこの形、つまりデータ型やそれらの (`List` や `Tree`)
> といったコンストラクタをパラメータ化する必要がある。
> これを**データ型ジェネリシティ** (datatype genericity) とよぶ。

例えば、`fold` は以下のように表現できるらしい。

```console
scala> import cats._, cats.functor._, cats.std.all._
scala> :paste
trait Fix[F[_,_], A]
def cata[S[_,_]: Bifunctor, A, B](t: Fix[S, A])(f: S[A, B] => B): B = ???
```

上の例では、`S` はデータ型の形を表す。
形を抽象化することで、**パラメトリックにデータ型ジェネリック**なプログラムを書くことができる。
これについては後ほど。

> その振る舞いにおいて何らかの方法で形を利用するプログラムは**アドホックなデータ型ジェネリック**とよぶ。
> pretty printer やシリアライザが典型的な例だ。

この例に当てはまりそうなのは [Scala Pickling][Pickling] だ。
Pickling はよくある型には予め pickler を提供して、
マクロを使って異なる形に対して pickler のインスタンスを導出している。

> この方法のデータ型ジェネリシティは *polytypism*、
> **構造的多相性**、*typecase* など様々な名前でよばれ、
> Generic Haskell チームが「ジェネリック・プログラミング」と言うときもこの意味だ。....
>
> 我々は、パラメトリックなデータ型ジェネリシティこそが「最高基準」であると考え、
> 講義ノートでも今後は可能な限りパラメトリックなデータ型ジェネリシティに焦点を置く。

Scala 界隈だと、[shapeless][shapeless] が形の抽象化に焦点を置いているだろう。
