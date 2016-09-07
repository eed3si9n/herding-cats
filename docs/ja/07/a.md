---
out: State.html
---

  [@ceedubs]: https://github.com/ceedubs
  [302]: https://github.com/typelevel/cats/pull/302
  [@retronym]: https://twitter.com/retronym
  [SI-7139]: https://issues.scala-lang.org/browse/SI-7139
  [322]: https://github.com/typelevel/cats/pull/322

### State データ型

不変 (immutable) なデータ構造を使ってコードを書いていると、
何らかの状態を表す値を引き回すというパターンがよく発生する。
僕が好きな例はテトリスだ。テトリスの関数型の実装があるとして、
`Tetrix.init` が初期状態を作って、他に色々な状態遷移関数が変換された状態と何らかの戻り値を返すとする:

```scala
val (s0, _) = Tetrix.init()
val (s1, _) = Tetrix.nextBlock(s0)
val (s2, moved0) = Tetrix.moveBlock(s1, LEFT)
val (s3, moved1) =
  if (moved0) Tetrix.moveBlock(s2, LEFT)
  else (s2, moved0)
```

状態オブジェクト (`s0`, `s1`, `s2`, ...) の引き回しはエラーの温床的なボイラープレートとなる。
状態の明示的な引き回しを自動化するのがゴールだ。

本にあわせてここではスタックの例を使う。まずは、`State` 無しでの実装:

```console:new
scala> type Stack = List[Int]
scala> def pop(s0: Stack): (Stack, Int) =
         s0 match {
           case x :: xs => (xs, x)
           case Nil     => sys.error("stack is empty")
         }
scala> def push(s0: Stack, a: Int): (Stack, Unit) = (a :: s0, ())
scala> def stackManip(s0: Stack): (Stack, Int) = {
         val (s1, _) = push(s0, 3)
         val (s2, a) = pop(s1)
         pop(s2)
       }
scala> stackManip(List(5, 8, 2, 1))
```

### State と StateT データ型


[すごいHaskellたのしく学ぼう](http://www.amazon.co.jp/dp/4274068854) 曰く:

> そこで Haskell には `State` モナドが用意されています。これさえあれば、状態付きの計算などいとも簡単。しかもすべてを純粋に保ったまま扱えるんです。...
>
> 状態付きの計算とは、ある状態を取って、更新された状態と一緒に計算結果を返す関数として表現できるでしょう。そんな関数の型は、こうなるはずです。

```haskell
s -> (a, s)
```

`State` は状態付きの計算 `S => (S, A)` をカプセル化するデータ型だ。
`State` は型 `S` で表される状態を渡すモナドを**形成する**。
Haskell はこの混乱を避けるために、`Stater` とか `Program` という名前を付けるべきだったと思うけど、
既に `State` という名前が定着してるので、もう遅いだろう。

Cody Allen ([@ceedubs][@ceedubs]) さんが Cats に
`State`/`StateT` を実装する [#302][302] を投げていて、それが最近マージされた。(Erik サンキュー)
`State` はただの型エイリアスとなっている:

```scala
package object data {
  ....
  type State[S, A] = StateT[Eval, S, A]
  object State extends StateFunctions
}
```

`StateT` はモナド変換子で、これは他のデータ型を受け取る型コンストラクタだ。
`State` はこれに `Trampoline` 部分適用している。
`Eval` は in-memory でコール・スタックをエミュレートしてスタックオーバーフローを回避するための機構だ。
以下が `StateT` の定義:

```scala
final class StateT[F[_], S, A](val runF: F[S => F[(S, A)]]) {
  ....
}

object StateT extends StateTInstances {
  def apply[F[_], S, A](f: S => F[(S, A)])(implicit F: Applicative[F]): StateT[F, S, A] =
    new StateT(F.pure(f))

  def applyF[F[_], S, A](runF: F[S => F[(S, A)]]): StateT[F, S, A] =
    new StateT(runF)

  /**
   * Run with the provided initial state value
   */
  def run(initial: S)(implicit F: FlatMap[F]): F[(S, A)] =
    F.flatMap(runF)(f => f(initial))

  ....
}


```

`State` 値を構築するには、状態遷移関数を `State.apply` に渡す:

```scala
private[data] abstract class StateFunctions {
  def apply[S, A](f: S => (S, A)): State[S, A] =
    StateT.applyF(Now((s: S) => Now(f(s))))
  
  ....
}
```

`State` の実装はできたてなので、まだ小慣れない部分もあったりする。
REPL から `State` を使ってみると、最初の state は成功するけど、2つ目が失敗するという奇妙な動作に遭遇した。
[@retronym][@retronym] に
[SI-7139: Type alias and object with the same name cause type mismatch in REPL][SI-7139]
のことを教えてもらって、[#322][322] として回避することができた。

`State` を使ってスタックを実装してみよう:

```console:new
scala> type Stack = List[Int]
scala> import cats._, cats.data.State, cats.instances.all._
scala> val pop = State[Stack, Int] {
         case x :: xs => (xs, x)
         case Nil     => sys.error("stack is empty")
       }
scala> def push(a: Int) = State[Stack, Unit] {
         case xs => (a :: xs, ())
       }
```

これらがプリミティブ・プログラムだ。
これらをモナド的に合成することで複合プログラムを構築することができる。

```console
scala> def stackManip: State[Stack, Int] = for {
         _ <- push(3)
         a <- pop
         b <- pop
       } yield(b)
scala> stackManip.run(List(5, 8, 2, 1)).value
```

最初の `run` は `SateT` のためで、2つ目の `run` は `Eval` を最後まで実行する。

`push` も `pop` も純粋関数型だけども、状態オブジェクト (`s0`, `s1`, ...)
の引き回しをしなくても済むようになった。

### 状態の取得と設定

LYAHFGG:

> `Control.Monad.State` モジュールは、2つの便利な関数 `get` と `put` を備えた、`MonadState` という型クラスを提供しています。

`State` object は、いくつかのヘルパー関数を定義する:

```scala
private[data] abstract class StateFunctions {

  def apply[S, A](f: S => (S, A)): State[S, A] =
    StateT.applyF(Now((s: S) => Now(f(s))))

  /**
   * Return `a` and maintain the input state.
   */
  def pure[S, A](a: A): State[S, A] = State(s => (s, a))

  /**
   * Modify the input state and return Unit.
   */
  def modify[S](f: S => S): State[S, Unit] = State(s => (f(s), ()))

  /**
   * Inspect a value from the input state, without modifying the state.
   */
  def inspect[S, T](f: S => T): State[S, T] = State(s => (s, f(s)))

  /**
   * Return the input state without modifying it.
   */
  def get[S]: State[S, S] = inspect(identity)

  /**
   * Set the state to `s` and return Unit.
   */
  def set[S](s: S): State[S, Unit] = State(_ => (s, ()))
}
```

ちょっと最初は分かりづらかった。だけど、`State` モナドは状態遷移関数と戻り値をカプセル化していることを思い出してほしい。
そのため、状態というコンテキストでの `State.get` は、状態はそのままにして、状態を戻り値として返すというものだ。

似たように、状態というコンテキストでの `State.set(s)` は、状態を `s` で上書きして、戻り値として `()` を返す。

本で出てくる `stackStack` 関数を実装して具体例でみてみよう。

```console
scala> import cats.syntax.eq._
scala> def stackyStack: State[Stack, Unit] = for {
         stackNow <- State.get[Stack]
         r <- if (stackNow === List(1, 2, 3)) State.set[Stack](List(8, 3, 1))
              else State.set[Stack](List(9, 2, 1))
       } yield r
scala> stackyStack.run(List(1, 2, 3)).value
```

`pop` と `push` も `get` と `set` を使って実装できる:

```console
scala> val pop: State[Stack, Int] = for {
         s <- State.get[Stack]
         (x :: xs) = s
         _ <- State.set[Stack](xs)
       } yield x
scala> def push(x: Int): State[Stack, Unit] = for {
         xs <- State.get[Stack]
         r <- State.set(x :: xs)
       } yield r
```

見ての通りモナドそのものはあんまり大したこと無い (タプルを返す関数のカプセル化) けど、連鎖することでボイラープレートを省くことができた。

### 状態の抽出と変更

`State.get` と `State.set` の少しだけ高度なバリエーションとして、
`State.extract(f)` と `State.modify(f)` がある。

`State.extract(f)` は関数 `f: S => T` を状態 `s` に適用した結果を戻り値として返すが、状態そのものは変更しない。

逆に、`State.modify` は関数 `f: S => T` を状態 `s` に適用した結果を保存するが、戻り値として `()` を返す。
