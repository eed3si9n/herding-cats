---
out: FunctionK.html
---

  [Id]: Id.html
  [SemigroupK]: SemigroupK.html
  [MonadCancel]: MonadCancel.html
  [@runarorama]: https://twitter.com/runarorama
  [higher-rank]: https://apocalisp.wordpress.com/2010/07/02/higher-rank-polymorphism-in-scala/
  [LFST]: http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.144.2237&rep=rep1&type=pdf
  [Regions]: http://okmij.org/ftp/Haskell/regions.html
  [higher-rank-ja]: https://gist.github.com/xuwei-k/3300312

### FunctionK

Cats は 2つの型コンストラクタ `F1[_]` と `F2[_]` を型パラメータとして受け取り、全ての `A` において `F1[A]` の全ての値を `F2[A]` に変換することができることを表す `FunctionK` を提供する。

```scala
trait FunctionK[F[_], G[_]] extends Serializable { self =>

  /**
   * Applies this functor transformation from `F` to `G`
   */
  def apply[A](fa: F[A]): G[A]

  def compose[E[_]](f: FunctionK[E, F]): FunctionK[E, G] =
    new FunctionK[E, G] { def apply[A](fa: E[A]): G[A] = self(f(fa)) }

  def andThen[H[_]](f: FunctionK[G, H]): FunctionK[F, H] =
    f.compose(self)

  def or[H[_]](h: FunctionK[H, G]): FunctionK[EitherK[F, H, *], G] =
    new FunctionK[EitherK[F, H, *], G] { def apply[A](fa: EitherK[F, H, A]): G[A] = fa.fold(self, h) }

  ....
}
```

シンボルを使って `FunctionK[F1, F2]` は `F1 ~> F2` と表記される:

```scala mdoc
import cats._, cats.syntax.all._

lazy val first: List ~> Option = ???
```

`F[_]` のことをファンクター (函手) と呼ぶことが多いので、`FunctionK` も中二病的に「自然変換」と呼ばれることがあるが、`FunctionK` ぐらいの名前のほうが実態に即していると思う。

最初の要素を返す `List ~> Option` を実装してみよう。

```scala mdoc:reset:invisible
import cats._, cats.syntax.all._
```

```scala mdoc
val first: List ~> Option = new (List ~> Option) {
  def apply[A](fa: List[A]): Option[A] = fa.headOption
}

first(List("a", "b", "c"))
```

少し冗長に見える。このようなコードをどれだけ頻繁に書くかにもよるが、普通の関数が以下のように短く書けるように簡易記法があると嬉しい:

```scala mdoc
import scala.util.chaining._

List("a", "b", "c").pipe(_.headOption)
```

kind projector が提供する「多相ラムダ書き換え」(polymorphic lambda rewrite) `λ` を使うとこう書ける:

```scala mdoc:reset:invisible
import cats._, cats.syntax.all._
```

```scala mdoc
val first = λ[List ~> Option](_.headOption)

first(List("a", "b", "c"))
```

#### Higher-Rank Polymorphism in Scala

2010年の7月に Rúnar ([@runarorama][@runarorama]) さんが [Higher-Rank Polymorphism in Scala][higher-rank] というブログ記事を書いてランク2多相性を解説した。吉田さんが 2012年に [Scala での高ランクポリモーフィズム][higher-rank-ja]として和訳している。まずは、通常の (ランク1) 多相関数をみてみる:

```scala mdoc
def pureList[A](a: A): List[A] = List(a)
```

これはどの `A` に対しても動く:

```scala mdoc
pureList(1)

pureList("a")
```

Rúnar さんが 2010年に指摘したのは、Scala にはこれにに対するファーストクラス概念が無いということだ。

> この関数を別の関数の引数にしたいとします。ランク1多相では、これは不可能です

```scala mdoc:fail
def usePolyFunc[A, B](f: A => List[A], b: B, s: String): (List[B], List[String]) =
  (f(b), f(s))
```

これは Launchbury さんと SPJ が 1994年に [State Threads][LFST] で Haskell ができないと指摘したのと同じことだ:

```haskell
runST :: ∀a. (∀s. ST s a) -> a
```

> This is not a Hindley-Milner type, because the quantifiers are not all at the top level; it is an example of rank-2 polymorphism.

Rúnar さんに戻ると:

> `B` と `String` は `A` ではないので、これは型エラーになります。つまり、型`A`は `[A, B]`の `B` に固定されてしまいます。 私達が本当に欲しいのは、引数に対して多相的な関数です。もし仮に Scala にランクN型があるとすれば以下のようになるでしょう

```scala
def usePolyFunc[B](f: (A => List[A]) forAll { A }, b: B, s: String): (List[B], List[String]) =
  (f(b), f(s))
```

> ランク2多相な関数をあらわすために、`apply` メソッドに型引数をとる新しい trait をつくります。

```scala
trait ~>[F[_], G[_]] {
  def apply[A](a: F[A]): G[A]
}
```

これは `FunctionK` と同じ、正確には `FunctionK` は `~>` だと言うべきだろうか。次に巧みな技で Rúnar さんは [Id データ型][Id]を使って `A` を `F[_]` へと持ち上げている:

> identity functor から List functor の自然変換 (natural transformation) によって、(最初に例に出した)リストにある要素を加える関数をあらわすことができるようになりました:

```scala mdoc:reset:invisible
import cats._, cats.syntax.all._
```

```scala mdoc
val pureList: Id ~> List = λ[Id ~> List](List(_))

def usePolyFunc[B](f: Id ~> List, b: B, s: String): (List[B], List[String]) =
  (f(b), f(s))

usePolyFunc(pureList, 1, "x")
```

できた。これで頑張って多相関数を別の関数に渡せるようになった。一時期ランク2型多相が一部で大人気だった気がするが、これは [State Threads][LFST] やその他の後続の論文にてリソースに対する型安全なアクセスを保証する基礎だと喧伝されていたからじゃないだろうか。

#### MonadCancel での FunctionK

[MonadCancel][MonadCancel] をもう一度見てみると、`FunctionK` が隠れている:

```scala
trait MonadCancel[F[_], E] extends MonadError[F, E] {
  def rootCancelScope: CancelScope

  def forceR[A, B](fa: F[A])(fb: F[B]): F[B]

  def uncancelable[B](body: Poll[F] => F[B]): F[B]

  ....
}
```

上の `Poll[F]` というのは実は、`F ~> F` の型エイリアスだからだ:

```scala
trait Poll[F[_]] extends (F ~> F)
```

つまり、全ての `A` に対して、`F[A]` は `F[A]` を返す。

```scala mdoc
import cats.effect.IO

lazy val program = IO.uncancelable { poll =>
  poll(IO.canceled) >> IO.println("nope again")
}
```

上のような状況で `IO` は全ての `A` において動く関数を僕たちに渡す必要があるが、Rúnar さんの解説によってランク1多相だとそれが不可能なことが分かったはずだ。例えば仮に以下のような定義だとする:

```scala
def uncancelable[A, B](body: F[A] => F[A] => F[B]): F[B]
```

これは `poll(...)` が 1回呼び出される場合なら何とかなるかもしれないが、`IO.uncancelable { ... }` 内からは `poll(...)` は複数回呼んでもいいはずだ:

```scala mdoc
lazy val program2: IO[Int] = IO.uncancelable { poll =>
  poll(IO.println("a")) >> poll(IO.pure("b")) >> poll(IO.pure(1))
}
```

なので、`poll(...)` は実際には `∀A. IO[A] => IO[A]`、つまり `IO ~> IO` だ。
