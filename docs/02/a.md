---
out: making-our-own-typeclass-with-simulacrum.html
---

  [@stewoconnor]: https://twitter.com/stewoconnor
  [@stew]: https://github.com/stew
  [294]: https://github.com/typelevel/cats/pull/294
  [simulacrum]: https://github.com/mpilquist/simulacrum
  [@mpilquist]: https://github.com/mpilquist

### Making our own typeclass with simulacrum

LYAHFGG:

> In JavaScript and some other weakly typed languages, you can put almost anything inside an if expression.
> .... Even though strictly using `Bool` for boolean semantics works better in Haskell, let's try and implement that JavaScript-ish behavior anyway. For fun!

The conventional steps of defining a modular typeclass in Scala used to look like:

1. Define typeclass contract trait `Foo`.
2. Define a companion object `Foo` with a helper method `apply` that acts like `implicitly`, and a way of defining `Foo` instances typically from a function.
3. Define `FooOps` class that defines unary or binary operators.
4. Define `FooSyntax` trait that implicitly provides `FooOps` from a `Foo` instance.

Frankly, these steps are mostly copy-paste boilerplate except for the first one.
Enter Michael Pilquist ([@mpilquist][@mpilquist])'s [simulacrum][simulacrum].
simulacrum magically generates most of steps 2-4 just by putting `@typeclass` annotation.
By chance, Stew O'Connor ([@stewoconnor][@stewoconnor]/[@stew][@stew])'s [#294][294] got merged,
which refactors Cats to use it.

#### Yes-No typeclass

In any case, let's see if we can make our own truthy value typeclass.
Note the `@typeclass` annotation:

```scala
scala> import simulacrum._
scala> :paste
@typeclass trait CanTruthy[A] { self =>
  /** Return true, if `a` is truthy. */
  def truthy(a: A): Boolean
}
object CanTruthy {
  def fromTruthy[A](f: A => Boolean): CanTruthy[A] = new CanTruthy[A] {
    def truthy(a: A): Boolean = f(a)
  }
}
```

According to the [README][simulacrum], the macro will generate all the operator enrichment stuff:

```scala
// This is the supposed generated code. You don't have to write it!
object CanTruthy {
  def fromTruthy[A](f: A => Boolean): CanTruthy[A] = new CanTruthy[A] {
    def truthy(a: A): Boolean = f(a)
  }

  def apply[A](implicit instance: CanTruthy[A]): CanTruthy[A] = instance

  trait Ops[A] {
    def typeClassInstance: CanTruthy[A]
    def self: A
    def truthy: A = typeClassInstance.truthy(self)
  }

  trait ToCanTruthyOps {
    implicit def toCanTruthyOps[A](target: A)(implicit tc: CanTruthy[A]): Ops[A] = new Ops[A] {
      val self = target
      val typeClassInstance = tc
    }
  }

  trait AllOps[A] extends Ops[A] {
    def typeClassInstance: CanTruthy[A]
  }

  object ops {
    implicit def toAllCanTruthyOps[A](target: A)(implicit tc: CanTruthy[A]): AllOps[A] = new AllOps[A] {
      val self = target
      val typeClassInstance = tc
    }
  }
}
```

To make sure it works, let's define an instance for `Int` and use it. The eventual goal is to get `1.truthy` to return `true`:

```scala
scala> implicit val intCanTruthy: CanTruthy[Int] = CanTruthy.fromTruthy({
         case 0 => false
         case _ => true
       })
scala> import CanTruthy.ops._
scala> 10.truthy
```

It works. This is quite nifty.
One caveat is that this requires Macro Paradise plugin to compile. Once it's compiled the user of `CanTruthy` can use it without Macro Paradise.

### Symbolic operators

For `CanTruthy` the injected operator happened to be unary, and it matched the name of the function on the typeclass contract. simulacrum can also define operator with symbolic names using `@op` annotation:

```scala
scala> @typeclass trait CanAppend[A] {
  @op("|+|") def append(a1: A, a2: A): A
}
scala> implicit val intCanAppend: CanAppend[Int] = new CanAppend[Int] {
  def append(a1: Int, a2: Int): Int = a1 + a2
}
scala> import CanAppend.ops._
scala> 1 |+| 2
```
