---
out: Const.html
---

  [Gibbons2006]: http://www.cs.ox.ac.uk/jeremy.gibbons/publications/dgp.pdf
  [iterator2009]: http://www.comlab.ox.ac.uk/jeremy.gibbons/publications/iterator.pdf

### Const datatype

Chapter 5 of [Datatype-Generic Programming][Gibbons2006], the last one before Conclusions,
is called "The Essence of the Iterator pattern," the same name of
the paper Gibbons and Oliveira wrote in 2006.
The one available online as [The Essence of the Iterator Pattern][iterator2009] is from 2009.
Reading this paper as a continuation of DGP gives it a better context.

Here's the example given at the beginning of this paper, translated to Java.

```java
public static <E> int loop(Collection<E> coll) {
  int n = 0;
  for (E elem: coll) {
    n = n + 1;
    doSomething(elem);
  }
  return n;
}
```

EIP:

> We emphasize that we want to capture both aspects of the method *loop* and iterations like it:
> *mapping* over the elements, and simultaneously *accumulating* some measure of those elements.

The first half of the paper reviews functional iterations and applicative style.
For applicative functors, it brings up the fact that there are three kinds of applicatives:

1. Monadic applicative functors
2. Naperian applicative functors
3. Monoidal applicative functors

We've brought up the fact that all monads are applicatives many times.
Naperian applicative functor zips together data structure that are fixed in shape.

Appliactive functors were originally named *idom* by McBride and Paterson,
so Gibbons uses the term *idiomatic* interchangably with *applicative* througout this paper,
even though McBride and Paterson renamed it to applicative functors.

#### Monoidal applicative functors using Const datatype

> A second family of applicative functors, this time non-monadic,
> arises from constant functors with monoidal targets.

We can derive an applicative functor from any `Monoid`,
by using `empty` for `pure`, and `|+|` for `ap`.
The `Const` datatype is also called `Const` in Cats:

```scala
/**
 * [[Const]] is a phantom type, it does not contain a value of its second type parameter `B`
 * [[Const]] can be seen as a type level version of `Function.const[A, B]: A => B => A`
 */
final case class Const[A, B](getConst: A) {
  /**
   * changes the type of the second type parameter
   */
  def retag[C]: Const[A, C] =
    this.asInstanceOf[Const[A, C]]

  ....
}
```

In the above, the type parameter `A` represents the value,
but `B` is a phantom type used to make `Functor` happy.

```console:new
scala> import cats._, cats.std.all._, cats.data.Const
scala> import cats.syntax.functor._
scala> Const(1) map { (_: String) + "!" }
```

When `A` forms a `Semigroup`, an `Apply` is derived,
and when `A` form a `Monoid`, an `Applicative` is derived automatically.

> Computations within this applicative functor accumulate some measure:
> for the monoid of integers with addition, they count or sum...

```console
scala> import cats.syntax.apply._
scala> Const(1).retag[String] ap Const(2).retag[String => String]
```
