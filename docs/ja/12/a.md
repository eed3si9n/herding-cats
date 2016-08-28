---
out: Traverse.html
---

  [iterator2009]: http://www.comlab.ox.ac.uk/jeremy.gibbons/publications/iterator.pdf
  [TraverseSource]: $catsBaseUrl$/core/src/main/scala/cats/Traverse.scala
  [ControlMonadSequence]: https://downloads.haskell.org/~ghc/7.8.4/docs/html/libraries/base-4.7.0.2/Control-Monad.html#v:sequence
  [McBride2008]: http://strictlypositive.org/IdiomLite.pdf
  [FutureSequence]: http://www.scala-lang.org/api/2.11.6/index.html#scala.concurrent.Future\$

### Traverse

[The Essence of the Iterator Pattern][iterator2009]:

> McBride と Paterson がアプリカティブ計算の動機として例に挙げた 3つの例のうち、
> 2つ (モナディックな作用のリストの sequence と、行列の転置)
> は *traversal* と呼ばれる一般スキームの例だ。
> これは、`map` のようにデータ構造内の要素の反復することを伴うが、
> ただし、ある特定の関数適用をアプリカティブに解釈する。
>
> これは *Traversable* なデータ構造という型クラスとして表現される。

Cats では、この型クラスは [Traverse][TraverseSource] と呼ばれる:

```scala
@typeclass trait Traverse[F[_]] extends Functor[F] with Foldable[F] { self =>

  /**
   * given a function which returns a G effect, thread this effect
   * through the running of this function on all the values in F,
   * returning an F[A] in a G context
   */
  def traverse[G[_]: Applicative, A, B](fa: F[A])(f: A => G[B]): G[F[B]]

  /**
   * thread all the G effects through the F structure to invert the
   * structure from F[G[_]] to G[F[_]]
   */
  def sequence[G[_]: Applicative, A](fga: F[G[A]]): G[F[A]] =
    traverse(fga)(ga => ga)
  ....
}
```

`f` が　`A => G[B]` という形を取ることに注目してほしい。

> *m* が恒等アプリカティブ・ファンクターであるとき、
> traversal はリストのファンクター的な `map` と一致する (ラッパーを無視すると)。

Cats に恒等アプリカティブ・ファンクターは以下のように定義されている:

```scala
  type Id[A] = A
  implicit val Id: Bimonad[Id] =
    new Bimonad[Id] {
      def pure[A](a: A): A = a
      def extract[A](a: A): A = a
      def flatMap[A, B](a: A)(f: A => B): B = f(a)
      def coflatMap[A, B](a: A)(f: A => B): B = f(a)
      override def map[A, B](fa: A)(f: A => B): B = f(fa)
      override def ap[A, B](fa: A)(ff: A => B): B = ff(fa)
      override def flatten[A](ffa: A): A = ffa
      override def map2[A, B, Z](fa: A, fb: B)(f: (A, B) => Z): Z = f(fa, fb)
      override def lift[A, B](f: A => B): A => B = f
      override def imap[A, B](fa: A)(f: A => B)(fi: B => A): B = f(fa)
  }
```

`Id` を使って、`List(1, 2, 3)` を走査 (traverse) してみる。

```console:new
scala> import cats._, cats.instances.all._
scala> import cats.syntax.traverse._
scala> List(1, 2, 3) traverse[Id, Int] { (x: Int) => x + 1 }
```

> モナディックなアプリカティブ・ファンクターの場合、traversal はモナディックな map に特化し、同じ用例となる。
> traversal はモナディックな map を少し一般化したものだと考えることができる。

`List` を使って試してみる:

```console
scala> List(1, 2, 3) traverse { (x: Int) => (Some(x + 1): Option[Int]) }
scala> List(1, 2, 3) traverse { (x: Int) => None }
```

> Naperian なアプリカティブ・ファンクターの場合は、traversal は結果を転置する。

これはパス。

> モノイダルなアプリカティブ・ファンクターの場合は、traversal は値を累積する。
> `reduce` 関数は各要素に値を割り当てる関数を受け取って、累積する。

```console
scala> import cats.data.Const
scala> def reduce[A, B, F[_]](fa: F[A])(f: A => B)
         (implicit FF: Traverse[F], BB: Monoid[B]): B =
         {
           val g: A => Const[B, Unit] = { (a: A) => Const((f(a))) }
           val x = FF.traverse[Const[B, ?], A, Unit](fa)(g)
           x.getConst
         }
```

これはこのように使う:

```console
scala> reduce(List('a', 'b', 'c')) { c: Char => c.toInt }
```

一応できたけど、型注釈を少し減らせるともっと良い感じがする。
問題は、現状の Scala 2.11 コンパイラは `Const[B, ?]` を推論できないことにある。
コンパイラを説得していくつかの形を推論させるテクニックがあって、これは `Unapply` と呼ばれている。
`traverse` の代わりに `traverseU` を使うとこれを利用できる:

```console
scala> def reduce[A, B, F[_]](fa: F[A])(f: A => B)
         (implicit FF: Traverse[F], BB: Monoid[B]): B =
         {
           val x = fa traverseU { (a: A) => Const((f(a))) }
           x.getConst
         }
```

これに関してはまた後で。

### sequence 関数

`Applicative` と `Traverse` は McBride さんと Paterson さんによって
[Applicative programming with effects][McBride2008] の中でセットで言及されている。

この背景として、数ヶ月前 (2015年3月) まで、`Control.Monad` パッケージの
[sequence][ControlMonadSequence] 関数は以下のように定義されていた:

```haskell
-- | Evaluate each action in the sequence from left to right,
-- and collect the results.
sequence :: Monad m => [m a] -> m [a]
```

これを Scala に翻訳すると、このようになる:

```scala
def sequence[G[_]: Monad, A](gas: List[G[A]]): G[List[A]]
```

これはモナディック値のリストを受け取って、リストのモナディック値を返す。
これだけでも十分便利そうだが、このように `List` 決め打ちの関数が出てきたら、
何か良い型クラスで置換できないかを疑ってみるべきだ。

McBride さんと Paterson さんは、まず `sequence` 関数の
`Monad` を `Applicative` に置換して、`dist` として一般化した:

```scala
def dist[G[_]: Applicative, A](gas: List[G[A]]): G[List[A]]
```

次に、`dist` が `map` と一緒に呼ばれることが多いことに気付いたので、
アプリカティブ関数をパラメータとして追加して、これを `traverse` と呼んだ:

```scala
def traverse[G[_]: Applicative, A, B](as: List[A])(f: A => G[B]): G[List[B]]
```

最後に、このシグネチャを型クラスとして一般化したものが `Traversible` 型クラスと呼ばれるものとなった:

```scala
@typeclass trait Traverse[F[_]] extends Functor[F] with Foldable[F] { self =>

  /**
   * given a function which returns a G effect, thread this effect
   * through the running of this function on all the values in F,
   * returning an F[A] in a G context
   */
  def traverse[G[_]: Applicative, A, B](fa: F[A])(f: A => G[B]): G[F[B]]

  /**
   * thread all the G effects through the F structure to invert the
   * structure from F[G[_]] to G[F[_]]
   */
  def sequence[G[_]: Applicative, A](fga: F[G[A]]): G[F[A]] =
    traverse(fga)(ga => ga)
  ....
}
```

そのため、歴史の必然として `Traverse` はデータ型ジェネリックな `sequence` 関数を実装する。
言ってみれば `traverse` に `identity` を渡しただけなんだけど、
`F[G[A]]` を `G[F[A]]` にひっくり返しただけなので、コンセプトとして覚えやすい。
標準ライブラリの `Future` に入ってる[この関数][FutureSequence]として見たことがあるかもしれない。

```console
scala> import scala.concurrent.{ Future, ExecutionContext, Await }
scala> import scala.concurrent.duration._
scala> val x = {
         implicit val ec = scala.concurrent.ExecutionContext.global
         List(Future { 1 }, Future { 2 }).sequence
       }
scala> Await.result(x, 1 second)
```

`Either` の `List` をまとめて `Either` にするとか便利かもしれない。

```console
scala> List(Right(1): Either[String, Int]).sequenceU
scala> List(Right(1): Either[String, Int], Left("boom"): Either[String, Int]).sequenceU
```

`sequence` の `Unapply` 版である `sequenceU` を使ったことに注意。
続いては、これを見ることにする。
