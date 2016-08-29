---
out: Eval.html
---

  [769]: https://github.com/typelevel/cats/pull/769

### Eval データ型

Cats には、`Eval` という評価を制御するデータ型がある。

```scala
sealed abstract class Eval[+A] extends Serializable { self =>

  /**
   * Evaluate the computation and return an A value.
   *
   * For lazy instances (Later, Always), any necessary computation
   * will be performed at this point. For eager instances (Now), a
   * value will be immediately returned.
   */
  def value: A

  /**
   * Ensure that the result of the computation (if any) will be
   * memoized.
   *
   * Practically, this means that when called on an Always[A] a
   * Later[A] with an equivalent computation will be returned.
   */
  def memoize: Eval[A]
}
```

`Eval` 値を作成するにはいくつかの方法がある:

```scala
object Eval extends EvalInstances {

  /**
   * Construct an eager Eval[A] value (i.e. Now[A]).
   */
  def now[A](a: A): Eval[A] = Now(a)

  /**
   * Construct a lazy Eval[A] value with caching (i.e. Later[A]).
   */
  def later[A](a: => A): Eval[A] = new Later(a _)

  /**
   * Construct a lazy Eval[A] value without caching (i.e. Always[A]).
   */
  def always[A](a: => A): Eval[A] = new Always(a _)

  /**
   * Defer a computation which produces an Eval[A] value.
   *
   * This is useful when you want to delay execution of an expression
   * which produces an Eval[A] value. Like .flatMap, it is stack-safe.
   */
  def defer[A](a: => Eval[A]): Eval[A] =
    new Eval.Call[A](a _) {}

  /**
   * Static Eval instances for some common values.
   *
   * These can be useful in cases where the same values may be needed
   * many times.
   */
  val Unit: Eval[Unit] = Now(())
  val True: Eval[Boolean] = Now(true)
  val False: Eval[Boolean] = Now(false)
  val Zero: Eval[Int] = Now(0)
  val One: Eval[Int] = Now(1)

  ....
}
```

#### Eval.later

最も便利なのは、`Eval.later` で、これは名前渡しのパラメータを `lazy val` で捕獲している。

```console:new
scala> import cats._
scala> var g: Int = 0
scala> val x = Eval.later {
  g = g + 1
  g
}
scala> g = 2
scala> x.value
scala> x.value
```

`value` はキャッシュされているため、2回目の評価は走らない。

#### Eval.now

`Eval.now` は即座に評価され結果はフィールドにて捕獲されるため、これも 2回目の評価は走らない。

```console
scala> val y = Eval.now {
  g = g + 1
  g
}
scala> y.value
scala> y.value
```

#### Eval.always

`Eval.always` はキャッシュしない。

```console
scala> val z = Eval.always {
  g = g + 1
  g
}
scala> z.value
scala> z.value
```

#### スタックセーフな遅延演算

`Eval` の便利な機能は内部でトランポリンを使った `map` と `flatMap` により、スタックセーフな遅延演算をサポートすることだ。つまりスタックオーバーフローを回避できる。

また、`Eval[A]` を返す計算を遅延させるために `Eval.defer` というものもある。例えば、`List` の `foldRight` はそれを使って実装されている:

```scala
def foldRight[A, B](fa: List[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] = {
  def loop(as: List[A]): Eval[B] =
    as match {
      case Nil => lb
      case h :: t => f(h, Eval.defer(loop(t)))
    }
  Eval.defer(loop(fa))
}
```

まずはわざとスタックを溢れさせてみよう:

```scala
scala> :paste
object OddEven0 {
  def odd(n: Int): String = even(n - 1)
  def even(n: Int): String = if (n <= 0) "done" else odd(n - 1)
}

// Exiting paste mode, now interpreting.

defined object OddEven0

scala> OddEven0.even(200000)
java.lang.StackOverflowError
  at OddEven0\$.even(<console>:15)
  at OddEven0\$.odd(<console>:14)
  at OddEven0\$.even(<console>:15)
  at OddEven0\$.odd(<console>:14)
  at OddEven0\$.even(<console>:15)
  ....
```

安全版を書いてみるとこうなった:

```console
scala> :paste
object OddEven1 {
  def odd(n: Int): Eval[String] = Eval.defer {even(n - 1)}
  def even(n: Int): Eval[String] =
    Eval.now { n <= 0 } flatMap {
      case true => Eval.now {"done"}
      case _    => Eval.defer { odd(n - 1) }
    }
}
scala> OddEven1.even(200000).value
```

初期の Cats のバージョンだと上のコードでもスタックオーバーフローが発生していたが、David Gregory さんが [#769][769] で修正してくれたので、このままで動作するようになったみたいだ。
