---
out: datatype-generic-programming.html
---

  [Gibbons2006]: http://www.cs.ox.ac.uk/jeremy.gibbons/publications/dgp.pdf
  [wfmm]: http://www.haskellforall.com/2012/06/you-could-have-invented-free-monads.html
  [Free-monads]: Free-monads.html
  [Oliveira2008]: http://www.cs.ox.ac.uk/jeremy.gibbons/publications/scalagp.pdf
  [Oliveira2010]: http://www.cs.ox.ac.uk/jeremy.gibbons/publications/scalagp-jfp.pdf

### Bifunctor を用いたデータ型ジェネリック・プログラミング

[Datatype-Generic Programming][Gibbons2006] の 3.6節「データ型ジェネリシティ」をみてみよう。
Gibbons さんはこれをオリガミ・プログラミングと命名しようとしたみたいだけど、
名前として流行っている気配が無いのでここではデータ型ジェネリック・プログラミングと呼ぶことにする。

> 既に述べたように、データ構造はプログラム構造を規定する。
> そのため、決め手となる形を抽象化して、異なる形のプログラムの共通部分だけのこすというのは理にかなっている。
> `List` や `Tree` といったデータ型に共通しているのはそれらが再帰的、つまり `Fix` であることだ。

```haskell
data Fix s a = In {out :: s a (Fix s a)}
```

> 以下は `Fix` を異なる形に用いた例だ: リスト、既に見たラベルを内部に持つ二分木、そしてラベルを外部に持つ二分木だ。

```haskell
data ListF a b = NilF | ConsF a b
type List a = Fix ListF a
data TreeF a b = EmptyF | NodeF a b b
type Tree a = Fix TreeF a
data BtreeF a b = TipF a | BinF b b
type Btree a = Fix BtreeF a
```

[8日目][Free-monads]の [Why free monads matter][wfmm]
からこれは実は `Free` データ型であることが分かっているけども、
`Functor` などに関する意味が異なるので、一から実装してみる:

```console:new
scala> :paste
sealed abstract class Fix[S[_], A] extends Serializable {
  def out: S[Fix[S, A]]
}
object Fix {
  case class In[S[_], A](out: S[Fix[S, A]]) extends Fix[S, A]
}
```

`Free` に倣って、`S[_]` を左側に、`A` を右側に置く。

`List` をまず実装してみる。

```console
scala> :paste
sealed trait ListF[+Next, +A]
object ListF {
  case class NilF() extends ListF[Nothing, Nothing]
  case class ConsF[A, Next](a: A, n: Next) extends ListF[Next, A]
}
type GenericList[A] = Fix[ListF[+?, A], A]
object GenericList {
  def nil[A]: GenericList[A] = Fix.In[ListF[+?, A], A](ListF.NilF())
  def cons[A](a: A, xs: GenericList[A]): GenericList[A] =
    Fix.In[ListF[+?, A], A](ListF.ConsF(a, xs))
}
scala> import GenericList.{ cons, nil }
```

このように使うことができる:

```console
scala> cons(1, nil)
```

ここまでは自由モナドで見たのと似ている。

#### Bifunctor

> 全ての二項型コンストラクタが不動点化できるとは限らず、
> パラメータが**反変** (contravariant) な位置 (ソース側) だと問題となる。
> 全ての要素を「探しだす」ことができる *bimap* 演算をサポートする
> (共変な) 双函手 (bifunctor) だとうまくいくことが分かっている。

Cats はこれを `Bifunctor` とよんでいる:

```scala
/**
 * A typeclass of types which give rise to two independent, covariant
 * functors.
 */
trait Bifunctor[F[_, _]] extends Serializable { self =>

  /**
   * The quintessential method of the Bifunctor trait, it applies a
   * function to each "side" of the bifunctor.
   */
  def bimap[A, B, C, D](fab: F[A, B])(f: A => C, g: B => D): F[C, D]

  ....
}
```

これが、`GenericList` の `Bifunctor` インスタンスだ。

```console
scala> import cats._, cats.std.all._
scala> import cats.functor.Bifunctor
scala> :paste
implicit val listFBifunctor: Bifunctor[ListF] = new Bifunctor[ListF] {
  def bimap[S1, A1, S2, A2](fab: ListF[S1, A1])(f: S1 => S2, g: A1 => A2): ListF[S2, A2] =
    fab match {
      case ListF.NilF()         => ListF.NilF()
      case ListF.ConsF(a, next) => ListF.ConsF(g(a), f(next))
    }
}
```

#### Bifunctor からの map の導出

> `Bifunctor` クラスは、様々な再帰パターンをデータ型ジェネリックなプログラムとして表すのに十分な柔軟性を持っていることがわかった。

まず、`bimap` を使って `map` を実装する。

```console
scala> :paste
object DGP {
  def map[F[_, _]: Bifunctor, A1, A2](fa: Fix[F[?, A1], A1])(f: A1 => A2): Fix[F[?, A2], A2] =
    Fix.In[F[?, A2], A2](Bifunctor[F].bimap(fa.out)(map(_)(f), f))
}
scala> DGP.map(cons(1, nil)) { _ + 1 }
```

上の `map` の定義は `GenericList` から独立しているもので、
`Bifunctor` と `Fix` によって抽象化されている。
別の見方をすると、`Bifunctor` と `Fix` から `Functor` をただでもらえると言える。

```console
scala> :paste
trait FixInstances {
  implicit def fixFunctor[F[_, _]: Bifunctor]: Functor[Lambda[L => Fix[F[?, L], L]]] =
    new Functor[Lambda[L => Fix[F[?, L], L]]] {
      def map[A1, A2](fa: Fix[F[?, A1], A1])(f: A1 => A2): Fix[F[?, A2], A2] =
        Fix.In[F[?, A2], A2](Bifunctor[F].bimap(fa.out)(map(_)(f), f))
    }
}
scala> {
  val instances = new FixInstances {}
  import instances._
  import cats.syntax.functor._
  cons(1, nil) map { _ + 1 }
}
```

激しい量の型ラムダだけども、`DB.map` から `Functor` インスタンスへと翻訳しただけだというのは明らかだと思う。

#### Bifunctor からの fold の導出

`fold` も実装できる。これは、catamorphism から `cata` とも呼ばれる。

```console
scala> :paste
object DGP {
  // catamorphism
  def fold[F[_, _]: Bifunctor, A1, A2](fa: Fix[F[?, A1], A1])(f: F[A2, A1] => A2): A2 =
    {
      val g = (fa1: F[Fix[F[?, A1], A1], A1]) =>
        Bifunctor[F].leftMap(fa1) { (fold(_)(f)) }
      f(g(fa.out))
    }
}
scala> DGP.fold[ListF, Int, Int](cons(2, cons(1, nil))) {
         case ListF.NilF()      => 0
         case ListF.ConsF(x, n) => x + n
       }
```

#### Bifunctor からの unfold の導出

> `unfold` 演算は、ある値からデータ構造を育てるのに使う。
> 正確には、これは `fold` 演算の双対だ。

`unfold` は anamorphism から `ana` とも呼ばれる。

```console
scala> :paste
object DGP {
  // catamorphism
  def fold[F[_, _]: Bifunctor, A1, A2](fa: Fix[F[?, A1], A1])(f: F[A2, A1] => A2): A2 =
    {
      val g = (fa1: F[Fix[F[?, A1], A1], A1]) =>
        Bifunctor[F].leftMap(fa1) { (fold(_)(f)) }
      f(g(fa.out))
    }
  // anamorphism
  def unfold[F[_, _]: Bifunctor, A1, A2](x: A2)(f: A2 => F[A2, A1]): Fix[F[?, A1], A1] =
    Fix.In[F[?, A1], A1](Bifunctor[F].leftMap(f(x))(unfold[F, A1, A2](_)(f)))
}
```

数をカウントダウンしてリストを構築してみる:

```console
scala> def pred(n: Int): GenericList[Int] =
         DGP.unfold[ListF, Int, Int](n) {
           case 0 => ListF.NilF()
           case n => ListF.ConsF(n, n - 1)
         }
scala> pred(4)
```

他にもいくつか導出できるみたいだ。

#### Tree

データ型ジェネリック・プログラミングの肝は形の抽象化だ。
他のデータ型も定義してみよう。例えば、これは二分木の `Tree` だ:

```console
scala> :paste
sealed trait TreeF[+Next, +A]
object TreeF {
  case class EmptyF() extends TreeF[Nothing, Nothing]
  case class NodeF[Next, A](a: A, left: Next, right: Next) extends TreeF[Next, A]
}
type Tree[A] = Fix[TreeF[?, A], A]
object Tree {
  def empty[A]: Tree[A] =
    Fix.In[TreeF[+?, A], A](TreeF.EmptyF())
  def node[A, Next](a: A, left: Tree[A], right: Tree[A]): Tree[A] =
    Fix.In[TreeF[+?, A], A](TreeF.NodeF(a, left, right))
}
```

木はこのように作る:

```console
scala> import Tree.{empty,node}
scala> node(2, node(1, empty, empty), empty)
```

あとは `Bifunctor` のインスタンスだけを定義すればいいはずだ:

```console
scala> :paste
implicit val treeFBifunctor: Bifunctor[TreeF] = new Bifunctor[TreeF] {
  def bimap[A, B, C, D](fab: TreeF[A, B])(f: A => C, g: B => D): TreeF[C, D] =
    fab match {
      case TreeF.EmptyF() => TreeF.EmptyF()
      case TreeF.NodeF(a, left, right) =>
        TreeF.NodeF(g(a), f(left), f(right))
    }
}
```

まず、`Functor` を試してみる:

```console
scala> {
  val instances = new FixInstances {}
  import instances._
  import cats.syntax.functor._
  node(2, node(1, empty, empty), empty) map { _ + 1 }
}
```

うまくいった。次に、畳込み。

```console
scala> def sum(tree: Tree[Int]): Int =
         DGP.fold[TreeF, Int, Int](tree) {
           case TreeF.EmptyF()       => 0
           case TreeF.NodeF(a, l, r) => a + l + r
         }
scala> sum(node(2, node(1, empty, empty), empty))
```

`fold` もできた。

以下は `grow` という関数で、これはリストから二分探索木を生成する。

```console
scala> def grow[A: PartialOrder](xs: List[A]): Tree[A] =
          DGP.unfold[TreeF, A, List[A]](xs) {
            case Nil => TreeF.EmptyF()
            case x :: xs =>
              import cats.syntax.partialOrder._
              TreeF.NodeF(x, xs filter {_ <= x}, xs filter {_ > x})
          }
scala> grow(List(3, 1, 4, 2))
```

`unfold` もうまくいったみたいだ。

Scala での DGP に関する詳細は、Oliveira さんと Gibbons さん自身が
ここでみた考えや他の概念を Scala に翻訳した
[Scala for Generic Programmers (2008)][Oliveira2008] とその改定版である
[Scala for Generic Programmers (2010)][Oliveira2010] を出している。

#### オリガミ・パターン

次に、Gibbons さんはデザイン・パターンは
「それらの主流なプログラミング言語が表現性の欠けている証拠」だと主張している。
そして、それらのパターンを高階データ型ジェネリックなプログラミングで置き換えることに船舵を切っている。
