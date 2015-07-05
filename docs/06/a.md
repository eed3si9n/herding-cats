---
out: do-vs-for.html
---

  [SLS_6_19]: http://www.scala-lang.org/files/archive/spec/2.11/06-expressions.html#for-comprehensions-and-for-loops
  [foco]: http://docs.scala-lang.org/overviews/core/architecture-of-scala-collections.html#factoring-out-common-operations
  [ScalaAsync]: https://github.com/scala/async
  [Effectful]: https://github.com/pelotom/effectful
  [BasicDef]: http://www.scala-sbt.org/0.13/tutorial/Basic-Def.html
  [ActMSource]: https://github.com/eed3si9n/herding-cats/blob/day6/src/main/scala/example/MonadSyntax.scala

### do vs for

There are subtle differences in Haskell's `do` notation and Scala's `for` syntax. Here's an example of `do` notation:

```haskell
foo = do
  x <- Just 3
  y <- Just "!"
  Just (show x ++ y)
```

Typically one would write `return (show x ++ y)`, but I wrote out `Just`, so it's clear that the last line is a monadic value. On the other hand, Scala would look as follows:

```console
scala> def foo = for {
         x <- Some(3)
         y <- Some("!")
       } yield x.toString + y
```

Looks similar, but there are some differences.

- Scala doesn't have a built-in `Monad` type. Instead, the compiler desugars `for` comprehensions into `map`, `withFilter`, `flatMap`, and `foreach` calls mechanically. [SLS 6.19][SLS_6_19]
- For things like `Option` and `List` that the standard library implements `map`/`flatMap`, the built-in implementations would be prioritized over the typeclasses provided by Cats.
- The Scala collection library's `map` etc accepts `CanBuildFrom`, which may convert `F[A]` into `G[B]`. See [The Architecture of Scala Collections][foco]
- `CanBuildFrom` may convert some `G[A]` into `F[B]`. 
- `yield` with a pure value is required, otherwise `for` turns into `Unit`.

Here are some demonstration of these points:

```console
scala> import collection.immutable.BitSet
scala> val bits = BitSet(1, 2, 3)
scala> for {
         x <- bits
       } yield x.toFloat
scala> for {
         i <- List(1, 2, 3)
         j <- Some(1)
       } yield i + j
scala> for {
         i <- Map(1 -> 2)
         j <- Some(3)
       } yield j
```

#### Implementing actM

There are several DSLs around in Scala that transforms imperative-looking code
into monadic or applicative function calls using macros:

- [Scala Async][ScalaAsync]
- [Effectful][Effectful]
- [sbt 0.13 syntax][BasicDef]

Covering full array of Scala syntax in the macro is hard work,
but by copy-pasting code from Async and Effectful I put together
[a toy macro][ActMSource] that supports only simple expressions and `val`s.
I'll omit the details, but the key function is this:

```scala
  def transform(group: BindGroup, isPure: Boolean): Tree =
    group match {
      case (binds, tree) =>
        binds match {
          case Nil =>
            if (isPure) q"""\$monadInstance.pure(\$tree)"""
            else tree
          case (name, unwrappedFrom) :: xs =>
            val innerTree = transform((xs, tree), isPure)
            val param = ValDef(Modifiers(Flag.PARAM), name, TypeTree(), EmptyTree)
            q"""\$monadInstance.flatMap(\$unwrappedFrom) { \$param => \$innerTree }"""
        }
    }
```

Here's how we can use `actM`:

```console
scala> import cats._, cats.std.all._
scala> :paste
object Catnip {
  implicit class IdOp[A](val a: A) extends AnyVal {
    def some: Option[A] = Some(a)
  }
  def none[A]: Option[A] = None
}
import Catnip._
scala> import example.MonadSyntax._
scala> actM[Option, String] {
         val x = 3.some.next
         val y = "!".some.next
         x.toString + y
       }
```

`fa.next` expands to a `Monad[F].flatMap(fa)()` call.
So the above code expands into:

```console
scala> Monad[Option].flatMap[String, String]({
         val fa0: Option[Int] = 3.some
         Monad[Option].flatMap[Int, String](fa0) { (arg0: Int) => {
           val next0: Int = arg0
           val x: Int = next0
           val fa1: Option[String] = "!".some
           Monad[Option].flatMap[String, String](fa1)((arg1: String) => {
             val next1: String = arg1
             val y: String = next1
             Monad[Option].pure[String](x.toString + y)
           })
         }}
       }) { (arg2: String) => Monad[Option].pure[String](arg2) }
```

Let's see if this can prevent auto conversion from `Option` to `List`.

```console
scala> actM[List, Int] {
         val i = List(1, 2, 3).next
         val j = 1.some.next
         i + j
       }
```

The error message is a bit rough, but we were able to catch this at compile-time.
This will also work for any monads including `Future`.

```console
scala> :paste
val x = {
  import scala.concurrent.{ExecutionContext, Future}
  import ExecutionContext.Implicits.global
  actM[Future, Int] {
    val i = Future { 1 }.next
    val j = Future { 2 }.next
    i + j
  }
}
scala> x.value
```

This macro is incomplete toy code, but it demonstrates potential usefulness for having something like this.
