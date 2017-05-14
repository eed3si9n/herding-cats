
### FoldLeft

> `List` に関しても一般化した関数を目指しましょう。... そのためには、`foldLeft` 演算に関して一般化します。

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

> これで先ほどと同様の抽象化を行なって `FoldLeft` 型クラスを抜き出します。

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

これで `Int` と `List` の両方が `sum` から抜き出された。

### Cats の型クラス

上の例における trait の `Monoid` と `FoldLeft` は Haskell の型クラスに相当する。Cats は多くの型クラスを提供する。

> これらの型クラスの全ては必要な関数だけを含んだ部品に分けられています。ある関数が必要十分なものだけを要請するため究極のダック・タイピングだと言うこともできるでしょう。
