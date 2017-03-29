---
out: genericity.html
---

  [Gibbons2006]: http://www.cs.ox.ac.uk/jeremy.gibbons/publications/dgp.pdf
  [Pickling]: https://github.com/scala/pickling
  [shapeless]: https://github.com/milessabin/shapeless
  [scodec]: https://github.com/scodec/scodec

### Genericity

In the grand scheme of things, functional programming is about
abstracting things out.
Skimming over Jeremy Gibbons's 2006 book [Datatype-Generic Programming][Gibbons2006],
I found a nice overview.

> *Generic programming* is about making programming languages more flexible without compromising safety.

#### Genericity by value

> One of the first and most fundamental techniques that any programmer learns
> is how to parametrize computations by values

```console:new
scala> def triangle4: Unit = {
         println("*")
         println("**")
         println("***")
         println("****")
       }
```

We can abstract out 4 into a parameter:

```console
scala> def triangle(side: Int): Unit = {
         (1 to side) foreach { row =>
           (1 to row) foreach { col =>
             println("*")
           }
         }
       }
```

#### Genericity by type

`List[A]` is a *polymorphic datatype* parametrized by another type,
the type of list elements. This enables *parametric polymorphism*.

```console
scala> def head[A](xs: List[A]): A = xs(0)
```

The above function would work for all proper types.

#### Genericity by function

> *Higher-order* programs are programs parametrized by other programs.

For example `foldLeft` can be used to `append` two lists:

```console
scala> def append[A](list: List[A], ys: List[A]): List[A] =
         list.foldLeft(ys) { (acc, x) => x :: acc }
scala> append(List(1, 2, 3), List(4, 5, 6))
```

Or it can also be used to add numbers:

```console
scala> def sum(list: List[Int]): Int =
        list.foldLeft(0) { _ + _ }
```

#### Genericity by structure

"Generic programming" embodied in the sense of a collection library,
like Scala Collections.
In the case of C++'s Standard Template Library,
the parametric datatypes are called *containers*, and various abstractions are provided via *iterators*,
such as input iterators and forward iterators.

The notion of the typeclass fits in here too.

```console
scala> :paste
trait Read[A] {
  def reads(s: String): Option[A]
}
object Read extends ReadInstances {
  def read[A](f: String => Option[A]): Read[A] = new Read[A] {
    def reads(s: String): Option[A] = f(s)
  }
  def apply[A: Read]: Read[A] = implicitly[Read[A]]
}
trait ReadInstances {
  implicit val stringRead: Read[String] =
    Read.read[String] { Some(_) }
  implicit val intRead: Read[Int] =
    Read.read[Int] { s =>
      try {
        Some(s.toInt)
      } catch {
        case e: NumberFormatException => None
      }
    }
}
scala> Read[Int].reads("1")
```

The typeclass captures the requirements required of types, called typeclass contract.
It also lets us list the types providing these requirements by defining
typeclass instances.
This enables *ad-hoc polymorphism* because `A` in `Read[A]` is not universal.

#### Genericity by property

In Scala Collection library, some of the concepts promised are more
elaborate than the list of operations covered by the type.

> as well as signatures of operations,
> the concept might specify the laws these operations satisfy, and non-functional
> characteristics such as the asymptotic complexities of the operations in terms of
> time and space.

Typeclasses with laws fit in here too. For example `Monoid[A]` comes with the monoid laws.
The laws need to be validated for each instance using property-based testing tools.

#### Genericity by stage

Various flavors of *metaprogramming* can be though of as the development
or program that construct or manipulate other programs.
This could include code generation and macros.

#### Genericity by shape

Let's say there's a polymorphic datatype of binary trees:

```console
scala> :paste
sealed trait Btree[A]
object Btree {
  case class Tip[A](a: A) extends Btree[A]
  case class Bin[A](left: Btree[A], right: Btree[A]) extends Btree[A]
}
```

Let's write `foldB` as a way of abstracting similar programs.

```console
scala> def foldB[A, B](tree: Btree[A], b: (B, B) => B)(t: A => B): B =
         tree match {
           case Btree.Tip(a)      => t(a)
           case Btree.Bin(xs, ys) => b(foldB(xs, b)(t), foldB(ys, b)(t))
         }
```

The next goal is to abstract `foldB` and `foldLeft`.

> In fact, what differs between the two fold operators is the *shape* of the data
> on which they operate, and hence the shape of the programs themselves. The
> kind of parametrization required is by this shape; that is, by the datatype or type
> constructor (such as `List` or `Tree`) concerned. We call this *datatype genericity*.

For example, `fold` apparently could be expressed as

```console
scala> import cats._, cats.data._, cats.implicits._, cats.functor.Bifunctor
scala> :paste
trait Fix[F[_,_], A]
def cata[S[_,_]: Bifunctor, A, B](t: Fix[S, A])(f: S[A, B] => B): B = ???
```

In the above, `S` represents the shape of the datatype.
By abstracting out the shapes, we can construct *parametrically datatype-generic* programs.
We'll come back to this later.

> Alternatively, such programs might be *ad-hoc datatype-generic*,
> when the behaviour exploits that shape in some essential manner.
> Typical examples of the latter are pretty printers and marshallers.

The example that fits in this category might be [Scala Pickling][Pickling].
Pickling defines picklers for common types, and it derives
pickler instances for different shapes using macro.

> This approach to datatype genericity has been variously called *polytypism*,
> *structural polymorphism* or *typecase* , and is the meaning given to 'generic programming' by the Generic Haskell team.
> Whatever the name, functions are defined inductively by case analysis on the structure of datatypes
> ....
>
> We consider parametric datatype genericity to be the 'gold standard', and in the remainder of these lecture notes, we concentrate on parametric datatype-generic definitions where possible.

In Scala, [shapeless][shapeless] is focused on abstracting out the shape.
