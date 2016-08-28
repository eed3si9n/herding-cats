---
out: applicative-wordcount.html
---

  [388]: https://github.com/typelevel/cats/pull/388

### Applicative wordcount

Skipping over to section 6 of EIP "Modular programming with applicative functors."

EIP:

> There is an additional benefit of applicative functors over monads,
> which concerns the modular development of complex iterations from simpler aspects.
> ....
>
> As an illustration, we consider the Unix word-counting utility `wc`,
> which computes the numbers of characters, words and lines in a text file.

We can translate the full example using applicative function composition,
which is only available on my personal branch. (PR [#388][388] is pending)

#### Modular iterations, applicatively

```console:new
scala> import cats._, cats.instances.all._
scala> import cats.data.{ Func, AppFunc, Const }
scala> import Func.{ appFunc, appFuncU }
```

> The character-counting slice of the `wc` program accumulates a result in the integers-as-monoid applicative functor:

Here's a type alias to treat `Int` as a monoidal applicative:

```console
scala> type Count[A] = Const[Int, A]
```

In the above, `A` is a phantom type we don't need, so let's just hardcode it to `Unit`:

```console
scala> def liftInt(i: Int): Count[Unit] = Const(i)
scala> def count[A](a: A): Count[Unit] = liftInt(1)
```

> The body of the iteration simply yields 1 for every element:

```console
scala> val countChar: AppFunc[Count, Char, Unit] = appFunc(count)
```

To use this `AppFunc`, we would call `traverse` with `List[Char]`.
Here's a quote I found from Hamlet.

```console
scala> val text = ("Faith, I must leave thee, love, and shortly too.\n" +
                  "My operant powers their functions leave to do.\n").toList
scala> countChar traverse text
```

This looks ok.

> Counting the lines (in fact, the newline characters, thereby ignoring a final 'line' that is not terminated with a newline character) is similar:
> the difference is simply what number to use for each element, namely 1 for a newline and 0 for anything else.

```console
scala> import cats.syntax.eq._
scala> def testIf(b: Boolean): Int = if (b) 1 else 0
scala> val countLine: AppFunc[Count, Char, Unit] =
         appFunc { (c: Char) => liftInt(testIf(c === '\n')) }
```

Again, to use this we'll just call `traverse`:

```console
scala> countLine traverse text
```

> Counting the words is trickier, because it necessarily involves state.
> Here, we use the `State` monad with a boolean state, indicating whether we are currently within a word,
> and compose this with the applicative functor for counting.

```console
scala> def isSpace(c: Char): Boolean = (c === ' ' || c === '\n' || c === '\t')
scala> val countWord =
         appFuncU { (c: Char) =>
           import cats.data.State.{ get, set }
           for {
             x <- get[Boolean]
             y = !isSpace(c)
             _ <- set(y)
           } yield testIf(y && !x)
         } andThen appFunc(liftInt)
```

Traversing this `AppFunc` returns a `State` datatype:

```console
scala> val x = countWord traverse text
```

We then need to run this state machine with an initial value `false` to get the result:

```console
scala> x.runA(false).value
```

17 words.

Like we did with `shape` and `content`, we can *fuse* the traversal into one shot
by combining the applicative functions.

```console
scala> val countAll = countWord product countLine product countChar
scala> val allResults = countAll traverse text
scala> val charCount = allResults.second
scala> val lineCount = allResults.first.second
scala> val wordCountState = allResults.first.first
scala> val wordCount = wordCountState.runA(false).value
```

EIP:

> Applicative functors have a richer algebra of composition operators,
> which can often replace the use of monad transformers;
> there is the added advantage of being able to compose applicative but non-monadic computations.

That's it for today.
