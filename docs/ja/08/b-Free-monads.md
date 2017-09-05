---
out: Free-monads.html
---

  [Free-monoids]: Free-monoids.html
  [@gabrielg439]: https://twitter.com/gabrielg439
  [wfmm]: http://www.haskellforall.com/2012/06/you-could-have-invented-free-monads.html
  [FreeSource]: $catsBaseUrl$free/src/main/scala/cats/free/Free.scala
  [ControlMonadFreeSource]: http://hackage.haskell.org/package/free-4.2/docs/src/Control-Monad-Free.html
  [WikipediaMonad]: http://en.wikipedia.org/wiki/Monad_(functional_programming)#Free_monads

### 自由モナド (Free)

[自由モノイド][Free-monoids]は自由対象の例だと言った。
同様に、自由モナドも自由対象の例だ。

細かい話は省くが、モナドは自己函手 `F: C => C` の圏におけるモノイドで、
`F × F => F` を2項演算子とする。
`A` から `A*` を導き出したのと同様に、
任意の自己函手 `F` から自由モナド `F*` を導き出すことができる。

Haskell ではこのように行っている:

```haskell
data Free f a = Pure a | Free (f (Free f a))

instance Functor f => Monad (Free f) where
  return = Pure
  Pure a >>= f = f a
  Free m >>= f = Free ((>>= f) <\$> m)
```

[Wikipedia on Monad][WikipediaMonad]:

> 値のリストを保持する `List` と違って、`Free` は函手を初期値にラッピングしたもののリストを保持する。
> そのため、`Free` の `Functor` と `Monad` のインスタンスは、`fmap` を使って与えられた関数を渡して回る以外のことは何もしない。

また、`Free` というのはデータ型だけども、`Functor` ごとに異なる自由モナドが得られることにも注意してほしい。

#### 自由モナドの重要性

実務上では、`Free` を `Functor` から `Monad` を得るための巧妙な手口だと考えることができる。
これは interperter パターンと呼ばれる使い方で特に便利で、
Gabriel Gonzalez ([@gabrielg439][@gabrielg439]) さんの
[Why free monads matter][wfmm] で解説されている。

> 構文木の本質を表す抽象体を考えてみよう。[中略]
>
> 僕らの toy 言語には 3つのコマンドしかない:

```
output b -- prints a "b" to the console
bell     -- rings the computer's bell
done     -- end of execution
```

> 次のコマンドが前のコマンドの子ノードであるような構文木としてあらわしてみる:

```haskell
data Toy b next =
    Output b next
  | Bell next
  | Done
```

とりあえずこれを素直に Scala に翻訳するとこうなる:

```console:new
scala> :paste
sealed trait Toy[+A, +Next]
object Toy {
  case class Output[A, Next](a: A, next: Next) extends Toy[A, Next]
  case class Bell[Next](next: Next) extends Toy[Nothing, Next]
  case class Done() extends Toy[Nothing, Nothing]
}
scala> Toy.Output('A', Toy.Done())
scala> Toy.Bell(Toy.Output('A', Toy.Done()))
```

#### CharToy

WFMM の DSL はアウトプット用のデータ型を型パラメータとして受け取るので、任意のアウトプット型を扱うことができる。
上に `Toy` として示したように Scala も同じことができる。だけども、Scala の部分適用型の処理がヘボいため `Free` の説明としては不必要に複雑となってしまう。そのため、本稿では、以下のようにデータ型を `Char` に決め打ちしたものを使う:

```console
scala> :paste
sealed trait CharToy[+Next]
object CharToy {
  case class CharOutput[Next](a: Char, next: Next) extends CharToy[Next]
  case class CharBell[Next](next: Next) extends CharToy[Next]
  case class CharDone() extends CharToy[Nothing]

  def output[Next](a: Char, next: Next): CharToy[Next] = CharOutput(a, next)
  def bell[Next](next: Next): CharToy[Next] = CharBell(next)
  def done: CharToy[Nothing] = CharDone()
}
scala> { import CharToy._
         output('A', done)
       }
scala> { import CharToy._
         bell(output('A', done))
       }
```

型を `CharToy` に統一するため、小文字の `output`、`bell`、`done` を加えた。

#### Fix

WFMM:

> しかし残念なことに、コマンドを追加するたびに型が変わってしまうのでこれはうまくいかない。

`Fix` を定義しよう:

```console
scala> :paste
case class Fix[F[_]](f: F[Fix[F]])
object Fix {
  def fix(toy: CharToy[Fix[CharToy]]) = Fix[CharToy](toy)
}
scala> { import Fix._, CharToy._
         fix(output('A', fix(done)))
       }
scala> { import Fix._, CharToy._
         fix(bell(fix(output('A', fix(done)))))
       }
```

ここでも `fix` を提供して型推論が動作するようにした。

#### FixE

これに例外処理を加えた `FixE` も実装してみる。`throw` と `catch` は予約語なので、`throwy`、`catchy` という名前に変える:

```console
scala> import cats._, cats.data._, cats.implicits._
scala> :paste
sealed trait FixE[F[_], E]
object FixE {
  case class Fix[F[_], E](f: F[FixE[F, E]]) extends FixE[F, E]
  case class Throwy[F[_], E](e: E) extends FixE[F, E]

  def fix[E](toy: CharToy[FixE[CharToy, E]]): FixE[CharToy, E] =
    Fix[CharToy, E](toy)
  def throwy[F[_], E](e: E): FixE[F, E] = Throwy(e)
  def catchy[F[_]: Functor, E1, E2](ex: => FixE[F, E1])
      (f: E1 => FixE[F, E2]): FixE[F, E2] = ex match {
    case Fix(x)    => Fix[F, E2](Functor[F].map(x) {catchy(_)(f)})
    case Throwy(e) => f(e)
  }
}
```

> これを実際に使うには Toy b が functor である必要があるので、型検査が通るまで色々試してみる (Functor則を満たす必要もある)。

`CharToy` の `Functor` はこんな感じになった:

```console
scala> implicit val charToyFunctor: Functor[CharToy] = new Functor[CharToy] {
         def map[A, B](fa: CharToy[A])(f: A => B): CharToy[B] = fa match {
           case o: CharToy.CharOutput[A] => CharToy.CharOutput(o.a, f(o.next))
           case b: CharToy.CharBell[A]   => CharToy.CharBell(f(b.next))
           case CharToy.CharDone()       => CharToy.CharDone()
         }
       }
```

これがサンプルの使用例だ:

```console
scala> :paste
{
  import FixE._, CharToy._
  case class IncompleteException()
  def subroutine = fix[IncompleteException](
    output('A',
      throwy[CharToy, IncompleteException](IncompleteException())))
  def program = catchy[CharToy, IncompleteException, Nothing](subroutine) { _ =>
    fix[Nothing](bell(fix[Nothing](done)))
  }
}
```

型パラメータでゴテゴテになってるのはちょっと残念な感じだ。

#### Free データ型

WFMM:

> 僕らの `FixE` は既に存在していて、それは Free モナドと呼ばれる:

```haskell
data Free f r = Free (f (Free f r)) | Pure r
```

> 名前の通り、これは自動的にモナドだ (ただし、`f` が Functor の場合)

```haskell
instance (Functor f) => Monad (Free f) where
    return = Pure
    (Free x) >>= f = Free (fmap (>>= f) x)
    (Pure r) >>= f = f r
```

> 僕達の `Throw` は `return` となって、僕達の `catch` は `(>>=)` に対応する。

Cats でのデータ型は [Free][FreeSource] と呼ばれる:

```scala
/**
 * A free operational monad for some functor `S`. Binding is done
 * using the heap instead of the stack, allowing tail-call
 * elimination.
 */
sealed abstract class Free[S[_], A] extends Product with Serializable {

  final def map[B](f: A => B): Free[S, B] =
    flatMap(a => Pure(f(a)))

  /**
   * Bind the given continuation to the result of this computation.
   * All left-associated binds are reassociated to the right.
   */
  final def flatMap[B](f: A => Free[S, B]): Free[S, B] =
    Gosub(this, f)

  ....
}

object Free {
  /**
   * Return from the computation with the given value.
   */
  private final case class Pure[S[_], A](a: A) extends Free[S, A]

  /** Suspend the computation with the given suspension. */
  private final case class Suspend[S[_], A](a: S[A]) extends Free[S, A]

  /** Call a subroutine and continue with the given function. */
  private final case class Gosub[S[_], B, C](c: Free[S, C], f: C => Free[S, B]) extends Free[S, B]

  /**
   * Suspend a value within a functor lifting it to a Free.
   */
  def liftF[F[_], A](value: F[A]): Free[F, A] = Suspend(value)

  /** Suspend the Free with the Applicative */
  def suspend[F[_], A](value: => Free[F, A])(implicit F: Applicative[F]): Free[F, A] =
    liftF(F.pure(())).flatMap(_ => value)

  /** Lift a pure value into Free */
  def pure[S[_], A](a: A): Free[S, A] = Pure(a)

  final class FreeInjectPartiallyApplied[F[_], G[_]] private[free] {
    def apply[A](fa: F[A])(implicit I : Inject[F, G]): Free[G, A] =
      Free.liftF(I.inj(fa))
  }

  def inject[F[_], G[_]]: FreeInjectPartiallyApplied[F, G] = new FreeInjectPartiallyApplied

  ....
}
```

これらのデータ型を使うには `Free.liftF` を使う:

```console
scala> import cats.free.Free
scala> :paste
sealed trait CharToy[+Next]
object CharToy {
  case class CharOutput[Next](a: Char, next: Next) extends CharToy[Next]
  case class CharBell[Next](next: Next) extends CharToy[Next]
  case class CharDone() extends CharToy[Nothing]

  implicit val charToyFunctor: Functor[CharToy] = new Functor[CharToy] {
    def map[A, B](fa: CharToy[A])(f: A => B): CharToy[B] = fa match {
        case o: CharOutput[A] => CharOutput(o.a, f(o.next))
        case b: CharBell[A]   => CharBell(f(b.next))
        case CharDone()       => CharDone()
      }
    }
  def output(a: Char): Free[CharToy, Unit] =
    Free.liftF[CharToy, Unit](CharOutput(a, ()))
  def bell: Free[CharToy, Unit] = Free.liftF[CharToy, Unit](CharBell(()))
  def done: Free[CharToy, Unit] = Free.liftF[CharToy, Unit](CharDone())
  def pure[A](a: A): Free[CharToy, A] = Free.pure[CharToy, A](a)
}
```

コマンドのシーケンスはこんな感じになる:

```console
scala> import CharToy._
scala> val subroutine = output('A')
scala> val program = for {
         _ <- subroutine
         _ <- bell
         _ <- done
       } yield ()
```

> 面白くなってきた。「まだ評価されていないもの」に対する `do` 記法を得られることができた。これは純粋なデータだ。

次に、これが本当に純粋なデータであることを証明するために `showProgram` を定義する:

```console
scala> def showProgram[R: Show](p: Free[CharToy, R]): String =
         p.fold({ r: R => "return " + Show[R].show(r) + "\n" },
           {
             case CharOutput(a, next) =>
               "output " + Show[Char].show(a) + "\n" + showProgram(next)
             case CharBell(next) =>
               "bell " + "\n" + showProgram(next)
             case CharDone() =>
               "done\n"
           })
scala> showProgram(program)
```

`Free` を使って生成したモナドがモナド則を満たしているか手で確かめてみる:

```console
scala> showProgram(output('A'))
scala> showProgram(pure('A') flatMap output)
scala> showProgram(output('A') flatMap pure)
scala> showProgram((output('A') flatMap { _ => done }) flatMap { _ => output('C') })
scala> showProgram(output('A') flatMap { _ => (done flatMap { _ => output('C') }) })
```

うまくいった。`done` が abort的な意味論になっていることにも注目してほしい。
型推論の制約上、`>>=` と `>>` をここでは使うことができなかった。

WFMM:

> Free モナドはインタプリタの良き友だ。Free モナドはインタプリタを限りなく「解放 (free) 」しつつも必要最低限のモナドの条件を満たしている。

もう一つの見方としては、`Free` は与えられたコンテナを使って構文木を作る方法を提供する。

`Free` データ型が人気を得ているのは、異なるモナドの合成した場合の制約に色んな人がハマってるからではないかと思う。
モナド変換子を使えば不可能ではないけども、型シグネチャはすぐにゴチャゴチャになるし、積み上げた型がコードの色んな所に漏れ出す。
その反面、`Free` はモナドに意味を持たせることを諦める代わりに、インタープリター関数で好き勝手できる柔軟性を得る。
例えば、テストでは逐次実行して、本番では並列で走らせるということもできるはずだ。
