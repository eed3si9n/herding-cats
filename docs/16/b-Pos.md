
### Pos

Awodey:

> Another kind of example one often sees in mathematics is categories of *structured sets*, that is, sets with some further "structure" and functions that "preserve it," where these notions are determined in some independent way.

> A partially ordered set or *poset* is a set *A* equipped with a binary relation *a ≤<sub>A</sub> b* such that the following conditions hold for all *a, b, c ∈ A*:
>
> - reflexivity: a ≤<sub>A</sub> a
> - transitivity: if a ≤<sub>A</sub> b and b ≤<sub>A</sub> c, then a ≤<sub>A</sub> c
> - antisymmetry: if a ≤<sub>A</sub> b and b ≤<sub>A</sub> a, then a = b
>
> An arrow from a poset *A* to a poset *B* is a function m: A => B that is *monotone*, in the sense that, for all a, a' ∈ A,
>
> - a ≤<sub>A</sub> a' implies m(a) ≤<sub>A</sub> m(a').

As long as the functions are *monotone*, the objects will continue to be in the category, so the "structure" is preserved. The category of posets and monotone functions is denoted as **Pos**. Awodey likes posets, so it's important we understand it.

An example of poset is the `Int` type where the binary operation of `≤` is integer `<=` just as it is defined with `PartialOrder` typeclass.
Another example could be `case class LString(value: String)` where `≤` is defined by comparing the length of `value`.

```scala
scala> :paste
// Entering paste mode (ctrl-D to finish)

case class LString(value: String)
val f: Int => LString = (x: Int) => LString(if (x < 0) "" else x.toString)

// Exiting paste mode, now interpreting.

defined class LString
f: Int => LString = <function1>

scala> f(0)
res0: LString = LString(0)

scala> f(10)
res1: LString = LString(10)
```

`f` in the above is monotone since `f(0) ≤ f(10)`, and any other pairs of `Int`s that are `a <= a'`.
