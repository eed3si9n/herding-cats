---
out: Free-monoids.html
---

  [fafmm]: http://learnyouahaskell.com/for-a-few-monads-more
  [awodey]: http://www.amazon.com/Category-Theory-Oxford-Logic-Guides/dp/0199237182

### Free monoids

I'm going to diverge from [Learn You a Haskell for Great Good][fafmm] a bit,
and explore free objects.

Let's first look into free monoids. Given a set of characters:

```
A = { 'a', 'b', 'c', ... }
```

We can form the *free monoid* on `A` called `A*` as follows:

```
A* = String
```

Here, the binary operation is `String` concatenation `+` operator.
We can show that this satisfies the monoid laws using the empty string `""`
as the identity.

Furthermore, a free monoid can be formed from any arbitrary set `A` by concatenating them:

```
A* = List[A]
```

Here, the binary operation is `:::`, and the identity is `Nil`.
The definition of the free monoid *M(A)* is given as follows:

[Awodey][awodey]:

> Universal Mapping Property of *M(A)*<br>
> There is a function *i: A => |M(A)|*, and given any monoid *N*
> and any function *f: A => |N|*, there is a *unique* monoid homomorphism
> *f_hom = M(A) => N* such that 
> *|f_hom| ∘ i = f*, all as indicated in the following diagram:

Instead of `A`, I'll use `X` here:

![free monoids](files/day8-free-monoids.png)

If we think in terms of Scala,

```scala
def i(x: X): Set[M[X]] = ???
def f(x: X): Set[N] = ???

// there exists a unique
def f_hom(mx: M[X]): N

// such that
def f_hom_set(smx: Set[M[X]]): Set[N] = sma map {f_hom}
f == f_hom_set compose i
```

Suppose `A` is `Char` and `N` is `(Int, +)`.
We can write a property test to see if `String` is a free monoid.

```scala
scala> def i(x: Char): Set[String] = Set(x.toString)
i: (x: Char)Set[String]

scala> def f(x: Char): Set[Int] = Set(x.toInt) // example
f: (x: Char)Set[Int]

scala> val f_hom: PartialFunction[String, Int] = 
         { case mx: String if mx.size == 1 => mx.charAt(0).toInt }
f_hom: PartialFunction[String,Int] = <function1>

scala> def f_hom_set(smx: Set[String]): Set[Int] = smx map {f_hom}
f_hom_set: (smx: Set[String])Set[Int]

scala> val g = (f_hom_set _) compose (i _)
g: Char => Set[Int] = <function1>

scala> import org.scalacheck.Prop.forAll
import org.scalacheck.Prop.forAll

scala> val propMAFree = forAll { c: Char => f(c) == g(c) }
propMAFree: org.scalacheck.Prop = Prop

scala> propMAFree.check
+ OK, passed 100 tests.
```

At least for this implemention of `f` we were able to show that `String` is free.

#### Injective

This intuitively shows that `Set[M[X]]` needs to be lossless for it to allow any *f*,
meaning no two values on `X` can map into the same value in `M[X]`.
In algebra, this is expressed as `i` is *injective* for arrows from `Char`.

> **Definitions**: An arrow *f* satisfying the property 'for any pair of arrows *x<sub>1</sub>: T => A*
> and *x<sub>2</sub>: T => A*, if *f ∘ x<sub>1</sub> = f ∘ x<sub>2</sub>* then *x<sub>1</sub> = x<sub>2</sub>*',
> it is said to be *injective for arrows from T*.

![injective](file/day8-injective.png)

#### Uniqueness

UMP also stipulates `f_hom` to be unique, so that requires `Set[M[A]]` to be
zero or more combinations of `A`'s and nothing more.
Because `M[A]` is unique for `A`, conceptually there is one and only free monoid for a set `A`.
We can however have the free monoid expressed in different ways like `String` and `List[Char]`,
so it ends up being more like a free monoid.

#### Free objects

It turns out that the free monoid is an example of free objects,
which we can define using a functor `Set[A]: C[A] => Set[A]`.

![free objects](file/day8-free-objects.png)

Comparing the diagram, we see that they are mostly similar.
