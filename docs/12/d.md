---
out: shape-and-contents.html
---

### Shape and contents

EIP:

> In addition to being parametrically polymorphic in the collection elements,
> the generic `traverse` operation is parametrised along two further dimensions:
> the datatype being traversed, and the applicative functor in which the traversal is interpreted.
> Specialising the latter to lists as a monoid yields a generic `contents` operation:

Here's how we can implement this with Cats:

```scala mdoc
import cats._, cats.data._, cats.syntax.all._

def contents[F[_], A](fa: F[A])(implicit FF: Traverse[F]): Const[List[A], F[Unit]] =
  {
    val contentsBody: A => Const[List[A], Unit] = { (a: A) => Const(List(a)) }
    FF.traverse(fa)(contentsBody)
  }
```

Now we can take any datatype that supports `Traverse` and turn it into a `List`.

```scala mdoc
contents(Vector(1, 2, 3)).getConst
```

I am actually not sure if the result suppose to come out in the reverse order here.

> The other half of the decomposition is obtained simply by a map,
> which is to say, a traversal interpreted in the identity idiom.

When Gibbons say identity idiom, he means the identity applicative functor, `Id[_]`.

```scala mdoc
def shape[F[_], A](fa: F[A])(implicit FF: Traverse[F]): Id[F[Unit]] =
  {
    val shapeBody: A => Id[Unit] = { (a: A) => () }
    FF.traverse(fa)(shapeBody)
  }
```

Here's the shape for `Vector(1, 2, 3)`:

```scala mdoc
shape(Vector(1, 2, 3))
```

EIP:

> This pair of traversals nicely illustrates the two aspects of iterations
> that we are focussing on, namely mapping and accumulation.

Next EIP demonstrates applicative composition by first combining `shape` and `contents` together like this:

```scala mdoc
def decompose[F[_], A](fa: F[A])(implicit FF: Traverse[F]) =
  Tuple2K[Const[List[A], *], Id, F[Unit]](contents(fa), shape(fa))

val d = decompose(Vector(1, 2, 3))

d.first

d.second
```

The problem here is that we are running `traverse` twice.

> Is it possible to fuse the two traversals into one?
> The product of applicative functors allows exactly this.

Let's try rewriting this using `AppFunc`.

```scala mdoc:reset:invisible
import cats._, cats.data._, cats.syntax.all._
```

```scala mdoc
import cats.data.Func.appFunc

def contentsBody[A]: AppFunc[Const[List[A], *], A, Unit] =
  appFunc[Const[List[A], *], A, Unit] { (a: A) => Const(List(a)) }

def shapeBody[A]: AppFunc[Id, A, Unit] =
  appFunc { (a: A) => ((): Id[Unit]) }

def decompose[F[_], A](fa: F[A])(implicit FF: Traverse[F]) =
  (contentsBody[A] product shapeBody[A]).traverse(fa)

val d = decompose(Vector(1, 2, 3))

d.first

d.second
```

The return type of the `decompose` is a bit messy, but it's infered by `AppFunc`:
`Tuple2K[Const[List[Int], β1], Id[A], Vector[Unit]]`.
