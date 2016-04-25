---
out: State.html
---

  [@ceedubs]: https://github.com/ceedubs
  [302]: https://github.com/typelevel/cats/pull/302
  [@retronym]: https://twitter.com/retronym
  [SI-7139]: https://issues.scala-lang.org/browse/SI-7139
  [322]: https://github.com/typelevel/cats/pull/322
  [fafmm]: http://learnyouahaskell.com/for-a-few-monads-more

### State datatype

When writing code using an immutable data structure,
one pattern that arises often is passing of a value that represents some state.
The example I like to use is Tetris. Imagine a functional implementation of Tetris
where `Tetrix.init` creates the initial state, and then various
transition functions return a transformed state and some return value:

```scala
val (s0, _) = Tetrix.init()
val (s1, _) = Tetrix.nextBlock(s0)
val (s2, moved0) = Tetrix.moveBlock(s1, LEFT)
val (s3, moved1) =
  if (moved0) Tetrix.moveBlock(s2, LEFT)
  else (s2, moved0)
```

The passing of the state objects (`s0`, `s1`, `s2`, ...) becomes error-prone boilerplate.
The goal is to automate the explicit passing of the states.

To follow along the book, we'll use the stack example from the book.
Here's an implementation without using `State`.

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

### State and StateT datatype

[Learn You a Haskell for Great Good][fafmm] says:

> Haskell features the `State` monad, which makes dealing with stateful problems a breeze while still keeping everything nice and pure. ....
>
>  We'll say that a stateful computation is a function that takes some state and returns a value along with some new state. That function would have the following type:

```haskell
s -> (a, s)
```

`State` is a datatype that encapsulates a stateful computation: `S => (S, A)`.
`State` *forms* a monad which passes along the states represented by the type `S`.
Haskell should've named this `Stater` or `Program` to avoid the confusion,
but now people already know this by `State`, so it's too late.

Cody Allen ([@ceedubs][@ceedubs]) had a pull request open for `State`/`StateT` on
Cats [#302][302], which was merged recently. (Thanks, Erik)
As it happens, `State` is just a type alias:

```scala
package object state {
  type State[S, A] = StateT[Trampoline, S, A]
}
```

`StateT` is a monad transformer, a type constructor for other datatypes.
`State` partially applies `StateT` with `Trampoline`,
which emulates a call stack with heap memory to prevent overflow.
Here's the definition of `StateT`:

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

To construct a `State` value, you pass the state transition function to `State.apply`.

```scala
object State {
  def apply[S, A](f: S => (S, A)): State[S, A] =
    StateT.applyF(Trampoline.done((s: S) => Trampoline.done(f(s))))
  
  ....
}
```

As the `State` implementation is fresh, a few bumps on the road are expected.
When I tried using `State` on the REPL, I ran into an odd behavior where I can create
one state, but not the second. [@retronym][@retronym] pointed me to
[SI-7139: Type alias and object with the same name cause type mismatch in REPL][SI-7139], which I was able to workaround as [#322][322].

Let's consider how to implement stack with `State`:

```console:new
scala> type Stack = List[Int]
scala> import cats._, cats.state._, cats.std.all._
scala> val pop = State[Stack, Int] {
         case x :: xs => (xs, x)
         case Nil     => sys.error("stack is empty")
       }
scala> def push(a: Int) = State[Stack, Unit] {
         case xs => (a :: xs, ())
       }
```

These are the primitive programs. Now we can construct
compound programs by composing the monad.

```console
scala> def stackManip: State[Stack, Int] = for {
         _ <- push(3)
         a <- pop
         b <- pop
       } yield(b)
scala> stackManip.run(List(5, 8, 2, 1)).run
```

The first `run` is for `StateT`, and the second is to `run` until the end `Trampoline`.

Both `push` and `pop` are still purely functional, and we 
were able to eliminate explicitly passing the state object (`s0`, `s1`, ...).

### Getting and setting the state

LYAHFGG:

> The `Control.Monad.State` module provides a type class that's called `MonadState` and it features two pretty useful functions, namely `get` and `put`.

The `State` object defines a few helper functions:

```scala
object State {
  def apply[S, A](f: S => (S, A)): State[S, A] =
    StateT.applyF(Trampoline.done((s: S) => Trampoline.done(f(s))))

  /**
   * Modify the input state and return Unit.
   */
  def modify[S](f: S => S): State[S, Unit] = State(s => (f(s), ()))

  /**
   * Extract a value from the input state, without modifying the state.
   */
  def extract[S, T](f: S => T): State[S, T] = State(s => (s, f(s)))

  /**
   * Return the input state without modifying it.
   */
  def get[S]: State[S, S] = extract(identity)

  /**
   * Set the state to `s` and return Unit.
   */
  def set[S](s: S): State[S, Unit] = State(_ => (s, ()))
}
```

These are confusing at first. But remember that the `State` monad encapsulates
a pair of a state transition function and a return value.
So `State.get` keeps the state as is, and returns it.

Similarly, `State.set(s)` in this context means to overwrite the state with `s` and return `()`.

Let's try using them with the `stackyStack` example from the book:

```console
scala> import cats.syntax.eq._
scala> def stackyStack: State[Stack, Unit] = for {
         stackNow <- State.get[Stack]
         r <- if (stackNow === List(1, 2, 3)) State.set[Stack](List(8, 3, 1))
              else State.set[Stack](List(9, 2, 1))
       } yield r
scala> stackyStack.run(List(1, 2, 3)).run
```

We can also implement both `pop` and `push` in terms of `get` and `put`:

```console
scala> val pop: State[Stack, Int] = for {
         s <- State.get[Stack]
         (x :: xs) = s
       } yield x
scala> def push(x: Int): State[Stack, Unit] = for {
         xs <- State.get[Stack]
         r <- State.set(x :: xs)
       } yield r
```

As you can see, the `State` monad on its own doesn't do much
(encapsulate a state transition function and a return value),
but by chaining them we can remove some boilerplates.

### Extracting and modifying the state

`State.extract(f)` and `State.modify(f)` are slightly more 
advanced variants of `State.get` and `State.set(s)`.

`State.extract(f)` applies the function `f: S => T` to `s`,
and returns the result without modifying the state itself.

`State.modify` applies the function `f: S => T` to `s`,
saves the result as the new state, and returns `()`.
