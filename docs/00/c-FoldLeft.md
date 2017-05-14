
### FoldLeft

> What we wanted was a function that generalized on `List`. ... So we want to generalize on `foldLeft` operation.

```console
scala> object FoldLeftList {
         def foldLeft[A, B](xs: List[A], b: B, f: (B, A) => B) = xs.foldLeft(b)(f)
       }
scala> def sum[A: Monoid](xs: List[A]): A = {
         val m = implicitly[Monoid[A]]
         FoldLeftList.foldLeft(xs, m.mzero, m.mappend)
       }
scala> sum(List(1, 2, 3, 4))
scala> sum(List("a", "b", "c"))
scala> sum(List(1, 2, 3, 4))(multiMonoid)
```

> Now we can apply the same abstraction to pull out `FoldLeft` typeclass.

```console
scala> :paste
trait FoldLeft[F[_]] {
  def foldLeft[A, B](xs: F[A], b: B, f: (B, A) => B): B
}
object FoldLeft {
  implicit val FoldLeftList: FoldLeft[List] = new FoldLeft[List] {
    def foldLeft[A, B](xs: List[A], b: B, f: (B, A) => B) = xs.foldLeft(b)(f)
  }
}

def sum[M[_]: FoldLeft, A: Monoid](xs: M[A]): A = {
  val m = implicitly[Monoid[A]]
  val fl = implicitly[FoldLeft[M]]
  fl.foldLeft(xs, m.mzero, m.mappend)
}

scala> sum(List(1, 2, 3, 4))
scala> sum(List("a", "b", "c"))
```

Both `Int` and `List` are now pulled out of `sum`.

### Typeclasses in Cats

In the above example, the traits `Monoid` and `FoldLeft` correspond to Haskell's typeclass.
Cats provides many typeclasses.

> All this is broken down into just the pieces you need.
> So, it's a bit like ultimate ducktyping because you define in your function definition that this is what you need and nothing more.
