---
out: import-guide.html
---

  [ImplicitsSource]: $catsBaseUrl$/core/src/main/scala/cats/implicits/package.scala

### Import guide

Cats makes heavy use of implicits. Both as a user and an extender of the library, it will be useful to have general idea on where things are coming from.
If you're just starting out with Cats, you can use the following the imports and skip this page:

```scala
scala> import cats._, cats.data._, cats.implicits._
```

### Implicits review

Let's quickly review Scala's imports and implicits! In Scala, imports are used for two purposes:

1. To include names of values and types into the scope.
2. To include implicits into the scope.

Implicits are for 4 purposes that I can think of:

1. To provide typeclass instances.
2. To inject methods and operators. (static monkey patching)
3. To declare type constraints.
4. To retrieve type information from compiler.

Implicits are selected in the following precedence:

1. Values and converters accessible without prefix via local declaration, imports, outer scope, inheritance, and current package object. Inner scope can shadow values when they are named the same.
2. Implicit scope. Values and converters declared in companion objects and package object of the type, its parts, or super types.

### import cats._

Now let's see what gets imported with `import cats._`.

First, the names. Typeclasses like `Show[A]` and `Functor[F[_]]` are implemented as trait, and are defined under the `cats` package. So instead of writing `cats.Show[A]` we can write `Show[A]`.

Next, also the names, but type aliases. `cats`'s package object declares type aliases like `Eq[A]` and `~>[F[_], G[_]]`. Again, these can also be accessed as `cats.Eq[A]` if you want.

Finally, `catsInstancesForId` is defined as typeclass instance of `Id[A]` for `Traverse[F[_]]`, `Monad[F[_]]` etc, but it's not relevant. By virtue of declaring an instance within its package object it will be available, so importing doesn't add much. Let's check this:

```scala
scala> cats.Functor[cats.Id]
res0: cats.Functor[cats.Id] = cats.package\$\$anon\$1@3c201c09
```

No import needed, which is a good thing. So, the merit of `import cats._` is for convenience, and it's optional.

### import cats.data._

Next let's see what gets imported with `import cats.data._`.

First, more names. There are custom datatype defined under the `cats.data` package such as `Validated[+E, +A]`.

Next, the type aliases. The `cats.data` package object defines type aliases such as `Reader[A, B]`, which is treated as a specialization of `ReaderT` transformer. We can still write this as `cats.data.Reader[A, B]`.

### import cats.implicits._

What then is `import cats.implicits._` doing? Here's the definition of [implicits object][ImplicitsSource]:

```scala
package cats

object implicits extends syntax.AllSyntax with instances.AllInstances
```

This is quite a nice way of organizing the imports. `implicits` object itself doesn't define anythig and it just mixes in the traits. We are going to look at each traits in detail, but they can also be imported a la carte, dim sum style. Back to the full course.

#### cats.instances.AllInstances

Thus far, I have been intentionally conflating the concept of typeclass instances and method injection (aka enrich my library). But the fact that `(Int, +)` forms a `Monoid` and that `Monoid` introduces `|+|` operator are two different things.

One of the interesting design of Cats is that it rigorously separates the two concepts into "instance" and "syntax." Even if it makes logical sense to some users, the choice of symbolic operators can often be a point of contention with any libraries. Libraries and tools such as sbt, dispatch, and specs introduce its own DSL, and their effectiveness have been hotly debated.

`AllInstances` is a trait that mixes in all the typeclass instances for built-in datatypes such as `Either[A, B]` and `Option[A]`.

```scala
package cats
package instances

trait AllInstances
  extends FunctionInstances
  with    StringInstances
  with    EitherInstances
  with    ListInstances
  with    OptionInstances
  with    SetInstances
  with    StreamInstances
  with    VectorInstances
  with    AnyValInstances
  with    MapInstances
  with    BigIntInstances
  with    BigDecimalInstances
  with    FutureInstances
  with    TryInstances
  with    TupleInstances
  with    UUIDInstances
  with    SymbolInstances
```

#### cats.syntax.AllSyntax

`AllSyntax` is a trait that mixes in all of the operators available in Cats.

```scala
package cats
package syntax

trait AllSyntax
    extends ApplicativeSyntax
    with ApplicativeErrorSyntax
    with ApplySyntax
    with BifunctorSyntax
    with BifoldableSyntax
    with BitraverseSyntax
    with CartesianSyntax
    with CoflatMapSyntax
    with ComonadSyntax
    with ComposeSyntax
    with ContravariantSyntax
    with CoproductSyntax
    with EitherSyntax
    with EqSyntax
    ....
```

### a la carte style

Or, I'd like to call dim sum style, where they bring in a cart load of chinese dishes and you pick what you want.

If for whatever reason if you do not wish to import the entire `cats.implicits._`, you can pick and choose.

#### typeclass instances

Typeclass instances are broken down by the datatypes. Here's how to get all typeclass instances for `Option`:

```console:new
scala> {
         import cats.instances.option._
         cats.Monad[Option].pure(0)
       }
```

If you just want all instances, here's how to load them all:

```console
scala> {
         import cats.instances.all._
         cats.Monoid[Int].empty
       }
```

Because we have not injected any operators, you would have to work more with helper functions and functions under typeclass instances, which could be exactly what you want.

#### Cats typeclass syntax

Typeclass syntax are broken down by the typeclass. Here's how to get injected methods and operators for `Eq`s:

```console
scala> {
          import cats.syntax.eq._
          import cats.instances.all._
          1 === 1
       }
```

#### Cats datatype syntax

Cats datatype syntax like `Writer` are also available under `cats.syntax` package:

```console
scala> {
          import cats.syntax.writer._
          import cats.instances.all._
          1.tell
       }
```

#### standard datatype syntax

Standard datatype syntax are broken down by the datatypes. Here's how to get injected methods and functions for `Option`:

```console
scala> {
          import cats.syntax.option._
          import cats.instances.all._
          1.some
       }
```

#### all syntax

Here's how to load all syntax and all instances.

```console
scala> {
          import cats.syntax.all._
          import cats.instances.all._
          1.some
       }
```

This is the same as importing `cats.implicits._`.
Again, if you are at all confused by this, just stick with the following first:

```scala
scala> import cats._, cats.data._, cats.implicits._
```
