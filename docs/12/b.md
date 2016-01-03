---
out: Unapply.html
---

  [SI-2712]: https://issues.scala-lang.org/browse/SI-2712
  [cecc3a]: https://github.com/stew/cats/commit/cecc3afbdbb6fbbe764005cd52e9efe7acdfc8f2
  [combining-applicative]: combining-applicative.html

### Coercing type inference using Unapply

EIP:

> We will have a number of new datatypes with coercion functions like `Id`, `unId`, `Const` and `unConst`.
> To reduce clutter, we introduce a common notation for such coercions.

In Scala, implicits and type inference take us pretty far.
But by dealing with typeclass a lot, we also end up seeing some of the weaknesses of Scala's type inference.
One of the issues we come across frequently is its inability to infer partially applied parameterized type,
also known as [SI-2712][SI-2712].
If you're reading this, please jump to the page, upvote the bug, or help solving the problem.

#### Unapply

To work around this issue, Cats uses a typeclass called `Unapply`:

```scala
/**
 * A typeclass that is used to help guide scala's type inference to
 * find typeclass instances for types which have shapes which differ
 * from what their typeclasses are looking for.
 *
 * For example, [[Functor]] is defined for types in the shape
 * F[_]. Scala has no problem finding instance of Functor which match
 * this shape, such as Functor[Option], Functor[List], etc. There is
 * also a functor defined for some types which have the Shape F[_,_]
 * when one of the two 'holes' is fixed. For example. there is a
 * Functor for Map[A,?] for any A, and for Either[A,?] for any A,
 * however the scala compiler will not find them without some coercing.
 */
trait Unapply[TC[_[_]], MA] {
  // a type constructor which is properly kinded for the typeclass
  type M[_]
  // the type applied to the type constructor to make an MA
  type A

  // the actual typeclass instance found
  def TC: TC[M]

  // a function which will coerce the MA value into one of type M[A]
  // this will end up being the identity function, but we can't supply
  // it until we have proven that MA and M[A] are the same type
  def subst: MA => M[A]
}
```

I think it's easier to demonstrate this using an example.


```console:new
scala> import cats._, cats.std.all._
scala> def foo[F[_]: Applicative](fa: F[Int]): F[Int] = fa
```

In the above, `foo` is a simple function that returns the passed in value `fa: F[Int]`
where `F` forms an `Applicative`.
Since `Either[String, Int]` is an applicative, it should qualify.

```console:error
scala> foo(Right(1): Either[String, Int])
```

We got the error "argument expression's type is not compatible with formal parameter type."
We can make an `Unapply` version of `foo` as follows:


```console
scala> def fooU[FA](fa: FA)(implicit U: Unapply[Applicative, FA]): U.M[U.A] =
         U.subst(fa)
```

Now let's try passing in exactly same parameter as we tried:

```console
scala> fooU(Right(1): Either[String, Int])
```

It works. Let's look into how this is implemented.
For `Either`, a monad is formed for `Either[AA, ?]`, which means
during `map` the right side of the parameter might change like
`List[Int]` changing to `List[String]`, but the left side stays put.

```scala
sealed abstract class Unapply2Instances extends Unapply3Instances {
  type Aux2Right[TC[_[_]], MA, F[_,_], AA, B] = Unapply[TC, MA] {
    type M[X] = F[AA,X]
    type A = B
  }

  implicit def unapply2right[TC[_[_]], F[_,_], AA, B](implicit tc: TC[F[AA,?]]): Aux2Right[TC,F[AA,B], F, AA, B] = new Unapply[TC, F[AA,B]] {
     type M[X] = F[AA, X]
     type A = B
     def TC: TC[F[AA, ?]] = tc
     def subst: F[AA, B] => M[A] = identity
   }

   ....
}
```

First Cats is defining `Aux2Right` as a type alias that defines the abstract types `M[_]` and `A`.
Next, it defines an implicit converter from an arbitray typeclass instance `TC[F[AA,?]]`
to `Aux2Right[TC,F[AA,B], F, AA, B]`.

#### Bedazzle my instance

One area where `Unapply` is used in Cats is where infix operators, also known as "syntax",
is injected. These implicit converters are called "bedazzlers" in [cecc3a][cecc3a].

```scala
package cats
package syntax

trait FunctorSyntax1 {
  implicit def functorSyntaxU[FA](fa: FA)(implicit U: Unapply[Functor,FA]): Functor.Ops[U.M, U.A] =
    new Functor.Ops[U.M, U.A]{
      val self = U.subst(fa)
      val typeClassInstance = U.TC
    }
}

trait FunctorSyntax extends Functor.ToFunctorOps with FunctorSyntax1
```

Let's try using `*>` operator from `Apply`:

```console
scala> import cats.syntax.monoidal._
scala> (Right(1): Either[String, Int]) *> Right(2)
```

This works, likely thanks to the `Unapply`.

#### The limitations

[AppFunc][combining-applicative] we looked at on day 11 lets us create ever more complex
composition of applicative functor instances.
It would be half as useful without the help of `Unapply`, since part of the benefit
is that it can derive the instances automatically.
At the same time, `Unapply` cannot possibly be the ultimate solution because
it requires all shapes to be spelled out up front, and the most complex type it handles currently is `F[AA, B, ?]`.
I could go beyond this by making a product of compositions of monad instances with two parameters.

I can only guess that this type inference problem is a problem that needs to be
solved by the Scala compiler itself by treating sequential composition and
parallel composition (product) as a first-class constuct in the type system.
