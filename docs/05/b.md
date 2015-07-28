
### Monad

Earlier I wrote that Cats breaks down the Monad typeclass into two typeclasses: `FlatMap` and `Monad`.
The `FlatMap`-`Monad` relationship forms a parallel with the `Apply`-`Applicative` relationship:

```scala
@typeclass trait Monad[F[_]] extends FlatMap[F] with Applicative[F] {
  ....
}
```

`Monad` is a `FlatMap` with `pure`. Unlike Haskell, `Monad[F]` extends `Applicative[F]` so there's no `return` vs `pure` discrepancies.

#### Walk the line

<div class="floatingimage">
<img src="files/day5-with-birds.jpg">
<div class="credit">Derived from <a href="https://www.flickr.com/photos/72562013@N06/10016847314/">Bello Nock's Sky Walk</a> by Chris Phutully</div>
</div>

LYAHFGG:

> Let's say that [Pierre] keeps his balance if the number of birds on the left side of the pole and on the right side of the pole is within three. So if there's one bird on the right side and four birds on the left side, he's okay. But if a fifth bird lands on the left side, then he loses his balance and takes a dive.

Now let's try implementing the `Pole` example from the book.

```console:new
scala> import cats._, cats.std.all._, cats.syntax.flatMap._
scala> :paste
object Catnip {
  implicit class IdOp[A](val a: A) extends AnyVal {
    def some: Option[A] = Some(a)
  }
  def none[A]: Option[A] = None
}
import Catnip._
scala> type Birds = Int
scala> case class Pole(left: Birds, right: Birds)
```

I don't think it's common to alias `Int` like this in Scala, but we'll go with the flow. I am going to turn `Pole` into a case class so I can implement `landLeft` and `landRight` as methods:

```console
scala> :paste
case class Pole(left: Birds, right: Birds) {
  def landLeft(n: Birds): Pole = copy(left = left + n)
  def landRight(n: Birds): Pole = copy(right = right + n)
}
```

I think it looks better with some OO:

```console
scala> Pole(0, 0).landLeft(2)
scala> Pole(1, 2).landRight(1)
scala> Pole(1, 2).landRight(-1)
```

We can chain these too:

```console
scala> Pole(0, 0).landLeft(1).landRight(1).landLeft(2)
scala> Pole(0, 0).landLeft(1).landRight(4).landLeft(-1).landRight(-2)
```

As the book says, an intermediate value has failed but the calculation kept going. Now let's introduce failures as `Option[Pole]`:

```console
scala> :paste
case class Pole(left: Birds, right: Birds) {
  def landLeft(n: Birds): Option[Pole] =
    if (math.abs((left + n) - right) < 4) copy(left = left + n).some
    else none[Pole]
  def landRight(n: Birds): Option[Pole] =
    if (math.abs(left - (right + n)) < 4) copy(right = right + n).some
    else none[Pole]
  }
scala> Pole(0, 0).landLeft(2)
scala> Pole(0, 3).landLeft(10)
```

Now we can chain the `landLeft`/`landRight` using `flatMap` or its symbolic alias `>>=`.

```console
scala> val rlr = Monad[Option].pure(Pole(0, 0)) >>= {_.landRight(2)} >>=
  {_.landLeft(2)} >>= {_.landRight(2)}
```

Let's see if monadic chaining simulates the pole balancing better:

```console
scala> val lrlr = Monad[Option].pure(Pole(0, 0)) >>= {_.landLeft(1)} >>=
  {_.landRight(4)} >>= {_.landLeft(-1)} >>= {_.landRight(-2)}
```

It works. Take time to understand this example because this example
highlights what a monad is.

1. First, `pure` puts `Pole(0, 0)` into a default context: `Pole(0, 0).some`.
2. Then, `Pole(0, 0).some >>= {_.landLeft(1)}` happens. Since it's a `Some` value, `_.landLeft(1)` gets applied to `Pole(0, 0)`, resulting to `Pole(1, 0).some`.
3. Next, `Pole(1, 0).some >>= {_.landRight(4)}` takes place. The result is `Pole(1, 4).some`. Now we at at the max difference between left and right.
4. `Pole(1, 4).some >>= {_.landLeft(-1)}` happens, resulting to `none[Pole]`. The difference is too great, and pole becomes off balance.
5. `none[Pole] >>= {_.landRight(-2)}` results automatically to `none[Pole]`.

In this chain of monadic functions, the effect from one function is carried over to the next.

#### Banana on wire

LYAHFGG:

> We may also devise a function that ignores the current number of birds on the balancing pole and just makes Pierre slip and fall. We can call it `banana`.

Here's the `banana` that always fails:

```console
scala> :paste
case class Pole(left: Birds, right: Birds) {
  def landLeft(n: Birds): Option[Pole] =
    if (math.abs((left + n) - right) < 4) copy(left = left + n).some
    else none[Pole]
  def landRight(n: Birds): Option[Pole] =
    if (math.abs(left - (right + n)) < 4) copy(right = right + n).some
    else none[Pole]
  def banana: Option[Pole] = none[Pole]
}
scala> val lbl = Monad[Option].pure(Pole(0, 0)) >>= {_.landLeft(1)} >>=
  {_.banana} >>= {_.landRight(1)}
```

LYAHFGG:

> Instead of making functions that ignore their input and just return a predetermined monadic value, we can use the `>>` function.

Here's how `>>` behaves with `Option`:

```console
scala> none[Int] >> 3.some
scala> 3.some >> 4.some
scala> 3.some >> none[Int]
```

Let's try replacing `banana` with `>> none[Pole]`:

```console:error
scala> val lbl = Monad[Option].pure(Pole(0, 0)) >>= {_.landLeft(1)} >>
  none[Pole] >>= {_.landRight(1)}
```

The type inference broke down all the sudden. The problem is likely the operator precedence. [Programming in Scala](http://www.artima.com/pins1ed/basic-types-and-operations.html) says:

> The one exception to the precedence rule, alluded to above, concerns assignment operators, which end in an equals character. If an operator ends in an equals character (`=`), and the operator is not one of the comparison operators `<=`, `>=`, `==`, or `!=`, then the precedence of the operator is the same as that of simple assignment (`=`). That is, it is lower than the precedence of any other operator.

Note: The above description is incomplete. Another exception from the assignment operator rule is if it starts with (`=`) like `===`.


Because `>>=` (bind) ends in the equals character, its precedence is the lowest, which forces `({_.landLeft(1)} >> (none: Option[Pole]))` to evaluate first. There are a few unpalatable work arounds. First we can use dot-and-parens like normal method calls:

```console
scala> Monad[Option].pure(Pole(0, 0)).>>=({_.landLeft(1)}).>>(none[Pole]).>>=({_.landRight(1)})
```

Or we can recognize the precedence issue and place parens around just the right place:

```console
scala> (Monad[Option].pure(Pole(0, 0)) >>= {_.landLeft(1)}) >> none[Pole] >>= {_.landRight(1)}
```

Both yield the right result.

#### for comprehension

LYAHFGG:

> Monads in Haskell are so useful that they got their own special syntax called `do` notation.

First, let's write the nested lambda:

```console
scala> import cats._, cats.syntax.show._
scala> 3.some >>= { x => "!".some >>= { y => (x.show + y).some } }
```

By using `>>=`, any part of the calculation can fail:

```console
scala> 3.some >>= { x => none[String] >>= { y => (x.show + y).some } }
scala> (none: Option[Int]) >>= { x => "!".some >>= { y => (x.show + y).some } }
scala> 3.some >>= { x => "!".some >>= { y => none[String] } }
```

Instead of the `do` notation in Haskell, Scala has the `for` comprehension, which does similar things:

```console
scala> for {
         x <- 3.some
         y <- "!".some
       } yield (x.show + y)
```

LYAHFGG:

> In a `do` expression, every line that isn't a `let` line is a monadic value.

That's not quite accurate for `for`, but we can come back to this later.

#### Pierre returns

LYAHFGG:

> Our tightwalker's routine can also be expressed with `do` notation.

```console
scala> def routine: Option[Pole] =
         for {
           start <- Monad[Option].pure(Pole(0, 0))
           first <- start.landLeft(2)
           second <- first.landRight(2)
           third <- second.landLeft(1)
         } yield third
scala> routine
```

We had to extract `third` since `yield` expects `Pole` not `Option[Pole]`.

LYAHFGG:

> If we want to throw the Pierre a banana peel in `do` notation, we can do the following:

```console
scala> def routine: Option[Pole] =
         for {
           start <- Monad[Option].pure(Pole(0, 0))
           first <- start.landLeft(2)
           _ <- none[Pole]
           second <- first.landRight(2)
           third <- second.landLeft(1)
         } yield third
scala> routine
```

#### Pattern matching and failure

LYAHFGG:

> In `do` notation, when we bind monadic values to names, we can utilize pattern matching, just like in let expressions and function parameters.

```console
scala> def justH: Option[Char] =
         for {
           (x :: xs) <- "hello".toList.some
         } yield x
scala> justH
```

> When pattern matching fails in a do expression, the `fail` function is called. It's part of the `Monad` type class and it enables failed pattern matching to result in a failure in the context of the current monad instead of making our program crash.

```console
scala> def wopwop: Option[Char] =
         for {
           (x :: xs) <- "".toList.some
         } yield x
scala> wopwop
```

The failed pattern matching returns `None` here. This is an interesting aspect of `for` syntax that I haven't thought about, but totally makes sense.

#### Monad laws

Monad had three laws:

- left identity: `(Monad[F].pure(x) flatMap {f}) === f(x)`
- right identity: `(m flatMap {Monad[F].pure(_)}) === m`
- associativity: `(m flatMap f) flatMap g === m flatMap { x => f(x) flatMap {g} }`

LYAHFGG:

> The first monad law states that if we take a value, put it in a default context with `return` and then feed it to a function by using `>>=`, it's the same as just taking the value and applying the function to it. 

```console
scala> import cats.syntax.eq._
scala> assert { (Monad[Option].pure(3) >>= { x => (x + 100000).some }) ===
         ({ (x: Int) => (x + 100000).some })(3) }
```

LYAHFGG:

> The second law states that if we have a monadic value and we use `>>=` to feed it to `return`, the result is our original monadic value.

```console
scala> assert { ("move on up".some >>= {Monad[Option].pure(_)}) === "move on up".some }
```

LYAHFGG:

> The final monad law says that when we have a chain of monadic function applications with `>>=`, it shouldn't matter how they're nested. 

```console
scala> Monad[Option].pure(Pole(0, 0)) >>= {_.landRight(2)} >>= {_.landLeft(2)} >>= {_.landRight(2)}
scala> Monad[Option].pure(Pole(0, 0)) >>= { x =>
       x.landRight(2) >>= { y =>
       y.landLeft(2) >>= { z =>
       z.landRight(2)
       }}}
```

These laws look might look familiar if you remember monoid laws from day 4.
That's because monad is a special kind of a monoid.

You might be thinking, "But wait. Isn't `Monoid` for kind `A` (or `*`)?"
Yes, you're right. And that's the difference between monoid with lowercase *m* and `Monoid[A]`.
Haskell-style functional programming allows you to abstract out the container and execution model.
In category theory, a notion like monoid can be generalized to `A`, `F[A]`, `F[A] => F[B]` and all sorts of things.
Instead of thinking "omg so many laws," know that there's an underlying structure that connects many of them.

Here's how to check Monad laws using Discipline:

```scala
scala> import cats._, cats.std.all._, cats.laws.discipline.MonadTests
import cats._
import cats.std.all._
import cats.laws.discipline.MonadTests

scala> val rs = MonadTests[Option].monad[Int, Int, Int]
rs: cats.laws.discipline.MonadTests[Option]#RuleSet = cats.laws.discipline.MonadTests\$\$anon\$2@35e8de37

scala> rs.all.check
+ monad.applicative homomorphism: OK, passed 100 tests.
+ monad.applicative identity: OK, passed 100 tests.
+ monad.applicative interchange: OK, passed 100 tests.
+ monad.applicative map: OK, passed 100 tests.
+ monad.apply composition: OK, passed 100 tests.
+ monad.covariant composition: OK, passed 100 tests.
+ monad.covariant identity: OK, passed 100 tests.
+ monad.flatMap associativity: OK, passed 100 tests.
+ monad.flatMap consistent apply: OK, passed 100 tests.
+ monad.invariant composition: OK, passed 100 tests.
+ monad.invariant identity: OK, passed 100 tests.
+ monad.monad left identity: OK, passed 100 tests.
+ monad.monad right identity: OK, passed 100 tests.
```
