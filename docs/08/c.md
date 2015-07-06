---
out: Free-monads.html
---

  [Free-monoids]: Free-monoids.htmls
  [@gabrielg439]: https://twitter.com/gabrielg439
  [wfmm]: http://www.haskellforall.com/2012/06/you-could-have-invented-free-monads.html
  [FreeSource]: $catsBaseUrl$free/src/main/scala/cats/free/Free.scala
  [ControlMonadFreeSource]: http://hackage.haskell.org/package/free-4.2/docs/src/Control-Monad-Free.html
  [WikipediaMonad]: http://en.wikipedia.org/wiki/Monad_(functional_programming)#Free_monads

### Free monads

We said that [free monoids][Free-monoids] are examples of free objects.
Similarly, free monads are examples of free objects.

I'm not going to get into the details, monad is a monoid in the category of endofunctors `F: C => C`,
using `F × F => F` as the binary operator.
Similar to the way we could derive `A*` from `A`,
we can derive the free monad `F*` for a given endofunctor `F`.

Here's how Haskell does it:

```haskell
data Free f a = Pure a | Free (f (Free f a))

instance Functor f => Monad (Free f) where
  return = Pure
  Pure a >>= f = f a
  Free m >>= f = Free ((>>= f) <\$> m)
```

[Wikipedia on Monad][WikipediaMonad]:

> Unlike `List`, which stores a list of values, `Free` stores a list of functors, wrapped around an initial value.
> Accordingly, the `Functor` and `Monad` instances of `Free` do nothing other than handing a given function down that list with `fmap`.

Also note that `Free` is a datatype, but
there are different free monads that gets formed for each `Functor`.

#### Why free monads matter

In practice, we can view `Free` as a clever way of forming `Monad` out of `Functor`.
This is particularly useful for interpreter pattern,
which is explained by
Gabriel Gonzalez ([@gabrielg439][@gabrielg439])'s [Why free monads matter][wfmm]:

> Let's try to come up with some sort of abstraction that represents the essence of a syntax tree. ... 
>
> Our toy language will only have three commands:

```
output b -- prints a "b" to the console
bell     -- rings the computer's bell
done     -- end of execution
```

> So we represent it as a syntax tree where subsequent commands are leaves of prior commands:

```haskell
data Toy b next =
    Output b next
  | Bell next
  | Done
```

Here's `Toy` translated into Scala as is:

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

In WFMM's original DSL, `Output b next` has two type parameters `b` and `next`.
This allows `Output` to be generic about the output type.
As demonstrated above as `Toy`, Scala can do this too.
But doing so unnecessarily complicates the demonstration of `Free` because of
Scala's handling of partially applied types. So we'll first hardcode the datatype to `Char` as follows:

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

I've added helper functions lowercase `output`, `bell`, and `done` to unify the types to `CharToy`.

#### Fix

WFMM:

> but unfortunately this doesn't work because every time I want to add a command, it changes the type.

Let's define `Fix`:

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

Again, `fix` is provided so that the type inference works.

#### FixE

We are also going to try to implement `FixE`, which adds an exception to this. Since `throw` and `catch` are reserved, I am renaming them to `throwy` and `catchy`:

```console
scala> import cats._, cats.std.all._
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

> We can only use this if Toy b is a functor, so we muddle around until we find something that type-checks (and satisfies the Functor laws).

Let's define `Functor` for `CharToy`:

```console
scala> implicit val charToyFunctor: Functor[CharToy] = new Functor[CharToy] {
         def map[A, B](fa: CharToy[A])(f: A => B): CharToy[B] = fa match {
           case o: CharToy.CharOutput[A] => CharToy.CharOutput(o.a, f(o.next))
           case b: CharToy.CharBell[A]   => CharToy.CharBell(f(b.next))
           case CharToy.CharDone()       => CharToy.CharDone()
         }
       }
```

Here's a sample usage:

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

The fact that we need to supply type parameters everywhere is a bit unfortunate.

#### Free datatype

WFMM:

> our `FixE` already exists, too, and it's called the Free monad:

```haskell
data Free f r = Free (f (Free f r)) | Pure r
```

> As the name suggests, it is automatically a monad (if `f` is a functor):

```haskell
instance (Functor f) => Monad (Free f) where
    return = Pure
    (Free x) >>= f = Free (fmap (>>= f) x)
    (Pure r) >>= f = f r
```

> The `return` was our `Throw`, and `(>>=)` was our `catch`.

The datatype in Cats is called [Free][FreeSource]:

```scala
/**
 * A free operational monad for some functor `S`. Binding is done
 * using the heap instead of the stack, allowing tail-call
 * elimination.
 */
sealed abstract class Free[S[_], A] extends Serializable {
  final def map[B](f: A => B): Free[S, B] =
    flatMap(a => Pure(f(a)))

  /**
   * Bind the given continuation to the result of this computation.
   * All left-associated binds are reassociated to the right.
   */
  final def flatMap[B](f: A => Free[S, B]): Free[S, B] = this match {
    case a: Gosub[S, A] => gosub(a.a)(x => gosub(() => a.f(x))(f))
    case a => gosub(() => a)(f)
  }

  ....
}

object Free {
  /**
   * Return from the computation with the given value.
   */
  case class Pure[S[_], A](a: A) extends Free[S, A]

  /** Suspend the computation with the given suspension. */
  case class Suspend[S[_], A](a: S[Free[S, A]]) extends Free[S, A]

  /** Call a subroutine and continue with the given function. */
  sealed abstract class Gosub[S[_], B] extends Free[S, B] {
    type C
    val a: () => Free[S, C]
    val f: C => Free[S, B]
  }

  ....
}
```

In Cats' version, the `Free` constructor is called `Free.Suspend`,
and `Pure` is called `Free.Pure`. We can re-implement the `CharToy` commands based on `Free`:

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
    Free.Suspend[CharToy, Unit](CharOutput(a, Free.Pure[CharToy, Unit](())))
  def bell: Free[CharToy, Unit] =
    Free.Suspend[CharToy, Unit](CharBell(Free.Pure[CharToy, Unit](())))
  def done: Free[CharToy, Unit] = Free.Suspend[CharToy, Unit](CharDone())
}
```

> I'll be damned if that's not a common pattern we can abstract.

Fortunately Cats ships with `liftF` that we can use.

```console
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
  def pure[A](a: A): Free[CharToy, A] = Free.Pure[CharToy, A](a)
}
```

Here's the command sequence:

```console
scala> import CharToy._
scala> val subroutine = output('A')
scala> val program = for {
         _ <- subroutine
         _ <- bell
         _ <- done
       } yield ()
```

> This is where things get magical. We now have `do` notation for something
> that hasn't even been interpreted yet: it's pure data.

Next we'd like to define `showProgram` to prove that what we have is just data:

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

We can manually check that the monad generated using `Free` satisfies the monad laws:

```console
scala> showProgram(output('A'))
scala> showProgram(pure('A') flatMap output)
scala> showProgram(output('A') flatMap pure)
scala> showProgram((output('A') flatMap { _ => done }) flatMap { _ => output('C') })
scala> showProgram(output('A') flatMap { _ => (done flatMap { _ => output('C') }) })
```

Looking good. Notice the abort-like semantics of `done`.
Also, due to type inference limitation, I was not able to use `>>=` and `>>` here.

WFMM:

> The free monad is the interpreter's best friend. Free monads "free the interpreter" as much as possible while still maintaining the bare minimum necessary to form a monad.

Another way of looking at it is that the `Free` datatype provides a way of building a syntax tree given a container.

One of the reasons the `Free` datatype is gaining popularity I think is that people
are running into the limitation of combining different monads.
It's not impossible with monad transformer, but the type signature gets hairy quickly, and the stacking leaks into
various places in code. On the other hand, `Free` essentially gives up on encoding meaning into the monad.
You gain flexibility because you can do whatever in the interpreter function, for instance run sequentially
during testing, but run in parallel for production.
