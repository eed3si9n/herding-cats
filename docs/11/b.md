---
out: datatype-generic-programming.html
---

  [Gibbons2006]: http://www.cs.ox.ac.uk/jeremy.gibbons/publications/dgp.pdf
  [wfmm]: http://www.haskellforall.com/2012/06/you-could-have-invented-free-monads.html
  [Free-monads]: Free-monads.html
  [Oliveira2008]: http://www.cs.ox.ac.uk/jeremy.gibbons/publications/scalagp.pdf
  [Oliveira2010]: http://www.cs.ox.ac.uk/jeremy.gibbons/publications/scalagp-jfp.pdf

### Datatype-generic programming with Bifunctor

Moving on the section 3.6 in [Datatype-Generic Programming][Gibbons2006] called "Datatype genericity."
Gibbons tries to call this Origami programming, but I don't think the name stuck,
so we'll go with datatype-generic programming.

> As we have already argued, data structure determines program structure.
> It therefore makes sense to abstract from the determining shape, leaving only what programs of different shape have in common.
> What datatypes such as `List` and `Tree` have in common is the fact that they are recursive — which is to say, a datatype `Fix`

```haskell
data Fix s a = In {out :: s a (Fix s a)}
```

> Here are three instances of Fix using different shapes: lists and internally labelled binary trees as seen before, and also a datatype of externally labelled binary trees.

```haskell
data ListF a b = NilF | ConsF a b
type List a = Fix ListF a
data TreeF a b = EmptyF | NodeF a b b
type Tree a = Fix TreeF a
data BtreeF a b = TipF a | BinF b b
type Btree a = Fix BtreeF a
```

From [Why free monads matter][wfmm] we saw on [day 8][Free-monads]
we actually know this is similar as `Free` datatype, but
the semantics around `Functor` etc is going to different,
so let's implement it from scratch:

```console:new
scala> :paste
sealed abstract class Fix[S[_], A] extends Serializable {
  def out: S[Fix[S, A]]
}
object Fix {
  case class In[S[_], A](out: S[Fix[S, A]]) extends Fix[S, A]
}
```

Following `Free`, I am putting `S[_]` on the left, and `A` on the right.

Let's try implementing the `List` first.

```console
scala> :paste
sealed trait ListF[+Next, +A]
object ListF {
  case class NilF() extends ListF[Nothing, Nothing]
  case class ConsF[A, Next](a: A, n: Next) extends ListF[Next, A]
}
type GenericList[A] = Fix[ListF[+?, A], A]
object GenericList {
  def nil[A]: GenericList[A] = Fix.In[ListF[+?, A], A](ListF.NilF())
  def cons[A](a: A, xs: GenericList[A]): GenericList[A] =
    Fix.In[ListF[+?, A], A](ListF.ConsF(a, xs))
}
scala> import GenericList.{ cons, nil }
```

Here's how we can use it:


```console
scala> cons(1, nil)
```

So far this is similar to what we saw with the free monad.

#### Bifunctor

> Not all valid binary type constructors s are suitable for *Fixing*;
> for example, function types with the parameter appearing in
> *contravariant* (source) positions cause problems.
> It turns out that we should restrict attention to (covariant) *bifunctors*,
> which support a *bimap* operation 'locating' all the elements.

Cats ships with `Bifunctor`:

```scala
/**
 * A typeclass of types which give rise to two independent, covariant
 * functors.
 */
trait Bifunctor[F[_, _]] extends Serializable { self =>

  /**
   * The quintessential method of the Bifunctor trait, it applies a
   * function to each "side" of the bifunctor.
   */
  def bimap[A, B, C, D](fab: F[A, B])(f: A => C, g: B => D): F[C, D]

  ....
}
```

Here is the `Bifunctor` instance for `GenericList`.

```console
scala> import cats._, cats.std.all._
scala> import cats.functor.Bifunctor
scala> :paste
implicit val listFBifunctor: Bifunctor[ListF] = new Bifunctor[ListF] {
  def bimap[S1, A1, S2, A2](fab: ListF[S1, A1])(f: S1 => S2, g: A1 => A2): ListF[S2, A2] =
    fab match {
      case ListF.NilF()         => ListF.NilF()
      case ListF.ConsF(a, next) => ListF.ConsF(g(a), f(next))
    }
}
```

#### Deriving map from Bifunctor

> It turns out that the class `Bifunctor` provides sufficient flexibility
> to capture a wide variety of recursion patterns as datatype-generic programs.

First, we can implement `map` in terms of `bimap`.

```console
scala> :paste
object DGP {
  def map[F[_, _]: Bifunctor, A1, A2](fa: Fix[F[?, A1], A1])(f: A1 => A2): Fix[F[?, A2], A2] =
    Fix.In[F[?, A2], A2](Bifunctor[F].bimap(fa.out)(map(_)(f), f))
}
scala> DGP.map(cons(1, nil)) { _ + 1 }
```

The above definition of `map` is independent from `GenericList`, abstracted by `Bifunctor` and `Fix`.
Another way of looking at it is that we can get `Functor` for free out of `Bifunctor` and `Fix`.

```console
scala> :paste
trait FixInstances {
  implicit def fixFunctor[F[_, _]: Bifunctor]: Functor[Lambda[L => Fix[F[?, L], L]]] =
    new Functor[Lambda[L => Fix[F[?, L], L]]] {
      def map[A1, A2](fa: Fix[F[?, A1], A1])(f: A1 => A2): Fix[F[?, A2], A2] =
        Fix.In[F[?, A2], A2](Bifunctor[F].bimap(fa.out)(map(_)(f), f))
    }
}
scala> {
  val instances = new FixInstances {}
  import instances._
  import cats.syntax.functor._
  cons(1, nil) map { _ + 1 }
}
```

Intense amount of type lambdas, but I think it's clear that I translated `DB.map` into a `Functor` instance.

#### Deriving fold from Bifunctor

We can also implement `fold`, also known as `cata` from catamorphism:

```console
scala> :paste
object DGP {
  // catamorphism
  def fold[F[_, _]: Bifunctor, A1, A2](fa: Fix[F[?, A1], A1])(f: F[A2, A1] => A2): A2 =
    {
      val g = (fa1: F[Fix[F[?, A1], A1], A1]) =>
        Bifunctor[F].leftMap(fa1) { (fold(_)(f)) }
      f(g(fa.out))
    }
}
scala> DGP.fold[ListF, Int, Int](cons(2, cons(1, nil))) {
         case ListF.NilF()      => 0
         case ListF.ConsF(x, n) => x + n
       }
```

#### Deriving unfold from Bifunctor

> The `unfold` operator for a datatype grows a data structure from a value.
> In a precise technical sense, it is the dual of the `fold` operator.

The `unfold` is also called `ana` from anamorphism:

```console
scala> :paste
object DGP {
  // catamorphism
  def fold[F[_, _]: Bifunctor, A1, A2](fa: Fix[F[?, A1], A1])(f: F[A2, A1] => A2): A2 =
    {
      val g = (fa1: F[Fix[F[?, A1], A1], A1]) =>
        Bifunctor[F].leftMap(fa1) { (fold(_)(f)) }
      f(g(fa.out))
    }
  // anamorphism
  def unfold[F[_, _]: Bifunctor, A1, A2](x: A2)(f: A2 => F[A2, A1]): Fix[F[?, A1], A1] =
    Fix.In[F[?, A1], A1](Bifunctor[F].leftMap(f(x))(unfold[F, A1, A2](_)(f)))
}
```

Here's how we can construct list of numbers counting down:


```console
scala> def pred(n: Int): GenericList[Int] =
         DGP.unfold[ListF, Int, Int](n) {
           case 0 => ListF.NilF()
           case n => ListF.ConsF(n, n - 1)
         }
scala> pred(4)
```

There are several more we can derive, too.

#### Tree

The point of the datatype-generic programming is to abstract out the shape.
Let's introduce some other datatype, like a binary `Tree`:

```console
scala> :paste
sealed trait TreeF[+Next, +A]
object TreeF {
  case class EmptyF() extends TreeF[Nothing, Nothing]
  case class NodeF[Next, A](a: A, left: Next, right: Next) extends TreeF[Next, A]
}
type Tree[A] = Fix[TreeF[?, A], A]
object Tree {
  def empty[A]: Tree[A] =
    Fix.In[TreeF[+?, A], A](TreeF.EmptyF())
  def node[A, Next](a: A, left: Tree[A], right: Tree[A]): Tree[A] =
    Fix.In[TreeF[+?, A], A](TreeF.NodeF(a, left, right))
}
```

Here's how to create this tree:

```console
scala> import Tree.{empty,node}
scala> node(2, node(1, empty, empty), empty)
```

Now, all we have to do should be to define a `Bifunctor` instance:

```console
scala> :paste
implicit val treeFBifunctor: Bifunctor[TreeF] = new Bifunctor[TreeF] {
  def bimap[A, B, C, D](fab: TreeF[A, B])(f: A => C, g: B => D): TreeF[C, D] =
    fab match {
      case TreeF.EmptyF() => TreeF.EmptyF()
      case TreeF.NodeF(a, left, right) =>
        TreeF.NodeF(g(a), f(left), f(right))
    }
}
```

First, let's try `Functor`:

```console
scala> {
  val instances = new FixInstances {}
  import instances._
  import cats.syntax.functor._
  node(2, node(1, empty, empty), empty) map { _ + 1 }
}
```

Looking good. Next, let's try folding.

```console
scala> def sum(tree: Tree[Int]): Int =
         DGP.fold[TreeF, Int, Int](tree) {
           case TreeF.EmptyF()       => 0
           case TreeF.NodeF(a, l, r) => a + l + r
         }
scala> sum(node(2, node(1, empty, empty), empty))
```

We got the `fold`.

Here's a function named `grow` that generates a binary search tree from a list.

```console
scala> def grow[A: PartialOrder](xs: List[A]): Tree[A] =
          DGP.unfold[TreeF, A, List[A]](xs) {
            case Nil => TreeF.EmptyF()
            case x :: xs =>
              import cats.syntax.partialOrder._
              TreeF.NodeF(x, xs filter {_ <= x}, xs filter {_ > x})
          }
scala> grow(List(3, 1, 4, 2))
```

Looks like `unfold` is working too.

For more details on DGP in Scala, Oliveira and Gibbons wrote a paper translating the
ideas and many more called [Scala for Generic Programmers (2008)][Oliveira2008],
and its updated version [Scala for Generic Programmers (2010)][Oliveira2010].

Datatype-generic programming described here has a limitation of datatype being recursive,
so I don't think it's very usable as is, but there are some interesting concepts that can be exploited.

#### The Origami patterns

Next, Gibbons claims that the design patterns are evidence of a
"evidence of a lack of expressivity in those mainstream programming languages."
He then sets out to replace the patterns using
higher-order datatype-generic programming.
