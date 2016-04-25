---
out: combining-applicative.html
---

  [388]: https://github.com/typelevel/cats/pull/388


### アプリカティブ・ファンクターの組み合わせ

EIP:

> モナド同様に、アプリカティブ・ファンクターは積 (product) に関して閉じているため、
> 2つの独立したアプリカティブな効果を積という 1つのものに融合することができ。

Cats はファンクターの積を全く持っていないみたいだ。

#### ファンクターの積

<s>実装してみよう。</s> (ここで書いた実装は [#388][388] で Cats に取り込まれた。)

```scala
/**
 * [[Prod]] is a product to two independent functor values.
 *
 * See: [[https://www.cs.ox.ac.uk/jeremy.gibbons/publications/iterator.pdf The Essence of the Iterator Pattern]]
 */
sealed trait Prod[F[_], G[_], A] {
  def first: F[A]
  def second: G[A]
}
object Prod extends ProdInstances {
  def apply[F[_], G[_], A](first0: => F[A], second0: => G[A]): Prod[F, G, A] = new Prod[F, G, A] {
    val firstThunk: Eval[F[A]] = Later(first0)
    val secondThunk: Eval[G[A]] = Later(second0)
    def first: F[A] = firstThunk.value
    def second: G[A] = secondThunk.value
  }
  def unapply[F[_], G[_], A](x: Prod[F, G, A]): Option[(F[A], G[A])] =
    Some((x.first, x.second))
}
```

まずは `Functor` の積から始める:

```scala
private[data] sealed abstract class ProdInstances4 {
  implicit def prodFunctor[F[_], G[_]](implicit FF: Functor[F], GG: Functor[G]): Functor[Lambda[X => Prod[F, G, X]]] = new ProdFunctor[F, G] {
    def F: Functor[F] = FF
    def G: Functor[G] = GG
  }
}

sealed trait ProdFunctor[F[_], G[_]] extends Functor[Lambda[X => Prod[F, G, X]]] {
  def F: Functor[F]
  def G: Functor[G]
  def map[A, B](fa: Prod[F, G, A])(f: A => B): Prod[F, G, B] = Prod(F.map(fa.first)(f), G.map(fa.second)(f))
}
```

使ってみる:

```console:new
scala> import cats._, cats.std.all._
scala> import cats.data.Prod
scala> val x = Prod(List(1), (Some(1): Option[Int]))
scala> Functor[Lambda[X => Prod[List, Option, X]]].map(x) { _ + 1 }
```

まず、ペアのようなデータ型 `Prod` を定義して、型クラスインスタンスの積を表す。
両方に関数 `f` を渡すことで、簡単に `Prod[F, G]` に関する `Functor` を形成することができる
(ただし `F`、`G` ともに `Functor`)。

動作を確かめるために `x` を写像して、`1` を加算してみる。
使用する側のコードをもっときれいにすることができると思うけど、
今の所はこれで良しとする。

#### Apply ファンクターの積

次は `Apply`:

```scala
private[data] sealed abstract class ProdInstances3 extends ProdInstances4 {
  implicit def prodApply[F[_], G[_]](implicit FF: Apply[F], GG: Apply[G]): Apply[Lambda[X => Prod[F, G, X]]] = new ProdApply[F, G] {
    def F: Apply[F] = FF
    def G: Apply[G] = GG
  }
}

sealed trait ProdApply[F[_], G[_]] extends Apply[Lambda[X => Prod[F, G, X]]] with ProdFunctor[F, G] {
  def F: Apply[F]
  def G: Apply[G]
  def ap[A, B](fa: Prod[F, G, A])(f: Prod[F, G, A => B]): Prod[F, G, B] =
    Prod(F.ap(fa.first)(f.first), G.ap(fa.second)(f.second))
  def product[A, B](fa: Prod[F, G, A], fb: Prod[F, G, B]): Prod[F, G, (A, B)] =
    Prod(F.product(fa.first, fb.first), G.product(fa.second, fb.second))
}
```

これが用例:

```console
scala> val x = Prod(List(1), (Some(1): Option[Int]))
scala> val f = Prod(List((_: Int) + 1), (Some((_: Int) * 3): Option[Int => Int]))
scala> Apply[Lambda[X => Prod[List, Option, X]]].ap(x)(f)
```

`Apply` の積は左右で別の関数を渡している。

#### アプリカティブ・ファンクターの積

最後に、`Applicative` の積が実装できるようになった:

```scala
private[data] sealed abstract class ProdInstances2 extends ProdInstances3 {
  implicit def prodApplicative[F[_], G[_]](implicit FF: Applicative[F], GG: Applicative[G]): Applicative[Lambda[X => Prod[F, G, X]]] = new ProdApplicative[F, G] {
    def F: Applicative[F] = FF
    def G: Applicative[G] = GG
  }
}

sealed trait ProdApplicative[F[_], G[_]] extends Applicative[Lambda[X => Prod[F, G, X]]] with ProdApply[F, G] {
  def F: Applicative[F]
  def G: Applicative[G]
  def pure[A](a: A): Prod[F, G, A] = Prod(F.pure(a), G.pure(a))
}
```

簡単な用例:

```console
scala> Applicative[Lambda[X => Prod[List, Option, X]]].pure(1)
```

`pure(1)` を呼び出すことで `Prod(List(1), Some(1))` を生成することができた。

#### Applicative の合成

> モナド一般では成り立たないが、アプリカティブ・ファンクターは合成に関しても閉じている。
> そのため、逐次的に依存したアプリカティブな効果は、合成として融合することができる。

幸いなことに Cats は `Applicative` の合成は元から入っている。
型クラスインスタンスに `compose` メソッドが入っている:

```scala
@typeclass trait Applicative[F[_]] extends Apply[F] { self =>
  /**
   * `pure` lifts any value into the Applicative Functor
   *
   * Applicative[Option].pure(10) = Some(10)
   */
  def pure[A](x: A): F[A]

  /**
   * Two sequentially dependent Applicatives can be composed.
   *
   * The composition of Applicatives `F` and `G`, `F[G[x]]`, is also an Applicative
   *
   * Applicative[Option].compose[List].pure(10) = Some(List(10))
   */
  def compose[G[_]](implicit GG : Applicative[G]): Applicative[λ[α => F[G[α]]]] =
    new CompositeApplicative[F,G] {
      implicit def F: Applicative[F] = self
      implicit def G: Applicative[G] = GG
    }

  ....
}
```

使ってみよう。

```console
scala> Applicative[List].compose[Option].pure(1)
```

断然使い勝手が良い。

#### アプリカティブ関数の積

Gibbons さんは、ここでアプリカティブ関数を合成する演算子も紹介しているのだけど、
何故かその点は忘れられることが多い気がする。
アプリカティブ関数とは、`A => F[B]` の形を取る関数で `F` が `Applicative` を形成するものを言う。
`Kleisli` 合成に似ているが、**より良い**ものだ。

その理由を説明する。
`Kliesli` 合成は `andThen` を使って `A => F[B]` と `B => F[C]` を合成することができるが、
`F` は一定であることに注目してほしい。
一方、`AppFunc` は `A => F[B]` と `B => G[C]` を合成することができる。

```scala
/**
 * [[Func]] is a function `A => F[B]`.
 *
 * See: [[https://www.cs.ox.ac.uk/jeremy.gibbons/publications/iterator.pdf The Essence of the Iterator Pattern]]
 */
sealed abstract class Func[F[_], A, B] { self =>
  def run: A => F[B]
  def map[C](f: B => C)(implicit FF: Functor[F]): Func[F, A, C] =
    Func.func(a => FF.map(self.run(a))(f))
}

object Func extends FuncInstances {
  /** function `A => F[B]. */
  def func[F[_], A, B](run0: A => F[B]): Func[F, A, B] =
    new Func[F, A, B] {
      def run: A => F[B] = run0
    }

  /** applicative function. */
  def appFunc[F[_], A, B](run0: A => F[B])(implicit FF: Applicative[F]): AppFunc[F, A, B] =
    new AppFunc[F, A, B] {
      def F: Applicative[F] = FF
      def run: A => F[B] = run0
    }

  /** applicative function using [[Unapply]]. */
  def appFuncU[A, R](f: A => R)(implicit RR: Unapply[Applicative, R]): AppFunc[RR.M, A, RR.A] =
    appFunc({ a: A => RR.subst(f(a)) })(RR.TC)
}

....

/**
 * An implementation of [[Func]] that's specialized to [[Applicative]].
 */
sealed abstract class AppFunc[F[_], A, B] extends Func[F, A, B] { self =>
  def F: Applicative[F]

  def product[G[_]](g: AppFunc[G, A, B]): AppFunc[Lambda[X => Prod[F, G, X]], A, B] =
    {
      implicit val FF: Applicative[F] = self.F
      implicit val GG: Applicative[G] = g.F
      Func.appFunc[Lambda[X => Prod[F, G, X]], A, B]{
        a: A => Prod(self.run(a), g.run(a))
      }
    }

  ....
}
```

使ってみる:

```console
scala> import cats.data.Func
scala> val f = Func.appFunc { x: Int => List(x.toString + "!") }
scala> val g = Func.appFunc { x: Int => (Some(x.toString + "?"): Option[String]) }
scala> val h = f product g
scala> h.run(1)
```

2つのアプリカティブ・ファンクターが並んで実行されているのが分かると思う。

#### アプリカティブ関数の合成

これが `andThen` と `compose`:

```scala
  def compose[G[_], C](g: AppFunc[G, C, A]): AppFunc[Lambda[X => G[F[X]]], C, B] =
    {
      implicit val FF: Applicative[F] = self.F
      implicit val GG: Applicative[G] = g.F
      implicit val GGFF: Applicative[Lambda[X => G[F[X]]]] = GG.compose(FF)
      Func.appFunc[Lambda[X => G[F[X]]], C, B]({
        c: C => GG.map(g.run(c))(self.run)
      })
    }

  def andThen[G[_], C](g: AppFunc[G, B, C]): AppFunc[Lambda[X => F[G[X]]], A, C] =
    g.compose(self)
```

```console
scala> val f = Func.appFunc { x: Int => List(x.toString + "!") }
scala> val g = Func.appFunc { x: String => (Some(x + "?"): Option[String]) }
scala> val h = f andThen g
scala> h.run(1)
```

EIP:

> これらの 2つの演算子はアプリカティブ計算を2つの異なる方法で組み合わせる。
> これらをそれぞれ**並行合成**、**逐次合成**と呼ぶ。

新しいデータ型である `Prod` を作る必要があったけども、
アプリカティブ計算の組み合わせは `Applicative` の全てに適用できる抽象的な概念だ。

続きはまた後で。
