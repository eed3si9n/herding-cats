---
out: Eval.html
---

  [769]: https://github.com/typelevel/cats/pull/769

### Eval datatype

Cats also comes with `Eval` datatype that controls evaluation.

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

There are several ways to create an `Eval` value:

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

The most useful one is `Eval.later`, which captures a by-name parameter in a `lazy val`.

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

The `value` is cached, so the second evaluation doesn't happen.

#### Eval.now

`Eval.now` evaluates eagerly, and then captures the result in a field, so the second evaluation doesn't happen.

```console
scala> val y = Eval.now {
  g = g + 1
  g
}
scala> y.value
scala> y.value
```

#### Eval.always

`Eval.always` doesn't cache.

```console
scala> val z = Eval.always {
  g = g + 1
  g
}
scala> z.value
scala> z.value
```

#### stack-safe lazy computation

One useful feature of `Eval` is that it supports stack-safe lazy computation via `map` and `flatMap` methods,
which use an internal trampoline to avoid stack overflow.

You can also defer a computation which produces `Eval[A]` value using `Eval.defer`. Here's how `foldRight` is implemented for `List` for example:

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

Let's try blowing up the stack on purpose:

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

Here's my attempt at making a safer version:

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

In the earlier versions of Cats the above caused stack overflow, but as BryanM let me know in the comment, David Gregory fixed it in [#769][769], so it works now.
